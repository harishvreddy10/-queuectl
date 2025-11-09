package com.queuectl.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuectl.domain.Job;
import com.queuectl.domain.JobPriority;
import com.queuectl.domain.JobState;
import com.queuectl.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * CLI command for enqueuing jobs.
 * 
 * Supports both JSON job specification and individual parameters.
 * Includes bonus features: priority, scheduling, timeout.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "enqueue",
    description = "Add a new job to the queue"
)
public class EnqueueCommand implements Callable<Integer> {
    
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    
    @Parameters(
        index = "0", 
        arity = "0..1",
        description = "JSON job specification (alternative to individual options)"
    )
    private String jobJson;
    
    @Option(names = {"-c", "--command"}, description = "Command to execute")
    private String command;
    
    @Option(names = {"-p", "--priority"}, description = "Job priority (CRITICAL, HIGH, MEDIUM, LOW)")
    private JobPriority priority = JobPriority.MEDIUM;
    
    @Option(names = {"--run-at"}, description = "Schedule job for future execution (ISO 8601 format)")
    private String runAt;
    
    @Option(names = {"--timeout"}, description = "Job timeout (e.g., 30m, 1h, 2h30m)")
    private String timeout;
    
    @Option(names = {"--max-retries"}, description = "Maximum retry attempts")
    private Integer maxRetries;
    
    @Option(names = {"--id"}, description = "Custom job ID (auto-generated if not provided)")
    private String jobId;
    
    @Option(names = {"--tags"}, split = ",", description = "Comma-separated tags for job categorization")
    private String[] tags;
    
    @Override
    public Integer call() throws Exception {
        try {
            Job job;
            
            if (jobJson != null && !jobJson.trim().isEmpty()) {
                // Parse JSON job specification
                job = parseJobFromJson(jobJson);
            } else if (command != null) {
                // Build job from individual parameters
                job = buildJobFromParameters();
            } else {
                System.err.println("Error: Either provide JSON job specification or --command option");
                return 1;
            }
            
            // Enqueue the job
            Job enqueuedJob = jobService.enqueueJob(job);
            
            // Output success message
            System.out.println("Job enqueued successfully:");
            System.out.println("   ID: " + enqueuedJob.getId());
            System.out.println("   Command: " + enqueuedJob.getCommand());
            System.out.println("   Priority: " + enqueuedJob.getPriority());
            System.out.println("   State: " + enqueuedJob.getState());
            
            if (enqueuedJob.getRunAt() != null) {
                System.out.println("   Scheduled for: " + enqueuedJob.getRunAt());
            } else {
                System.out.println("   Ready for immediate execution");
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Failed to enqueue job: " + e.getMessage());
            log.error("Failed to enqueue job", e);
            return 1;
        }
    }
    
    private Job parseJobFromJson(String json) throws Exception {
        try {
            // Parse the JSON into a Map first for validation
            @SuppressWarnings("unchecked")
            Map<String, Object> jobMap = objectMapper.readValue(json, Map.class);
            
            // Validate required fields
            if (!jobMap.containsKey("command")) {
                throw new IllegalArgumentException("Job JSON must contain 'command' field");
            }
            
            // Build job from JSON
            Job.JobBuilder builder = Job.builder()
                .id(jobMap.containsKey("id") ? (String) jobMap.get("id") : UUID.randomUUID().toString())
                .command((String) jobMap.get("command"))
                .state(JobState.PENDING);
            
            // Optional fields
            if (jobMap.containsKey("priority")) {
                builder.priority(JobPriority.fromValue((String) jobMap.get("priority")));
            }
            
            if (jobMap.containsKey("max_retries")) {
                builder.maxRetries((Integer) jobMap.get("max_retries"));
            }
            
            if (jobMap.containsKey("run_at")) {
                builder.runAt(Instant.parse((String) jobMap.get("run_at")));
            }
            
            if (jobMap.containsKey("timeout")) {
                builder.timeout(Duration.parse((String) jobMap.get("timeout")));
            }
            
            return builder.build();
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON job specification: " + e.getMessage(), e);
        }
    }
    
    private Job buildJobFromParameters() throws Exception {
        Job.JobBuilder builder = Job.builder()
            .id(jobId != null ? jobId : UUID.randomUUID().toString())
            .command(command)
            .priority(priority)
            .state(JobState.PENDING);
        
        // Parse run-at time
        if (runAt != null) {
            try {
                builder.runAt(Instant.parse(runAt));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid run-at time format. Use ISO 8601 format (e.g., 2025-11-09T06:00:00Z)");
            }
        }
        
        // Parse timeout
        if (timeout != null) {
            try {
                builder.timeout(Duration.parse("PT" + timeout.toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid timeout format. Use format like '30m', '1h', '2h30m'");
            }
        }
        
        // Set max retries
        if (maxRetries != null) {
            builder.maxRetries(maxRetries);
        }
        
        return builder.build();
    }
}
