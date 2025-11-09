package com.queuectl.service;

import com.queuectl.config.QueueConfig;
import com.queuectl.worker.Worker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing worker pool and lifecycle.
 * 
 * This service handles:
 * - Starting and stopping workers
 * - Graceful shutdown with proper job completion
 * - Worker health monitoring
 * - Pool size management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {
    
    private final JobService jobService;
    private final CommandExecutor commandExecutor;
    private final QueueConfig queueConfig;
    
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private Instant startTime;
    
    /**
     * Start workers in daemon mode or blocking mode.
     * 
     * @param workerCount Number of workers to start
     * @param daemon If true, runs in background; if false, blocks until stopped
     */
    public void startWorkers(int workerCount, boolean daemon) {
        if (executorService != null && !executorService.isShutdown()) {
            log.warn("Workers are already running");
            return;
        }
        
        startTime = Instant.now();
        executorService = Executors.newFixedThreadPool(workerCount);
        
        log.info("Starting {} workers...", workerCount);
        
        for (int i = 0; i < workerCount; i++) {
            Worker worker = Worker.create(jobService, commandExecutor, queueConfig);
            workers.put(worker.getWorkerId(), worker);
            executorService.submit(worker);
            
            log.debug("Started worker {}", worker.getWorkerId());
        }
        
        log.info("Successfully started {} workers", workerCount);
        
        if (!daemon) {
            // Block until shutdown for interactive mode
            System.out.println("✅ Workers started successfully");
            System.out.println("   Press Ctrl+C to stop workers gracefully");
            try {
                while (!executorService.isShutdown()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                log.info("Worker service interrupted");
                Thread.currentThread().interrupt();
                stopWorkersGracefully(30);
            }
        } else {
            // In daemon mode, keep the application alive by blocking the main thread
            System.out.println("✅ Workers started in daemon mode");
            System.out.println("   Use 'queuectl worker stop' to stop workers");
            
            // Create a shutdown hook to handle graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered, stopping workers gracefully...");
                stopWorkersGracefully(30);
            }));
            
            // Keep main thread alive while workers are running
            try {
                while (!executorService.isShutdown() && !workers.isEmpty()) {
                    Thread.sleep(5000); // Check every 5 seconds
                    
                    // Remove any dead workers
                    workers.entrySet().removeIf(entry -> !entry.getValue().isRunning());
                }
            } catch (InterruptedException e) {
                log.info("Worker service interrupted in daemon mode");
                Thread.currentThread().interrupt();
                stopWorkersGracefully(30);
            }
        }
    }
    
    /**
     * Stop workers gracefully, allowing current jobs to complete.
     * 
     * @param timeoutSeconds Maximum time to wait for graceful shutdown
     * @return true if all workers stopped gracefully, false if timeout occurred
     */
    public boolean stopWorkersGracefully(int timeoutSeconds) {
        if (executorService == null || executorService.isShutdown()) {
            log.info("No workers are running");
            return true;
        }
        
        log.info("Stopping {} workers gracefully (timeout: {}s)...", workers.size(), timeoutSeconds);
        
        // Request graceful shutdown of all workers
        workers.values().forEach(Worker::shutdown);
        
        // Shutdown executor service
        executorService.shutdown();
        
        try {
            // Wait for workers to finish current jobs
            boolean terminated = executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
            
            if (terminated) {
                log.info("All workers stopped gracefully");
                workers.clear();
                return true;
            } else {
                log.warn("Some workers did not stop within timeout, forcing shutdown");
                return stopWorkersImmediately();
            }
            
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for workers to stop");
            Thread.currentThread().interrupt();
            return stopWorkersImmediately();
        }
    }
    
    /**
     * Stop workers immediately, interrupting current jobs.
     * 
     * @return true if workers were stopped
     */
    public boolean stopWorkersImmediately() {
        if (executorService == null || executorService.isShutdown()) {
            log.info("No workers are running");
            return true;
        }
        
        log.warn("Stopping {} workers immediately...", workers.size());
        
        // Force shutdown of all workers
        workers.values().forEach(Worker::shutdownNow);
        
        // Force shutdown executor service
        List<Runnable> pendingTasks = executorService.shutdownNow();
        
        if (!pendingTasks.isEmpty()) {
            log.warn("Cancelled {} pending worker tasks", pendingTasks.size());
        }
        
        workers.clear();
        log.info("All workers stopped immediately");
        return true;
    }
    
    /**
     * Get current worker status information.
     */
    public WorkerPoolStatus getWorkerStatus() {
        int activeWorkers = (int) workers.values().stream()
            .filter(Worker::isRunning)
            .count();
        
        int jobsProcessing = (int) workers.values().stream()
            .filter(worker -> worker.getCurrentJobId() != null)
            .count();
        
        Map<String, Worker.WorkerStatus> workerDetails = workers.values().stream()
            .collect(Collectors.toMap(
                Worker::getWorkerId,
                Worker::getStatus
            ));
        
        Duration uptime = startTime != null ? Duration.between(startTime, Instant.now()) : Duration.ZERO;
        
        return WorkerPoolStatus.builder()
            .totalWorkers(workers.size())
            .activeWorkers(activeWorkers)
            .jobsProcessing(jobsProcessing)
            .uptime(formatDuration(uptime))
            .workerDetails(workerDetails)
            .build();
    }
    
    /**
     * Add more workers to the pool.
     */
    public void scaleUp(int additionalWorkers) {
        if (executorService == null || executorService.isShutdown()) {
            log.error("Cannot scale up: worker service is not running");
            return;
        }
        
        log.info("Adding {} workers to the pool", additionalWorkers);
        
        for (int i = 0; i < additionalWorkers; i++) {
            Worker worker = Worker.create(jobService, commandExecutor, queueConfig);
            workers.put(worker.getWorkerId(), worker);
            executorService.submit(worker);
            
            log.debug("Added worker {}", worker.getWorkerId());
        }
        
        log.info("Successfully added {} workers (total: {})", additionalWorkers, workers.size());
    }
    
    /**
     * Remove workers from the pool.
     */
    public void scaleDown(int workersToRemove) {
        if (workersToRemove >= workers.size()) {
            log.error("Cannot remove {} workers: only {} workers running", workersToRemove, workers.size());
            return;
        }
        
        log.info("Removing {} workers from the pool", workersToRemove);
        
        List<Worker> workersToStop = workers.values().stream()
            .filter(worker -> worker.getCurrentJobId() == null) // Prefer idle workers
            .limit(workersToRemove)
            .collect(Collectors.toList());
        
        // If not enough idle workers, include busy ones
        if (workersToStop.size() < workersToRemove) {
            workers.values().stream()
                .filter(worker -> !workersToStop.contains(worker))
                .limit(workersToRemove - workersToStop.size())
                .forEach(workersToStop::add);
        }
        
        // Stop selected workers
        workersToStop.forEach(worker -> {
            worker.shutdown();
            workers.remove(worker.getWorkerId());
            log.debug("Removed worker {}", worker.getWorkerId());
        });
        
        log.info("Successfully removed {} workers (remaining: {})", workersToStop.size(), workers.size());
    }
    
    /**
     * Cleanup on application shutdown.
     * Uses a fixed timeout to avoid bean access during Spring shutdown.
     */
    @PreDestroy
    public void cleanup() {
        log.info("WorkerService cleanup initiated");
        // Use fixed 30-second timeout to avoid accessing beans during shutdown
        stopWorkersGracefully(30);
    }
    
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Worker pool status information.
     */
    @lombok.Data
    @lombok.Builder
    public static class WorkerPoolStatus {
        private int totalWorkers;
        private int activeWorkers;
        private int jobsProcessing;
        private String uptime;
        private Map<String, Worker.WorkerStatus> workerDetails;
    }
}
