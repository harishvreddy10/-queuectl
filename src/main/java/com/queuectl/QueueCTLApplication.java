package com.queuectl;

import com.queuectl.cli.QueueCTLCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

/**
 * Main application class for QueueCTL - Enterprise Job Queue System.
 * 
 * This application provides:
 * - CLI-based job queue management
 * - MongoDB persistence with transactions
 * - Multi-worker job processing
 * - Retry mechanism with exponential backoff
 * - Dead Letter Queue for failed jobs
 * - Comprehensive metrics and logging
 * 
 * @author Harish
 */
@Slf4j
@SpringBootApplication
@EnableMongoAuditing
@EnableScheduling
@EnableAsync
// @EnableTransactionManagement // Disabled for standalone MongoDB demo
@RequiredArgsConstructor
public class QueueCTLApplication {
    
    private final QueueCTLCommand queueCTLCommand;
    private final ApplicationContext applicationContext;
    
    public static void main(String[] args) {
        // Set system properties for better CLI experience
        System.setProperty("spring.output.ansi.enabled", "always");
        System.setProperty("logging.pattern.console", "%clr(%d{yyyy-MM-dd HH:mm:ss}){faint} %clr(%-5level) %clr([%thread]){faint} %clr(%logger{36}){cyan} - %msg%n");
        
        SpringApplication app = new SpringApplication(QueueCTLApplication.class);
        
        // Configure for CLI usage
        app.setLogStartupInfo(false);
        app.setRegisterShutdownHook(true);
        
        System.exit(SpringApplication.exit(app.run(args)));
    }
    
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            // Create Picocli CommandLine with Spring factory for dependency injection
            CommandLine commandLine = new CommandLine(queueCTLCommand, new PicocliSpringFactory(applicationContext));
            
            // Execute the command and get exit code
            int exitCode = commandLine.execute(args);
            
            // Set exit code for Spring Boot
            System.exit(exitCode);
        };
    }
}
