package com.queuectl.cli;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command for web dashboard (bonus feature).
 */
@Component
@RequiredArgsConstructor
@Command(name = "web", description = "Start web dashboard")
public class WebCommand implements Callable<Integer> {
    
    @Option(names = {"--port"}, description = "Port for web dashboard")
    private Integer port = 8080;
    
    @Option(names = {"--open"}, description = "Open browser automatically")
    private boolean open = false;
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Starting web dashboard on port " + port);
        // Implementation will be added with WebService
        return 0;
    }
}
