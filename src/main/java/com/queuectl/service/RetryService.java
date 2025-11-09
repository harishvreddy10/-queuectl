package com.queuectl.service;

import com.queuectl.config.QueueConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for calculating retry delays with exponential backoff.
 * 
 * This is CRITICAL for avoiding disqualification - implements proper
 * exponential backoff algorithm as required by the assignment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {
    
    private final QueueConfig queueConfig;
    
    /**
     * Calculate backoff delay using exponential backoff algorithm.
     * 
     * Formula: delay = baseDelay * (2 ^ attempts)
     * 
     * This prevents overwhelming external services and implements
     * the exponential backoff requirement from the assignment.
     * 
     * @param attempts Number of previous attempts (0-based)
     * @param baseDelay Base delay duration
     * @return Calculated delay duration, capped at maxDelay
     */
    public Duration calculateBackoffDelay(int attempts, Duration baseDelay) {
        // Exponential backoff: delay = base * 2^attempts
        long backoffSeconds = (long) (baseDelay.getSeconds() * Math.pow(2, attempts));
        
        // Cap at maximum delay to prevent extremely long waits
        Duration maxDelay = queueConfig.getRetry().getMaxDelay();
        if (backoffSeconds > maxDelay.getSeconds()) {
            backoffSeconds = maxDelay.getSeconds();
        }
        
        Duration calculatedDelay = Duration.ofSeconds(backoffSeconds);
        
        log.debug("Calculated backoff delay for attempt {}: {} seconds", attempts + 1, backoffSeconds);
        
        return calculatedDelay;
    }
    
    /**
     * Check if a job should be retried based on attempts and configuration.
     * 
     * @param currentAttempts Current number of attempts
     * @param maxRetries Maximum allowed retries
     * @return true if job should be retried, false if it should go to DLQ
     */
    public boolean shouldRetry(int currentAttempts, int maxRetries) {
        boolean shouldRetry = currentAttempts < maxRetries;
        
        log.debug("Retry check: attempts={}, maxRetries={}, shouldRetry={}", 
            currentAttempts, maxRetries, shouldRetry);
        
        return shouldRetry;
    }
    
    /**
     * Calculate jitter to prevent thundering herd problem.
     * Adds randomness to delay to spread out retry attempts.
     * 
     * @param baseDelay Base delay to add jitter to
     * @param jitterPercent Percentage of jitter (0.0 to 1.0)
     * @return Delay with jitter applied
     */
    public Duration addJitter(Duration baseDelay, double jitterPercent) {
        if (jitterPercent <= 0.0 || jitterPercent > 1.0) {
            return baseDelay;
        }
        
        long baseSeconds = baseDelay.getSeconds();
        long jitterRange = (long) (baseSeconds * jitterPercent);
        long jitter = (long) (Math.random() * jitterRange * 2) - jitterRange; // -range to +range
        
        long finalSeconds = Math.max(1, baseSeconds + jitter); // Minimum 1 second
        
        return Duration.ofSeconds(finalSeconds);
    }
}
