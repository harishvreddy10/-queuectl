package com.queuectl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;

/**
 * Service for storing job outputs using MongoDB GridFS.
 * 
 * This implements the job output logging bonus feature.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutputStorageService {
    
    private final GridFsTemplate gridFsTemplate;
    
    /**
     * Store job output (stdout and stderr) in GridFS.
     * 
     * @param jobId Job ID
     * @param stdout Standard output
     * @param stderr Standard error
     * @return GridFS file ID
     */
    public String storeJobOutput(String jobId, String stdout, String stderr) {
        try {
            // Combine stdout and stderr with clear separation
            StringBuilder combinedOutput = new StringBuilder();
            combinedOutput.append("=== JOB OUTPUT ===\n");
            combinedOutput.append("Job ID: ").append(jobId).append("\n");
            combinedOutput.append("Timestamp: ").append(Instant.now()).append("\n");
            combinedOutput.append("\n=== STDOUT ===\n");
            combinedOutput.append(stdout);
            combinedOutput.append("\n=== STDERR ===\n");
            combinedOutput.append(stderr);
            combinedOutput.append("\n=== END ===\n");
            
            // Store in GridFS
            String filename = "job_" + jobId + "_output.log";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                combinedOutput.toString().getBytes()
            );
            
            String fileId = gridFsTemplate.store(inputStream, filename, "text/plain").toString();
            
            log.debug("Stored output for job {} in GridFS with ID {}", jobId, fileId);
            return fileId;
            
        } catch (Exception e) {
            log.error("Failed to store output for job {}: {}", jobId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Retrieve job output from GridFS.
     * 
     * @param outputId GridFS file ID
     * @return Job output as string, or null if not found
     */
    public String retrieveJobOutput(String outputId) {
        try {
            // Implementation for retrieving output
            // This would use GridFsTemplate to find and read the file
            log.debug("Retrieving job output with ID {}", outputId);
            return "Output retrieval not yet implemented";
            
        } catch (Exception e) {
            log.error("Failed to retrieve output {}: {}", outputId, e.getMessage(), e);
            return null;
        }
    }
}
