package com.queuectl.repository;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;

import java.time.Instant;
import java.util.Optional;

/**
 * Custom repository interface for atomic job operations.
 * 
 * This interface defines atomic operations that are CRITICAL
 * for preventing race conditions and duplicate job processing.
 */
public interface JobRepositoryCustom {
    
    /**
     * Atomically claim the next available job for processing.
     * 
     * This method uses MongoDB's findAndModify operation to:
     * 1. Find a job that is PENDING and ready to run
     * 2. Atomically update it to PROCESSING state
     * 3. Set the worker ID and timestamps
     * 
     * This prevents race conditions where multiple workers
     * could claim the same job simultaneously.
     * 
     * @param workerId ID of the worker claiming the job
     * @return Optional containing the claimed job, or empty if no jobs available
     */
    Optional<Job> claimNextJob(String workerId);
    
    /**
     * Atomically release a job back to PENDING state.
     * Used when a worker crashes or needs to release a job.
     * 
     * @param jobId ID of the job to release
     * @param workerId ID of the worker releasing the job (for verification)
     * @return true if job was successfully released, false otherwise
     */
    boolean releaseJob(String jobId, String workerId);
    
    /**
     * Atomically update job state with optimistic locking.
     * 
     * @param jobId ID of the job to update
     * @param expectedVersion Expected version for optimistic locking
     * @param newState New state to set
     * @return Updated job if successful, empty if version mismatch
     */
    Optional<Job> updateJobState(String jobId, Long expectedVersion, JobState newState);
    
    /**
     * Atomically increment job attempts and schedule retry.
     * 
     * @param jobId ID of the job to retry
     * @param nextRunAt When the job should be retried
     * @return Updated job if successful
     */
    Optional<Job> scheduleRetry(String jobId, Instant nextRunAt);
    
    /**
     * Atomically move job to Dead Letter Queue.
     * 
     * @param jobId ID of the job to move to DLQ
     * @param reason Reason for moving to DLQ
     * @return Updated job if successful
     */
    Optional<Job> moveToDeadLetterQueue(String jobId, String reason);
    
    /**
     * Reset all PROCESSING jobs to PENDING state.
     * Used during application startup to recover from crashes.
     * 
     * @return Number of jobs reset
     */
    long resetProcessingJobs();
    
    /**
     * Reset jobs claimed by a specific worker to PENDING state.
     * Used when a worker shuts down gracefully.
     * 
     * @param workerId ID of the worker
     * @return Number of jobs reset
     */
    long resetWorkerJobs(String workerId);
}
