package com.queuectl.worker;

import com.queuectl.config.QueueConfig;
import com.queuectl.domain.Job;
import com.queuectl.service.CommandExecutor;
import com.queuectl.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Individual worker that processes jobs from the queue.
 * 
 * This class implements the core job processing logic with proper
 * error handling and graceful shutdown capabilities.
 */
@Slf4j
@RequiredArgsConstructor
public class Worker implements Runnable {
    
    private final String workerId;
    private final JobService jobService;
    private final CommandExecutor commandExecutor;
    private final QueueConfig queueConfig;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private Job currentJob;
    private Thread workerThread;
    
    /**
     * Create a new worker with generated ID.
     */
    public static Worker create(JobService jobService, CommandExecutor commandExecutor, QueueConfig queueConfig) {
        String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        return new Worker(workerId, jobService, commandExecutor, queueConfig);
    }
    
    @Override
    public void run() {
        workerThread = Thread.currentThread();
        running.set(true);
        
        log.info("Worker {} started", workerId);
        
        try {
            while (!shutdown.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    processNextJob();
                    
                    // Sleep between job polling
                    Thread.sleep(queueConfig.getWorkers().getPollInterval().toMillis());
                    
                } catch (InterruptedException e) {
                    log.info("Worker {} interrupted", workerId);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Unexpected error in worker {}: {}", workerId, e.getMessage(), e);
                    // Continue processing despite errors
                }
            }
        } finally {
            running.set(false);
            log.info("Worker {} stopped", workerId);
        }
    }
    
    /**
     * Process the next available job.
     */
    private void processNextJob() {
        // Claim next job atomically
        Optional<Job> jobOpt = jobService.claimNextJob(workerId);
        
        if (jobOpt.isEmpty()) {
            // No jobs available
            return;
        }
        
        Job job = jobOpt.get();
        currentJob = job;
        
        try {
            log.info("Worker {} processing job {}: {}", workerId, job.getId(), job.getCommand());
            
            // Validate command for security
            if (!commandExecutor.isCommandSafe(job.getCommand())) {
                jobService.failJob(job.getId(), -1, "Command rejected for security reasons");
                return;
            }
            
            // Execute the job
            CommandExecutor.ExecutionResult result = commandExecutor.executeJob(job);
            
            // Handle result
            if (result.isSuccess()) {
                jobService.completeJob(job.getId(), result.getExitCode(), result.getOutputId());
                log.info("Worker {} completed job {} successfully", workerId, job.getId());
            } else {
                jobService.failJob(job.getId(), result.getExitCode(), result.getErrorMessage());
                log.warn("Worker {} job {} failed: {}", workerId, job.getId(), result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("Worker {} failed to process job {}: {}", workerId, job.getId(), e.getMessage(), e);
            jobService.failJob(job.getId(), -1, "Worker error: " + e.getMessage());
        } finally {
            currentJob = null;
        }
    }
    
    /**
     * Request graceful shutdown of this worker.
     * Will finish current job before stopping.
     */
    public void shutdown() {
        log.info("Worker {} shutdown requested", workerId);
        shutdown.set(true);
        
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
    
    /**
     * Force immediate shutdown of this worker.
     */
    public void shutdownNow() {
        log.warn("Worker {} immediate shutdown requested", workerId);
        shutdown.set(true);
        
        if (workerThread != null) {
            workerThread.interrupt();
        }
        
        // Release current job if any
        if (currentJob != null) {
            log.warn("Worker {} releasing job {} due to forced shutdown", workerId, currentJob.getId());
            // The job will be reset to PENDING by the repository on restart
        }
    }
    
    /**
     * Check if worker is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Check if worker has been shutdown.
     */
    public boolean isShutdown() {
        return shutdown.get();
    }
    
    /**
     * Get the ID of the job currently being processed.
     */
    public String getCurrentJobId() {
        return currentJob != null ? currentJob.getId() : null;
    }
    
    /**
     * Get worker ID.
     */
    public String getWorkerId() {
        return workerId;
    }
    
    /**
     * Get worker status information.
     */
    public WorkerStatus getStatus() {
        return WorkerStatus.builder()
            .workerId(workerId)
            .running(running.get())
            .shutdown(shutdown.get())
            .currentJobId(getCurrentJobId())
            .build();
    }
    
    /**
     * Worker status information.
     */
    @lombok.Data
    @lombok.Builder
    public static class WorkerStatus {
        private String workerId;
        private boolean running;
        private boolean shutdown;
        private String currentJobId;
        private Instant startTime;
        private String status;
    }
}
