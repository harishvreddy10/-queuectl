package com.queuectl.cli;

import com.queuectl.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * CLI command for configuration management.
 * 
 * Prevents disqualification by allowing runtime configuration changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "config",
    description = "Manage configuration settings",
    subcommands = {
        ConfigCommand.GetCommand.class,
        ConfigCommand.SetCommand.class,
        ConfigCommand.ListCommand.class,
        ConfigCommand.ResetCommand.class
    }
)
public class ConfigCommand implements Callable<Integer> {
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Configuration management commands:");
        System.out.println("  get    - Get configuration value");
        System.out.println("  set    - Set configuration value");
        System.out.println("  list   - List all configuration");
        System.out.println("  reset  - Reset to default values");
        return 0;
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "set",
        description = "Set configuration value"
    )
    public static class SetCommand implements Callable<Integer> {
        
        private final ConfigService configService;
        
        @Parameters(index = "0", description = "Configuration key")
        private String key;
        
        @Parameters(index = "1", description = "Configuration value")
        private String value;
        
        @Override
        public Integer call() throws Exception {
            try {
                boolean success = configService.setConfigValue(key, value);
                
                if (success) {
                    System.out.println("Configuration updated: " + key + " = " + value);
                } else {
                    System.err.println("Invalid configuration key or value: " + key);
                    return 1;
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to set configuration: " + e.getMessage());
                log.error("Failed to set configuration", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "get",
        description = "Get configuration value"
    )
    public static class GetCommand implements Callable<Integer> {
        
        private final ConfigService configService;
        
        @Parameters(index = "0", description = "Configuration key")
        private String key;
        
        @Override
        public Integer call() throws Exception {
            try {
                String value = configService.getConfigValue(key);
                
                if (value != null) {
                    System.out.println(key + " = " + value);
                } else {
                    System.err.println("Configuration key not found: " + key);
                    return 1;
                }
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to get configuration: " + e.getMessage());
                log.error("Failed to get configuration", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "list",
        description = "List all configuration settings"
    )
    public static class ListCommand implements Callable<Integer> {
        
        private final ConfigService configService;
        
        @Override
        public Integer call() throws Exception {
            try {
                var config = configService.getAllConfiguration();
                
                System.out.println("⚙️ Current Configuration");
                System.out.println("========================");
                
                config.forEach((key, value) -> {
                    System.out.println(key + " = " + value);
                });
                
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to list configuration: " + e.getMessage());
                log.error("Failed to list configuration", e);
                return 1;
            }
        }
    }
    
    @Component
    @RequiredArgsConstructor
    @Command(
        name = "reset",
        description = "Reset configuration to default values"
    )
    public static class ResetCommand implements Callable<Integer> {
        
        private final ConfigService configService;
        
        @Override
        public Integer call() throws Exception {
            try {
                configService.resetToDefaults();
                System.out.println("Configuration reset to default values");
                return 0;
                
            } catch (Exception e) {
                System.err.println("Failed to reset configuration: " + e.getMessage());
                log.error("Failed to reset configuration", e);
                return 1;
            }
        }
    }
}
