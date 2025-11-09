package com.queuectl.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core Job entity representing a background task in the queue system.
 * 
 * This class includes all fields required by the assignment plus bonus features:
 * - Basic job information (id, command, state, attempts, etc.)
 * - Priority queues support
 * - Scheduled execution (run_at)
 * - Timeout handling
 * - Execution history tracking
 * - Optimistic locking for concurrency control
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "jobs")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@CompoundIndexes({
    @CompoundIndex(name = "state_priority_runAt", def = "{'state': 1, 'priority.weight': -1, 'runAt': 1}"),
    @CompoundIndex(name = "state_createdAt", def = "{'state': 1, 'createdAt': 1}"),
    @CompoundIndex(name = "workerId_state", def = "{'workerId': 1, 'state': 1}")
})
public class Job {
    
    /**
     * Unique job identifier
     */
    @Id
    @NotBlank(message = "Job ID cannot be blank")
    private String id;
    
    /**
     * Shell command to execute
     */
    @NotBlank(message = "Command cannot be blank")
    private String command;
    
    /**
     * Current job state
     */
    @NotNull(message = "Job state cannot be null")
    @Indexed
    private JobState state;
    
    /**
     * Number of execution attempts
     */
    @Min(value = 0, message = "Attempts cannot be negative")
    @Builder.Default
    private int attempts = 0;
    
    /**
     * Maximum number of retry attempts before moving to DLQ
     */
    @Min(value = 0, message = "Max retries cannot be negative")
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * Job priority for queue ordering
     */
    @NotNull(message = "Job priority cannot be null")
    @Builder.Default
    private JobPriority priority = JobPriority.MEDIUM;
    
    /**
     * When the job was created
     */
    @NotNull(message = "Created timestamp cannot be null")
    @Indexed
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * When the job was last updated
     */
    @NotNull(message = "Updated timestamp cannot be null")
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    /**
     * When the job should be executed (for scheduled jobs)
     * If null, job is ready for immediate execution
     */
    @Indexed
    private Instant runAt;
    
    /**
     * Job timeout duration (bonus feature)
     */
    @Builder.Default
    private Duration timeout = Duration.ofMinutes(30);
    
    /**
     * When the job will timeout (calculated when job starts processing)
     */
    private Instant timeoutAt;
    
    /**
     * ID of the worker currently processing this job
     */
    private String workerId;
    
    /**
     * When the job was claimed by a worker
     */
    private Instant claimedAt;
    
    /**
     * When the job started processing
     */
    private Instant startedAt;
    
    /**
     * When the job finished processing
     */
    private Instant finishedAt;
    
    /**
     * Exit code of the executed command
     */
    private Integer exitCode;
    
    /**
     * Error message if job failed
     */
    private String errorMessage;
    
    /**
     * Additional metadata for the job
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Execution history for retry tracking and debugging
     */
    @Builder.Default
    private List<JobExecution> executionHistory = new ArrayList<>();
    
    /**
     * MongoDB GridFS ID for job output (stdout/stderr)
     */
    private String outputId;
    
    /**
     * Version field for optimistic locking to prevent race conditions
     * This is CRITICAL for preventing duplicate job processing
     */
    @Version
    private Long version;
    
    /**
     * Tags for job categorization and filtering
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    // Utility methods
    
    /**
     * Check if job is ready to be processed
     */
    public boolean isReadyToProcess() {
        return state == JobState.PENDING && 
               (runAt == null || runAt.isBefore(Instant.now()) || runAt.equals(Instant.now()));
    }
    
    /**
     * Check if job has exceeded maximum retry attempts
     */
    public boolean hasExceededMaxRetries() {
        return attempts >= maxRetries;
    }
    
    /**
     * Check if job has timed out
     */
    public boolean hasTimedOut() {
        return timeoutAt != null && Instant.now().isAfter(timeoutAt);
    }
    
    /**
     * Calculate next retry time using exponential backoff
     */
    public Instant calculateNextRetryTime(Duration baseDelay) {
        long backoffSeconds = (long) (baseDelay.getSeconds() * Math.pow(2, attempts));
        return Instant.now().plusSeconds(backoffSeconds);
    }
    
    /**
     * Add execution attempt to history
     */
    public void addExecutionAttempt(JobExecution execution) {
        if (executionHistory == null) {
            executionHistory = new ArrayList<>();
        }
        executionHistory.add(execution);
    }
    
    /**
     * Update job for retry attempt
     */
    public void prepareForRetry(Duration baseDelay) {
        this.attempts++;
        this.state = JobState.PENDING;
        this.runAt = calculateNextRetryTime(baseDelay);
        this.updatedAt = Instant.now();
        this.workerId = null;
        this.claimedAt = null;
        this.startedAt = null;
        this.timeoutAt = null;
    }
    
    /**
     * Mark job as claimed by a worker
     */
    public void claimByWorker(String workerId) {
        this.state = JobState.PROCESSING;
        this.workerId = workerId;
        this.claimedAt = Instant.now();
        this.startedAt = Instant.now();
        this.timeoutAt = Instant.now().plus(timeout);
        this.updatedAt = Instant.now();
    }
    
    /**
     * Mark job as completed successfully
     */
    public void markCompleted(int exitCode, String outputId) {
        this.state = JobState.COMPLETED;
        this.exitCode = exitCode;
        this.outputId = outputId;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.errorMessage = null;
    }
    
    /**
     * Mark job as failed
     */
    public void markFailed(int exitCode, String errorMessage) {
        this.state = JobState.FAILED;
        this.exitCode = exitCode;
        this.errorMessage = errorMessage;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Move job to Dead Letter Queue
     */
    public void moveToDLQ(String reason) {
        this.state = JobState.DEAD;
        this.errorMessage = reason;
        this.finishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
