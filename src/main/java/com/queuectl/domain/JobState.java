package com.queuectl.domain;

/**
 * Represents the lifecycle states of a job in the queue system.
 * 
 * State Transitions:
 * PENDING -> PROCESSING -> COMPLETED/FAILED
 * FAILED -> PENDING (retry) or DEAD (max retries exceeded)
 * SCHEDULED -> PENDING (when run_at time is reached)
 */
public enum JobState {
    /**
     * Job is waiting to be picked up by a worker
     */
    PENDING("pending"),
    
    /**
     * Job is currently being executed by a worker
     */
    PROCESSING("processing"),
    
    /**
     * Job completed successfully
     */
    COMPLETED("completed"),
    
    /**
     * Job failed but can be retried
     */
    FAILED("failed"),
    
    /**
     * Job permanently failed and moved to Dead Letter Queue
     */
    DEAD("dead"),
    
    /**
     * Job is scheduled for future execution
     */
    SCHEDULED("scheduled"),
    
    /**
     * Job was cancelled by user or system
     */
    CANCELLED("cancelled"),
    
    /**
     * Job exceeded its timeout limit
     */
    TIMEOUT("timeout");
    
    private final String value;
    
    JobState(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Check if the job state allows for retry attempts
     */
    public boolean isRetryable() {
        return this == FAILED || this == TIMEOUT;
    }
    
    /**
     * Check if the job is in a terminal state (no further processing)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == DEAD || this == CANCELLED;
    }
    
    /**
     * Check if the job is currently active (being processed)
     */
    public boolean isActive() {
        return this == PROCESSING;
    }
    
    public static JobState fromValue(String value) {
        for (JobState state : values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown job state: " + value);
    }
}
