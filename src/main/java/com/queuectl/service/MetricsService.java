package com.queuectl.service;

import com.queuectl.domain.Job;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for collecting metrics and statistics.
 * 
 * This implements the metrics/execution stats bonus feature.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * Record job enqueued metric.
     */
    public void recordJobEnqueued(Job job) {
        Counter.builder("queuectl.jobs.enqueued")
            .tag("priority", job.getPriority().name())
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record job started metric.
     */
    public void recordJobStarted(Job job) {
        Counter.builder("queuectl.jobs.started")
            .tag("priority", job.getPriority().name())
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record job completed metric.
     */
    public void recordJobCompleted(Job job) {
        Counter.builder("queuectl.jobs.completed")
            .tag("priority", job.getPriority().name())
            .register(meterRegistry)
            .increment();
        
        // Record execution time if available
        if (job.getStartedAt() != null && job.getFinishedAt() != null) {
            Timer.builder("queuectl.jobs.execution.time")
                .tag("priority", job.getPriority().name())
                .register(meterRegistry)
                .record(java.time.Duration.between(job.getStartedAt(), job.getFinishedAt()));
        }
    }
    
    /**
     * Record job retried metric.
     */
    public void recordJobRetried(Job job) {
        Counter.builder("queuectl.jobs.retried")
            .tag("priority", job.getPriority().name())
            .tag("attempt", String.valueOf(job.getAttempts()))
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Record job moved to DLQ metric.
     */
    public void recordJobMovedToDLQ(Job job) {
        Counter.builder("queuectl.jobs.dlq")
            .tag("priority", job.getPriority().name())
            .register(meterRegistry)
            .increment();
    }
}
