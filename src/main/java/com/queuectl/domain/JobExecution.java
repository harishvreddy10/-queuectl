package com.queuectl.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a single execution attempt of a job.
 * Used for tracking execution history and debugging failed jobs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobExecution {
    
    /**
     * Attempt number (1-based)
     */
    private int attemptNumber;
    
    /**
     * ID of the worker that executed this attempt
     */
    private String workerId;
    
    /**
     * When this execution attempt started
     */
    private Instant startedAt;
    
    /**
     * When this execution attempt finished
     */
    private Instant finishedAt;
    
    /**
     * Exit code returned by the command
     */
    private Integer exitCode;
    
    /**
     * Error message if execution failed
     */
    private String errorMessage;
    
    /**
     * MongoDB GridFS ID for this execution's output
     */
    private String outputId;
    
    /**
     * Whether this execution was successful
     */
    private boolean successful;
    
    /**
     * Duration of this execution attempt
     */
    public Duration getDuration() {
        if (startedAt != null && finishedAt != null) {
            return Duration.between(startedAt, finishedAt);
        }
        return null;
    }
    
    /**
     * Check if this execution is still running
     */
    public boolean isRunning() {
        return startedAt != null && finishedAt == null;
    }
}
