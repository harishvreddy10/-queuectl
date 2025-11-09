package com.queuectl.cli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main CLI command for QueueCTL.
 * 
 * This is the entry point for all CLI operations and demonstrates
 * a professional command-line interface structure.
 */
@Slf4j
@Component
@Command(
    name = "queuectl",
    description = "Enterprise-grade CLI-based job queue system",
    version = "QueueCTL 1.0.0",
    mixinStandardHelpOptions = true,
    subcommands = {
        EnqueueCommand.class,
        WorkerCommand.class,
        StatusCommand.class,
        ListCommand.class,
        DLQCommand.class,
        ConfigCommand.class,
        LogsCommand.class,
        StatsCommand.class,
        WebCommand.class
    }
)
public class QueueCTLCommand implements Runnable, ExitCodeGenerator {
    
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;
    
    @Option(names = {"--profile"}, description = "Spring profile to use (dev, prod)")
    private String profile;
    
    private int exitCode = 0;
    
    @Override
    public void run() {
        // Show help when no subcommand is specified
        CommandLine.usage(this, System.out);
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
