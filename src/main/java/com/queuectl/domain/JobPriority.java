package com.queuectl.domain;

/**
 * Job priority levels for queue processing order.
 * Higher weight values indicate higher priority.
 */
public enum JobPriority {
    /**
     * Critical system operations, alerts, emergency tasks
     */
    CRITICAL(1000, "critical"),
    
    /**
     * User-facing operations, important business processes
     */
    HIGH(100, "high"),
    
    /**
     * Standard background processing
     */
    MEDIUM(10, "medium"),
    
    /**
     * Cleanup tasks, analytics, non-urgent operations
     */
    LOW(1, "low");
    
    private final int weight;
    private final String value;
    
    JobPriority(int weight, String value) {
        this.weight = weight;
        this.value = value;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public String getValue() {
        return value;
    }
    
    public static JobPriority fromValue(String value) {
        for (JobPriority priority : values()) {
            if (priority.value.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown job priority: " + value);
    }
    
    /**
     * Get default priority for jobs when not specified
     */
    public static JobPriority getDefault() {
        return MEDIUM;
    }
}
