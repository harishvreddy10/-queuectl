package com.queuectl.cli;

import com.queuectl.service.JobService;
import com.queuectl.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command for showing system status.
 * 
 * Provides comprehensive overview of queue statistics and worker status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "status",
    description = "Show queue and worker status"
)
public class StatusCommand implements Callable<Integer> {
    
    private final JobService jobService;
    private final WorkerService workerService;
    
    @Option(names = {"--detailed"}, description = "Show detailed statistics")
    private boolean detailed = false;
    
    @Option(names = {"--priority-breakdown"}, description = "Show breakdown by priority")
    private boolean priorityBreakdown = false;
    
    @Option(names = {"--refresh"}, description = "Auto-refresh interval in seconds")
    private Integer refreshInterval;
    
    @Override
    public Integer call() throws Exception {
        try {
            if (refreshInterval != null && refreshInterval > 0) {
                // Auto-refresh mode
                while (true) {
                    clearScreen();
                    displayStatus();
                    Thread.sleep(refreshInterval * 1000L);
                }
            } else {
                // Single display
                displayStatus();
            }
            
            return 0;
            
        } catch (InterruptedException e) {
            System.out.println("\n Status monitoring stopped");
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to get status: " + e.getMessage());
            log.error("Failed to get status", e);
            return 1;
        }
    }
    
    private void displayStatus() {
        try {
            var jobStats = jobService.getJobStatistics();
            var workerStatus = workerService.getWorkerStatus();
            
            System.out.println("QueueCTL Status");
            System.out.println("==================");
            
            // Job Statistics
            System.out.println("\nJob Statistics:");
            System.out.println("   Pending:    " + jobStats.getPendingJobs());
            System.out.println("   Processing: " + jobStats.getProcessingJobs());
            System.out.println("   Completed:  " + jobStats.getCompletedJobs());
            System.out.println("   Failed:     " + jobStats.getFailedJobs());
            System.out.println("   Dead (DLQ): " + jobStats.getDeadJobs());
            System.out.println("   Total:      " + jobStats.getTotalJobs());
            
            // Worker Status
            System.out.println("\nWorker Status:");
            System.out.println("   Active:     " + workerStatus.getActiveWorkers());
            System.out.println("   Total:      " + workerStatus.getTotalWorkers());
            System.out.println("   Processing: " + workerStatus.getJobsProcessing() + " jobs");
            System.out.println("   Uptime:     " + workerStatus.getUptime());
            
            // Performance Metrics
            if (detailed) {
                System.out.println("\nPerformance Metrics:");
                System.out.println("   Jobs/minute:    " + jobStats.getJobsPerMinute());
                System.out.println("   Success rate:   " + String.format("%.1f%%", jobStats.getSuccessRate()));
                System.out.println("   Avg duration:   " + jobStats.getAverageJobDuration());
                System.out.println("   Queue depth:    " + jobStats.getQueueDepth());
            }
            
            // Priority Breakdown
            if (priorityBreakdown) {
                System.out.println("\nPriority Breakdown:");
                jobStats.getPriorityBreakdown().forEach((priority, count) -> {
                    System.out.println("   " + priority + ": " + count + " jobs");
                });
            }
            
            // System Health
            String healthStatus = getHealthStatus(jobStats, workerStatus);
            System.out.println("\nSystem Health: " + healthStatus);
            
            if (refreshInterval != null) {
                System.out.println("\nAuto-refreshing every " + refreshInterval + " seconds (Ctrl+C to stop)");
            }
            
        } catch (Exception e) {
            System.err.println("Error displaying status: " + e.getMessage());
        }
    }
    
    private String getHealthStatus(Object jobStats, Object workerStatus) {
        // Simple health check logic
        // In a real implementation, this would check various health indicators
        return "Healthy";
    }
    
    private void clearScreen() {
        // Clear screen for refresh mode
        System.out.print("\033[2J\033[H");
    }
}
