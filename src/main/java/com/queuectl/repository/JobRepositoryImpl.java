package com.queuectl.repository;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of custom repository operations using MongoDB atomic operations.
 * 
 * This class is CRITICAL for preventing race conditions and ensuring
 * that jobs are not processed by multiple workers simultaneously.
 * 
 * All operations use MongoDB's findAndModify which is atomic at the document level.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepositoryCustom {
    
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Optional<Job> claimNextJob(String workerId) {
        log.debug("Worker {} attempting to claim next job", workerId);
        
        // Query for jobs that are ready to be processed
        // Priority order: CRITICAL > HIGH > MEDIUM > LOW (by enum ordinal, reversed)
        // Then by creation time (FIFO within same priority)
        Query query = new Query()
            .addCriteria(Criteria.where("state").is(JobState.PENDING))
            .addCriteria(new Criteria().orOperator(
                Criteria.where("runAt").lte(Instant.now()),
                Criteria.where("runAt").exists(false),
                Criteria.where("runAt").isNull()
            ))
            .with(Sort.by(
                Sort.Order.asc("createdAt") // Simple FIFO for now
            ))
            .limit(1);
        
        // Atomic update to claim the job
        Update update = new Update()
            .set("state", JobState.PROCESSING)
            .set("workerId", workerId)
            .set("claimedAt", Instant.now())
            .set("startedAt", Instant.now())
            .set("updatedAt", Instant.now())
            .inc("version", 1);
        
        // Use findAndModify for atomic operation
        // Return the updated document (not the original)
        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
        Job claimedJob = mongoTemplate.findAndModify(query, update, options, Job.class);
        
        if (claimedJob != null) {
            // Calculate timeout
            Instant timeoutAt = Instant.now().plus(claimedJob.getTimeout());
            
            // Update timeout separately (job is already claimed)
            Query timeoutQuery = Query.query(Criteria.where("id").is(claimedJob.getId()));
            Update timeoutUpdate = Update.update("timeoutAt", timeoutAt);
            mongoTemplate.updateFirst(timeoutQuery, timeoutUpdate, Job.class);
            
            claimedJob.setTimeoutAt(timeoutAt);
            
            log.info("✅ SUCCESS: Worker {} claimed job {} with priority {}", 
                workerId, claimedJob.getId(), claimedJob.getPriority());
            return Optional.of(claimedJob);
        }
        
        log.warn("❌ CRITICAL: findAndModify returned NULL for worker {} - no jobs claimed despite DB updates!", workerId);
        return Optional.empty();
    }
    
    @Override
    public boolean releaseJob(String jobId, String workerId) {
        log.debug("Worker {} releasing job {}", workerId, jobId);
        
        Query query = new Query()
            .addCriteria(Criteria.where("id").is(jobId))
            .addCriteria(Criteria.where("workerId").is(workerId))
            .addCriteria(Criteria.where("state").is(JobState.PROCESSING));
        
        Update update = new Update()
            .set("state", JobState.PENDING)
            .unset("workerId")
            .unset("claimedAt")
            .unset("startedAt")
            .unset("timeoutAt")
            .set("updatedAt", Instant.now())
            .inc("version", 1);
        
        Job releasedJob = mongoTemplate.findAndModify(query, update, Job.class);
        
        if (releasedJob != null) {
            log.info("Job {} released by worker {}", jobId, workerId);
            return true;
        }
        
        log.warn("Failed to release job {} by worker {} - job not found or not owned", jobId, workerId);
        return false;
    }
    
    @Override
    public Optional<Job> updateJobState(String jobId, Long expectedVersion, JobState newState) {
        log.debug("Updating job {} to state {} with version {}", jobId, newState, expectedVersion);
        
        Query query = new Query()
            .addCriteria(Criteria.where("id").is(jobId))
            .addCriteria(Criteria.where("version").is(expectedVersion));
        
        Update update = new Update()
            .set("state", newState)
            .set("updatedAt", Instant.now())
            .inc("version", 1);
        
        // Set finishedAt for terminal states
        if (newState.isTerminal()) {
            update.set("finishedAt", Instant.now());
        }
        
        Job updatedJob = mongoTemplate.findAndModify(query, update, Job.class);
        
        if (updatedJob != null) {
            log.info("Job {} state updated to {}", jobId, newState);
            return Optional.of(updatedJob);
        }
        
        log.warn("Failed to update job {} - version mismatch or job not found", jobId);
        return Optional.empty();
    }
    
    @Override
    public Optional<Job> scheduleRetry(String jobId, Instant nextRunAt) {
        log.debug("Scheduling retry for job {} at {}", jobId, nextRunAt);
        
        Query query = Query.query(Criteria.where("id").is(jobId));
        
        Update update = new Update()
            .set("state", JobState.PENDING)
            .set("runAt", nextRunAt)
            .inc("attempts", 1)
            .unset("workerId")
            .unset("claimedAt")
            .unset("startedAt")
            .unset("timeoutAt")
            .set("updatedAt", Instant.now())
            .inc("version", 1);
        
        Job retriedJob = mongoTemplate.findAndModify(query, update, Job.class);
        
        if (retriedJob != null) {
            log.info("Job {} scheduled for retry at {} (attempt {})", 
                jobId, nextRunAt, retriedJob.getAttempts() + 1);
            return Optional.of(retriedJob);
        }
        
        log.warn("Failed to schedule retry for job {}", jobId);
        return Optional.empty();
    }
    
    @Override
    public Optional<Job> moveToDeadLetterQueue(String jobId, String reason) {
        log.debug("Moving job {} to DLQ with reason: {}", jobId, reason);
        
        Query query = Query.query(Criteria.where("id").is(jobId));
        
        Update update = new Update()
            .set("state", JobState.DEAD)
            .set("errorMessage", reason)
            .set("finishedAt", Instant.now())
            .set("updatedAt", Instant.now())
            .unset("workerId")
            .unset("claimedAt")
            .unset("startedAt")
            .unset("timeoutAt")
            .inc("version", 1);
        
        Job deadJob = mongoTemplate.findAndModify(query, update, Job.class);
        
        if (deadJob != null) {
            log.warn("Job {} moved to Dead Letter Queue: {}", jobId, reason);
            return Optional.of(deadJob);
        }
        
        log.error("Failed to move job {} to DLQ", jobId);
        return Optional.empty();
    }
    
    @Override
    public long resetProcessingJobs() {
        log.info("Resetting all PROCESSING jobs to PENDING state");
        
        Query query = Query.query(Criteria.where("state").is(JobState.PROCESSING));
        
        Update update = new Update()
            .set("state", JobState.PENDING)
            .unset("workerId")
            .unset("claimedAt")
            .unset("startedAt")
            .unset("timeoutAt")
            .set("updatedAt", Instant.now())
            .inc("version", 1);
        
        long resetCount = mongoTemplate.updateMulti(query, update, Job.class).getModifiedCount();
        
        log.info("Reset {} PROCESSING jobs to PENDING state", resetCount);
        return resetCount;
    }
    
    @Override
    public long resetWorkerJobs(String workerId) {
        log.info("Resetting jobs for worker {}", workerId);
        
        Query query = new Query()
            .addCriteria(Criteria.where("workerId").is(workerId))
            .addCriteria(Criteria.where("state").is(JobState.PROCESSING));
        
        Update update = new Update()
            .set("state", JobState.PENDING)
            .unset("workerId")
            .unset("claimedAt")
            .unset("startedAt")
            .unset("timeoutAt")
            .set("updatedAt", Instant.now())
            .inc("version", 1);
        
        long resetCount = mongoTemplate.updateMulti(query, update, Job.class).getModifiedCount();
        
        log.info("Reset {} jobs for worker {}", resetCount, workerId);
        return resetCount;
    }
}
