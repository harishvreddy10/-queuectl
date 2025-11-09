package com.queuectl.cli;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobPriority;
import com.queuectl.domain.JobState;
import com.queuectl.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

/**
 * CLI command for listing jobs with filtering options.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "list",
    description = "List jobs with filtering options"
)
public class ListCommand implements Callable<Integer> {
    
    private final JobService jobService;
    
    @Option(names = {"--state"}, description = "Filter by job state (PENDING, PROCESSING, COMPLETED, FAILED, DEAD)")
    private JobState state;
    
    @Option(names = {"--priority"}, description = "Filter by priority (CRITICAL, HIGH, MEDIUM, LOW)")
    private JobPriority priority;
    
    @Option(names = {"--limit"}, description = "Number of jobs to display per page")
    private Integer limit = 20;
    
    @Option(names = {"--page"}, description = "Page number (0-based)")
    private Integer page = 0;
    
    @Option(names = {"--sort"}, description = "Sort field (createdAt, updatedAt, priority)")
    private String sortField = "createdAt";
    
    @Option(names = {"--order"}, description = "Sort order (asc, desc)")
    private String sortOrder = "desc";
    
    @Option(names = {"--worker"}, description = "Filter by worker ID")
    private String workerId;
    
    @Option(names = {"--format"}, description = "Output format (table, json, csv)")
    private String format = "table";
    
    @Override
    public Integer call() throws Exception {
        try {
            Page<Job> jobs = jobService.listJobs(
                state, priority, workerId, page, limit, sortField, sortOrder
            );
            
            if (jobs.isEmpty()) {
                System.out.println("📭 No jobs found matching the criteria");
                return 0;
            }
            
            switch (format.toLowerCase()) {
                case "json":
                    displayJobsAsJson(jobs);
                    break;
                case "csv":
                    displayJobsAsCsv(jobs);
                    break;
                default:
                    displayJobsAsTable(jobs);
            }
            
            // Pagination info
            if (jobs.getTotalPages() > 1) {
                System.out.println("\nPage " + (page + 1) + " of " + jobs.getTotalPages() + 
                    " (Total: " + jobs.getTotalElements() + " jobs)");
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println(" Failed to list jobs: " + e.getMessage());
            log.error("Failed to list jobs", e);
            return 1;
        }
    }
    
    private void displayJobsAsTable(Page<Job> jobs) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        System.out.println("Jobs List");
        System.out.println("=".repeat(120));
        
        // Header
        System.out.printf("%-20s %-12s %-8s %-30s %-19s %-8s %-12s%n",
            "ID", "STATE", "PRIORITY", "COMMAND", "CREATED", "ATTEMPTS", "WORKER");
        System.out.println("-".repeat(120));
        
        // Jobs
        for (Job job : jobs.getContent()) {
            String createdAt = job.getCreatedAt().toString().substring(0, 19).replace("T", " ");
            String command = job.getCommand().length() > 30 ? 
                job.getCommand().substring(0, 27) + "..." : job.getCommand();
            String workerId = job.getWorkerId() != null ? 
                job.getWorkerId().substring(0, Math.min(12, job.getWorkerId().length())) : "-";
            
            System.out.printf("%-20s %-12s %-8s %-30s %-19s %-8d %-12s%n",
                job.getId().substring(0, Math.min(20, job.getId().length())),
                job.getState(),
                job.getPriority(),
                command,
                createdAt,
                job.getAttempts(),
                workerId
            );
        }
        
        System.out.println("-".repeat(120));
    }
    
    private void displayJobsAsJson(Page<Job> jobs) {
        // Simple JSON output (in a real implementation, use ObjectMapper)
        System.out.println("{");
        System.out.println("  \"jobs\": [");
        
        for (int i = 0; i < jobs.getContent().size(); i++) {
            Job job = jobs.getContent().get(i);
            System.out.println("    {");
            System.out.println("      \"id\": \"" + job.getId() + "\",");
            System.out.println("      \"command\": \"" + job.getCommand() + "\",");
            System.out.println("      \"state\": \"" + job.getState() + "\",");
            System.out.println("      \"priority\": \"" + job.getPriority() + "\",");
            System.out.println("      \"attempts\": " + job.getAttempts() + ",");
            System.out.println("      \"createdAt\": \"" + job.getCreatedAt() + "\"");
            System.out.print("    }");
            if (i < jobs.getContent().size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        
        System.out.println("  ],");
        System.out.println("  \"totalElements\": " + jobs.getTotalElements() + ",");
        System.out.println("  \"totalPages\": " + jobs.getTotalPages() + ",");
        System.out.println("  \"currentPage\": " + jobs.getNumber());
        System.out.println("}");
    }
    
    private void displayJobsAsCsv(Page<Job> jobs) {
        // CSV header
        System.out.println("ID,STATE,PRIORITY,COMMAND,CREATED_AT,ATTEMPTS,WORKER_ID");
        
        // CSV data
        for (Job job : jobs.getContent()) {
            System.out.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                job.getId(),
                job.getState(),
                job.getPriority(),
                job.getCommand().replace("\"", "\"\""), // Escape quotes
                job.getCreatedAt(),
                job.getAttempts(),
                job.getWorkerId() != null ? job.getWorkerId() : ""
            );
        }
    }
}
