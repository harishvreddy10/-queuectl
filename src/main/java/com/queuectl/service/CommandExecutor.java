package com.queuectl.service;

import com.queuectl.domain.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for executing shell commands with proper timeout handling.
 * 
 * This service handles the actual job execution and implements
 * timeout functionality (bonus feature).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutor {
    
    private final OutputStorageService outputStorageService;
    
    /**
     * Execute a job's command with timeout handling.
     * 
     * @param job Job to execute
     * @return Execution result with exit code and output
     */
    public ExecutionResult executeJob(Job job) {
        log.info("Executing job {}: {}", job.getId(), job.getCommand());
        
        Instant startTime = Instant.now();
        
        try {
            // Handle Windows vs Unix command execution
            ProcessBuilder processBuilder;
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) {
                // Windows: Use cmd /c to execute commands
                processBuilder = new ProcessBuilder("cmd", "/c", job.getCommand());
            } else {
                // Unix/Linux: Use sh -c to execute commands  
                processBuilder = new ProcessBuilder("sh", "-c", job.getCommand());
            }
            
            processBuilder.redirectErrorStream(false); // Keep stdout and stderr separate
            
            // Start process
            Process process = processBuilder.start();
            
            // Capture output asynchronously
            CompletableFuture<String> stdoutFuture = captureOutput(process.getInputStream());
            CompletableFuture<String> stderrFuture = captureOutput(process.getErrorStream());
            
            // Wait for process completion with timeout
            boolean finished = process.waitFor(job.getTimeout().getSeconds(), TimeUnit.SECONDS);
            
            if (!finished) {
                // Process timed out
                log.warn("Job {} timed out after {}", job.getId(), job.getTimeout());
                process.destroyForcibly();
                
                return ExecutionResult.builder()
                    .success(false)
                    .exitCode(-1)
                    .errorMessage("Command execution timed out after " + job.getTimeout())
                    .duration(Duration.between(startTime, Instant.now()))
                    .build();
            }
            
            // Get exit code
            int exitCode = process.exitValue();
            
            // Get output
            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            
            // Store output (bonus feature - job output logging)
            String outputId = null;
            if (!stdout.isEmpty() || !stderr.isEmpty()) {
                outputId = outputStorageService.storeJobOutput(job.getId(), stdout, stderr);
            }
            
            Duration executionTime = Duration.between(startTime, Instant.now());
            
            boolean success = exitCode == 0;
            String errorMessage = success ? null : 
                (stderr.isEmpty() ? "Command failed with exit code " + exitCode : stderr);
            
            log.info("Job {} completed with exit code {} in {}", 
                job.getId(), exitCode, executionTime);
            
            return ExecutionResult.builder()
                .success(success)
                .exitCode(exitCode)
                .stdout(stdout)
                .stderr(stderr)
                .outputId(outputId)
                .errorMessage(errorMessage)
                .duration(executionTime)
                .build();
            
        } catch (IOException e) {
            log.error("Failed to start process for job {}: {}", job.getId(), e.getMessage());
            
            return ExecutionResult.builder()
                .success(false)
                .exitCode(-1)
                .errorMessage("Failed to start command: " + e.getMessage())
                .duration(Duration.between(startTime, Instant.now()))
                .build();
            
        } catch (InterruptedException e) {
            log.warn("Job {} execution was interrupted", job.getId());
            Thread.currentThread().interrupt();
            
            return ExecutionResult.builder()
                .success(false)
                .exitCode(-1)
                .errorMessage("Command execution was interrupted")
                .duration(Duration.between(startTime, Instant.now()))
                .build();
            
        } catch (TimeoutException e) {
            log.error("Timeout while capturing output for job {}", job.getId());
            
            return ExecutionResult.builder()
                .success(false)
                .exitCode(-1)
                .errorMessage("Timeout while capturing command output")
                .duration(Duration.between(startTime, Instant.now()))
                .build();
            
        } catch (Exception e) {
            log.error("Unexpected error executing job {}: {}", job.getId(), e.getMessage(), e);
            
            return ExecutionResult.builder()
                .success(false)
                .exitCode(-1)
                .errorMessage("Unexpected error: " + e.getMessage())
                .duration(Duration.between(startTime, Instant.now()))
                .build();
        }
    }
    
    /**
     * Capture output from an input stream asynchronously.
     */
    private CompletableFuture<String> captureOutput(java.io.InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder output = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                log.warn("Error reading process output: {}", e.getMessage());
            }
            
            return output.toString();
        });
    }
    
    /**
     * Validate command for security (basic validation).
     * In production, this would be more sophisticated.
     */
    public boolean isCommandSafe(String command) {
        // Basic security checks
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        // Block obviously dangerous commands (extend as needed)
        String[] dangerousCommands = {
            "rm -rf", "format", "del /f", "shutdown", "reboot", 
            "mkfs", "dd if=", ":(){ :|:& };:"
        };
        
        String lowerCommand = command.toLowerCase();
        for (String dangerous : dangerousCommands) {
            if (lowerCommand.contains(dangerous)) {
                log.warn("Blocked potentially dangerous command: {}", command);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Result of command execution.
     */
    @lombok.Data
    @lombok.Builder
    public static class ExecutionResult {
        private boolean success;
        private int exitCode;
        private String stdout;
        private String stderr;
        private String outputId; // GridFS ID for stored output
        private String errorMessage;
        private Duration duration;
    }
}
