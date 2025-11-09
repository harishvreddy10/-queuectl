package com.queuectl.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

/**
 * MongoDB configuration for QueueCTL.
 * 
 * This configuration provides:
 * - Transaction support for atomic operations
 * - GridFS for storing job outputs
 * - Custom converters for proper date handling
 * - Connection optimization for production use
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    
    @Value("${spring.application.name}")
    private String databaseName;
    
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }
    
    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }
    
    /**
     * Enable MongoDB transactions for atomic job operations.
     * This is CRITICAL for preventing race conditions in job processing.
     */
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
    
    /**
     * GridFS template for storing job outputs (bonus feature).
     * Allows storing large command outputs efficiently.
     */
    @Bean
    public GridFsTemplate gridFsTemplate(MongoDatabaseFactory factory, 
                                        org.springframework.data.mongodb.core.convert.MappingMongoConverter converter) {
        return new GridFsTemplate(factory, converter);
    }
    
    /**
     * Custom converters for proper Java 8 time handling.
     * Ensures Instant objects are properly serialized/deserialized.
     */
    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
            // Convert Instant to ZonedDateTime for MongoDB storage
            new org.springframework.core.convert.converter.Converter<Instant, ZonedDateTime>() {
                @Override
                public ZonedDateTime convert(Instant source) {
                    return source.atZone(ZoneOffset.UTC);
                }
            },
            // Convert ZonedDateTime back to Instant
            new org.springframework.core.convert.converter.Converter<ZonedDateTime, Instant>() {
                @Override
                public Instant convert(ZonedDateTime source) {
                    return source.toInstant();
                }
            }
        ));
    }
    
    @Override
    protected Collection<String> getMappingBasePackages() {
        return Arrays.asList("com.queuectl.domain");
    }
}
