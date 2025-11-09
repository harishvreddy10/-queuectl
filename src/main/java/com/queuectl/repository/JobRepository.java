package com.queuectl.repository;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;
import com.queuectl.domain.JobPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for Job entities.
 * 
 * This interface provides basic CRUD operations and custom queries.
 * Complex atomic operations are handled by JobRepositoryCustom implementation.
 */
@Repository
public interface JobRepository extends MongoRepository<Job, String>, JobRepositoryCustom {
    
    /**
     * Find jobs by state
     */
    List<Job> findByState(JobState state);
    
    /**
     * Find jobs by state with pagination
     */
    Page<Job> findByState(JobState state, Pageable pageable);
    
    /**
     * Find jobs by state and priority
     */
    List<Job> findByStateAndPriority(JobState state, JobPriority priority);
    
    /**
     * Find jobs ready to be processed (pending and runAt <= now)
     */
    @Query("{ 'state': 'PENDING', $or: [ { 'runAt': null }, { 'runAt': { $lte: ?0 } } ] }")
    List<Job> findJobsReadyToProcess(Instant now);
    
    /**
     * Find jobs that have timed out
     */
    @Query("{ 'state': 'PROCESSING', 'timeoutAt': { $lt: ?0 } }")
    List<Job> findTimedOutJobs(Instant now);
    
    /**
     * Find jobs by worker ID
     */
    List<Job> findByWorkerId(String workerId);
    
    /**
     * Find jobs created within a time range
     */
    List<Job> findByCreatedAtBetween(Instant start, Instant end);
    
    /**
     * Count jobs by state
     */
    long countByState(JobState state);
    
    /**
     * Find jobs scheduled for future execution
     */
    @Query("{ 'state': 'SCHEDULED', 'runAt': { $lte: ?0 } }")
    List<Job> findScheduledJobsReadyToRun(Instant now);
    
    /**
     * Find old completed jobs for cleanup
     */
    @Query("{ 'state': 'COMPLETED', 'finishedAt': { $lt: ?0 } }")
    List<Job> findOldCompletedJobs(Instant before);
    
    /**
     * Find old failed jobs for cleanup
     */
    @Query("{ 'state': { $in: ['FAILED', 'DEAD'] }, 'updatedAt': { $lt: ?0 } }")
    List<Job> findOldFailedJobs(Instant before);
}
