package com.queuectl.cli;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command for detailed statistics (bonus feature).
 */
@Component
@RequiredArgsConstructor
@Command(name = "stats", description = "Show detailed execution statistics")
public class StatsCommand implements Callable<Integer> {
    
    @Option(names = {"--detailed"}, description = "Show detailed metrics")
    private boolean detailed = false;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Detailed Statistics");
        // Implementation will be added with MetricsService
        return 0;
    }
}
