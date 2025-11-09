package com.queuectl.cli;

import com.queuectl.domain.Job;
import com.queuectl.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI command for Dead Letter Queue management.
 * 
 * Critical feature for handling permanently failed jobs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "dlq",
    description = "Manage Dead Letter Queue",
    subcommands = {
        DLQCommand.ListCommand.class,
        DLQCommand.RetryCommand.class,
        DLQCommand.PurgeCommand.class,
        DLQCommand.StatsCommand.class
    }
)
public class DLQCommand implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Dead Letter Queue management commands:");
        System.out.println("  list   - List jobs in DLQ");
        System.out.println("  retry  - Retry a job from DLQ");
        System.out.println("  purge  - Remove jobs from DLQ");
        System.out.println("  stats  - Show DLQ statistics");
        return 0;
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "list",
        description = "List jobs in Dead Letter Queue"
    )
    public static class ListCommand implements Callable<Integer> {
        
        private final JobService jobService;
        
        @Option(names = {"--limit"}, description = "Maximum number of jobs to display")
        private Integer limit = 20;
        
        @Option(names = {"--format"}, description = "Output format (table, json)")
        private String format = "table";
        
        @Override
        public Integer call() throws Exception {
            try {
                List<Job> dlqJobs = jobService.getDeadLetterQueueJobs(limit);
                
                if (dlqJobs.isEmpty()) {
                    System.out.println("📭 Dead Letter Queue is empty");
                    return 0;
                }
                
                System.out.println("Dead Letter Queue (" + dlqJobs.size() + " jobs)");
                System.out.println("=".repeat(100));
                
                // Header
                System.out.printf("%-20s %-30s %-19s %-8s %-20s%n",
                    "ID", "COMMAND", "FAILED_AT", "ATTEMPTS", "ERROR");
                System.out.println("-".repeat(100));
                
                // Jobs
                for (Job job : dlqJobs) {
                    String failedAt = job.getFinishedAt() != null ? 
                        job.getFinishedAt().toString().substring(0, 19).replace("T", " ") : "-";
                    String command = job.getCommand().length() > 30 ? 
                        job.getCommand().substring(0, 27) + "..." : job.getCommand();
                    String error = job.getErrorMessage() != null && job.getErrorMessage().length() > 20 ?
                        job.getErrorMessage().substring(0, 17) + "..." : 
                        (job.getErrorMessage() != null ? job.getErrorMessage() : "-");
                    
                    System.out.printf("%-20s %-30s %-19s %-8d %-20s%n",
                        job.getId().substring(0, Math.min(20, job.getId().length())),
                        command,
                        failedAt,
                        job.getAttempts(),
                        error
                    );
                }
                
                System.out.println("-".repeat(100));
                System.out.println("Use 'queuectl dlq retry <job-id>' to retry a specific job");
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to list DLQ jobs: " + e.getMessage());
                log.error("Failed to list DLQ jobs", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "retry",
        description = "Retry a job from Dead Letter Queue"
    )
    public static class RetryCommand implements Callable<Integer> {
        
        private final JobService jobService;
        
        @Parameters(index = "0", description = "Job ID to retry")
        private String jobId;
        
        @Option(names = {"--reset-attempts"}, description = "Reset attempt count to 0")
        private boolean resetAttempts = false;
        
        @Option(names = {"--max-retries"}, description = "Override max retries for this job")
        private Integer maxRetries;
        
        @Override
        public Integer call() throws Exception {
            try {
                Job retriedJob = jobService.retryDeadLetterQueueJob(jobId, resetAttempts, maxRetries);
                
                if (retriedJob != null) {
                    System.out.println("Job " + jobId + " moved from DLQ back to queue");
                    System.out.println("   State: " + retriedJob.getState());
                    System.out.println("   Attempts: " + retriedJob.getAttempts());
                    if (retriedJob.getRunAt() != null) {
                        System.out.println("   Scheduled for: " + retriedJob.getRunAt());
                    }
                } else {
                    System.err.println("Job " + jobId + " not found in Dead Letter Queue");
                    return 1;
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to retry job: " + e.getMessage());
                log.error("Failed to retry job from DLQ", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "purge",
        description = "Remove jobs from Dead Letter Queue"
    )
    public static class PurgeCommand implements Callable<Integer> {
        
        private final JobService jobService;
        
        @Option(names = {"--older-than"}, description = "Remove jobs older than specified duration (e.g., 30d, 7d)")
        private String olderThan;
        
        @Option(names = {"--all"}, description = "Remove all jobs from DLQ")
        private boolean all = false;
        
        @Option(names = {"--confirm"}, description = "Confirm the purge operation")
        private boolean confirm = false;
        
        @Override
        public Integer call() throws Exception {
            try {
                if (!confirm) {
                    System.err.println("Purge operation requires --confirm flag for safety");
                    return 1;
                }
                
                long purgedCount;
                
                if (all) {
                    purgedCount = jobService.purgeAllDeadLetterQueueJobs();
                    System.out.println("Purged " + purgedCount + " jobs from Dead Letter Queue");
                } else if (olderThan != null) {
                    purgedCount = jobService.purgeOldDeadLetterQueueJobs(olderThan);
                    System.out.println("Purged " + purgedCount + " jobs older than " + olderThan + " from Dead Letter Queue");
                } else {
                    System.err.println("Specify either --all or --older-than option");
                    return 1;
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to purge DLQ: " + e.getMessage());
                log.error("Failed to purge DLQ", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "stats",
        description = "Show Dead Letter Queue statistics"
    )
    public static class StatsCommand implements Callable<Integer> {
        
        private final JobService jobService;
        
        @Override
        public Integer call() throws Exception {
            try {
                var dlqStats = jobService.getDeadLetterQueueStatistics();
                
                System.out.println("Dead Letter Queue Statistics");
                System.out.println("================================");
                System.out.println("Total jobs in DLQ: " + dlqStats.getTotalJobs());
                System.out.println("Oldest job: " + dlqStats.getOldestJobAge());
                System.out.println("Most common errors:");
                
                dlqStats.getTopErrors().forEach((error, count) -> {
                    System.out.println("  • " + error + ": " + count + " jobs");
                });
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to get DLQ statistics: " + e.getMessage());
                log.error("Failed to get DLQ statistics", e);
                return 1;
            }
        }
    }
}
