package com.queuectl.cli;

import com.queuectl.service.WorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * CLI command for worker management.
 * 
 * Supports starting and stopping workers with proper graceful shutdown.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "worker",
    description = "Manage worker processes",
    subcommands = {
        WorkerCommand.StartCommand.class,
        WorkerCommand.StopCommand.class,
        WorkerCommand.StatusCommand.class
    }
)
public class WorkerCommand implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Worker management commands:");
        System.out.println("  start  - Start worker processes");
        System.out.println("  stop   - Stop worker processes");
        System.out.println("  status - Show worker status");
        return 0;
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "start",
        description = "Start worker processes"
    )
    public static class StartCommand implements Callable<Integer> {
        
        private final WorkerService workerService;
        
        @Option(names = {"--count"}, description = "Number of workers to start")
        private Integer workerCount;
        
        @Option(names = {"--daemon"}, description = "Run workers in daemon mode")
        private boolean daemon = false;
        
        @Override
        public Integer call() throws Exception {
            try {
                int count = workerCount != null ? workerCount : 5; // Default from config
                
                System.out.println(" Starting " + count + " worker(s)...");
                
                // WorkerService will handle the output messages
                workerService.startWorkers(count, daemon);
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to start workers: " + e.getMessage());
                log.error("Failed to start workers", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "stop",
        description = "Stop worker processes gracefully"
    )
    public static class StopCommand implements Callable<Integer> {
        
        private final WorkerService workerService;
        
        @Option(names = {"--force"}, description = "Force stop workers immediately")
        private boolean force = false;
        
        @Option(names = {"--timeout"}, description = "Timeout for graceful shutdown (seconds)")
        private Integer timeout;
        
        @Override
        public Integer call() throws Exception {
            try {
                System.out.println("Stopping workers...");
                
                if (force) {
                    workerService.stopWorkersImmediately();
                    System.out.println("Workers stopped immediately");
                } else {
                    int timeoutSeconds = timeout != null ? timeout : 30;
                    boolean stopped = workerService.stopWorkersGracefully(timeoutSeconds);
                    
                    if (stopped) {
                        System.out.println("All workers stopped gracefully");
                    } else {
                        System.out.println("Some workers did not stop within timeout, forcing shutdown");
                    }
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to stop workers: " + e.getMessage());
                log.error("Failed to stop workers", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "status",
        description = "Show worker status information"
    )
    public static class StatusCommand implements Callable<Integer> {
        
        private final WorkerService workerService;
        
        @Override
        public Integer call() throws Exception {
            try {
                var workerStatus = workerService.getWorkerStatus();
                
                System.out.println("Worker Status:");
                System.out.println("   Active Workers: " + workerStatus.getActiveWorkers());
                System.out.println("   Total Workers: " + workerStatus.getTotalWorkers());
                System.out.println("   Jobs Processing: " + workerStatus.getJobsProcessing());
                System.out.println("   Uptime: " + workerStatus.getUptime());
                
                if (!workerStatus.getWorkerDetails().isEmpty()) {
                    System.out.println("\nWorker Details:");
                    workerStatus.getWorkerDetails().forEach((workerId, details) -> {
                        System.out.println("   " + workerId + ": " + details.getStatus() + 
                            (details.getCurrentJobId() != null ? " (processing " + details.getCurrentJobId() + ")" : ""));
                    });
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to get worker status: " + e.getMessage());
                log.error("Failed to get worker status", e);
                return 1;
            }
        }
    }
}
