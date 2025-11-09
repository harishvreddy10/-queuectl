# QueueCTL - Enterprise Job Queue System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-5.0+-green.svg)](https://www.mongodb.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A production-ready, CLI-based background job queue system with enterprise-grade reliability features including retry mechanisms, Dead Letter Queue (DLQ), and comprehensive monitoring.

## **Features**

### **Core Functionality**
- **Job Enqueuing** - Submit background tasks via CLI or JSON
- **Multi-Worker Processing** - Concurrent job execution with configurable worker pools
- **Retry Mechanism** - Exponential backoff with configurable limits
- **Dead Letter Queue** - Automatic handling of permanently failed jobs
- **Persistent Storage** - MongoDB with atomic operations (no job loss on restart)
- **Configuration Management** - Runtime configuration without hardcoded values

### **Bonus Features** 
- **Job Timeout Handling** - Configurable per-job execution timeouts
- **Priority Queues** - 4-level priority system (CRITICAL, HIGH, MEDIUM, LOW)
- **Scheduled Jobs** - Future job execution with `run_at` timestamps
- **Job Output Logging** - Complete stdout/stderr capture with GridFS storage
- **Metrics & Statistics** - Comprehensive performance monitoring with Micrometer
- **Web Dashboard Ready** - REST API foundation for monitoring UI

## 📋 **Requirements**

- **Java 17+** (OpenJDK or Oracle JDK)
- **MongoDB 5.0+** (Local or remote instance)
- **Maven 3.6+** (for building from source)

## 🛠️ **Quick Start**

### **1. Setup MongoDB**

```bash
# Option 1: Using Docker (Recommended)
docker run -d --name queuectl-mongo -p 27017:27017 mongo:5.0

# Option 2: Local MongoDB installation
# Download from: https://www.mongodb.com/try/download/community
```

### **2. Download & Run QueueCTL**

```bash
# Download the pre-built JAR
wget https://github.com/your-username/queuectl/releases/download/v1.0.0/queuectl-1.0.0.jar
# Or build from source
git clone https://github.com/harishvreddy10/-queuectl.git
cd queuectl
mvn clean package -DskipTests

# Run the application
java -jar target/queuectl-1.0.0.jar --help
```

### **3. Basic Usage**

```bash
# Enqueue a simple job
java -jar queuectl-1.0.0.jar enqueue --command "echo 'Hello World'"

# Start workers to process jobs
java -jar queuectl-1.0.0.jar worker start --count 3

# Check system status
java -jar queuectl-1.0.0.jar status

# List all jobs
java -jar queuectl-1.0.0.jar list
```

## 📖 **Detailed Usage**

### **Job Management**

#### **Enqueue Jobs**

```bash
# Simple command execution
java -jar queuectl-1.0.0.jar enqueue --command "echo 'Processing data...'"

# High priority job with timeout
java -jar queuectl-1.0.0.jar enqueue \
  --command "python process_data.py" \
  --priority HIGH \
  --timeout "10m"

# Scheduled job for future execution
java -jar queuectl-1.0.0.jar enqueue \
  --command "backup_database.sh" \
  --run-at "2025-11-09T02:00:00Z" \
  --priority CRITICAL

# Job with custom ID and tags
java -jar queuectl-1.0.0.jar enqueue \
  --command "process_data.sh" \
  --id "custom-job-001" \
  --tags "data-processing,nightly"

# JSON job specification (advanced)
java -jar queuectl-1.0.0.jar enqueue '{
  "id": "data-processing-001",
  "command": "python analyze_logs.py --input /data/logs",
  "priority": "HIGH",
  "max_retries": 5,
  "timeout": "PT30M"
}'
```

#### **Worker Management**

```bash
# Start workers in daemon mode
java -jar queuectl-1.0.0.jar worker start --count 5 --daemon

# Start workers in foreground (blocks until Ctrl+C)
java -jar queuectl-1.0.0.jar worker start --count 3

# Stop workers gracefully
java -jar queuectl-1.0.0.jar worker stop

# Stop workers with custom timeout
java -jar queuectl-1.0.0.jar worker stop --timeout 60

# Force stop workers immediately
java -jar queuectl-1.0.0.jar worker stop --force

# Check worker status
java -jar queuectl-1.0.0.jar worker status
```

#### **Monitoring & Status**

```bash
# System overview
java -jar queuectl-1.0.0.jar status

# Detailed statistics
java -jar queuectl-1.0.0.jar status --detailed

# Priority breakdown
java -jar queuectl-1.0.0.jar status --priority-breakdown

# Auto-refresh status (updates every 5 seconds)
java -jar queuectl-1.0.0.jar status --refresh 5
```

#### **Job Listing & Filtering**

```bash
# List all jobs (paginated)
java -jar queuectl-1.0.0.jar list

# Filter by job state
java -jar queuectl-1.0.0.jar list --state PENDING
java -jar queuectl-1.0.0.jar list --state PROCESSING
java -jar queuectl-1.0.0.jar list --state FAILED

# Filter by priority
java -jar queuectl-1.0.0.jar list --priority HIGH --state PENDING

# Filter by worker ID
java -jar queuectl-1.0.0.jar list --worker worker-123

# Custom pagination
java -jar queuectl-1.0.0.jar list --limit 50 --page 2

# Sort by different fields
java -jar queuectl-1.0.0.jar list --sort priority --order asc
java -jar queuectl-1.0.0.jar list --sort updatedAt --order desc

# Export as JSON
java -jar queuectl-1.0.0.jar list --format json

# Export as CSV
java -jar queuectl-1.0.0.jar list --format csv > jobs.csv
```

### **Dead Letter Queue Management**

```bash
# List jobs in DLQ
java -jar queuectl-1.0.0.jar dlq list

# List DLQ jobs with limit and format
java -jar queuectl-1.0.0.jar dlq list --limit 50 --format json

# Retry a specific job from DLQ
java -jar queuectl-1.0.0.jar dlq retry job_12345

# Retry with reset attempt count
java -jar queuectl-1.0.0.jar dlq retry job_12345 --reset-attempts

# Retry with custom max retries
java -jar queuectl-1.0.0.jar dlq retry job_12345 --max-retries 10

# DLQ statistics
java -jar queuectl-1.0.0.jar dlq stats

# Cleanup old DLQ jobs
java -jar queuectl-1.0.0.jar dlq purge --older-than 30d --confirm

# Purge all DLQ jobs
java -jar queuectl-1.0.0.jar dlq purge --all --confirm
```

### **Configuration Management**

```bash
# View current configuration
java -jar queuectl-1.0.0.jar config list

# Update configuration at runtime
java -jar queuectl-1.0.0.jar config set max-workers 10
java -jar queuectl-1.0.0.jar config set max-retries 5
java -jar queuectl-1.0.0.jar config set base-delay 2s

# Get specific configuration value
java -jar queuectl-1.0.0.jar config get max-workers

# Reset to defaults
java -jar queuectl-1.0.0.jar config reset
```

### **Job Output & Logging**

```bash
# View job output
java -jar queuectl-1.0.0.jar logs job_12345

# Follow live output (for running jobs)
java -jar queuectl-1.0.0.jar logs job_12345 --follow
```

### **Statistics & Metrics**

```bash
# Show detailed execution statistics
java -jar queuectl-1.0.0.jar stats

# Show detailed metrics
java -jar queuectl-1.0.0.jar stats --detailed
```

### **Web Dashboard**

```bash
# Start web dashboard
java -jar queuectl-1.0.0.jar web

# Start web dashboard on custom port
java -jar queuectl-1.0.0.jar web --port 9090

# Start web dashboard and open browser automatically
java -jar queuectl-1.0.0.jar web --open
```

##  **Architecture Overview**

### **System Components**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CLI Interface │    │  Job Queue      │    │  Worker Pool    │
│                 │    │                 │    │                 │
│ • Commands      │◄──►│ • Job Storage   │◄──►│ • Job Execution │
│ • Configuration │    │ • State Mgmt    │    │ • Retry Logic   │
│ • Monitoring    │    │ • Locking       │    │ • Error Handling│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌─────────────────┐
                    │   MongoDB       │
                    │                 │
                    │ • Jobs Collection│
                    │ • DLQ Collection │
                    │ • Config Storage │
                    │ • GridFS Outputs │
                    └─────────────────┘
```

### **Job Lifecycle**

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   PENDING   │───►│ PROCESSING  │───►│ COMPLETED   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                  │                    
       │                  ▼                    
       │           ┌─────────────┐             
       │           │   FAILED    │             
       │           └─────────────┘             
       │                  │                    
       │                  ▼                    
       │           ┌─────────────┐             
       └──────────►│    DEAD     │             
                   │    (DLQ)    │             
                   └─────────────┘             
```

### **Key Design Decisions**

#### **Race Condition Prevention**
- **Atomic Operations**: MongoDB `findAndModify` ensures only one worker claims a job
- **Optimistic Locking**: Version field prevents concurrent job modifications
- **Idempotent Operations**: Safe to retry any operation without side effects

#### **Reliability & Persistence**
- **Crash Recovery**: All PROCESSING jobs reset to PENDING on startup
- **Exponential Backoff**: `delay = baseDelay * (2 ^ attempts)` with configurable limits
- **DLQ Management**: Automatic movement after max retries exceeded

#### **Performance Optimizations**
- **MongoDB Indexes**: Compound indexes on `{state, priority, runAt}` for fast job queries
- **Connection Pooling**: HikariCP for optimal database connections
- **Async Processing**: Non-blocking job execution with CompletableFuture

## ⚙️ **Configuration**

### **Application Properties**

```yaml
# MongoDB Configuration
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/queuectl

# QueueCTL Configuration
queuectl:
  workers:
    max-workers: 5
    poll-interval: 1s
    shutdown-timeout: 30s
  
  retry:
    max-retries: 3
    base-delay: 1s
    max-delay: 300s
  
  jobs:
    default-timeout: 30m
    cleanup-completed-after: 7d
    cleanup-failed-after: 30d
  
  storage:
    output-collection: job_outputs
    max-output-size: 100MB
  
  web:
    enabled: true
    port: 8085
    context-path: /dashboard
```

### **Environment Variables**

```bash
# MongoDB Connection
export MONGODB_URI=mongodb://localhost:27017/queuectl

# Worker Configuration
export MAX_WORKERS=10
export MAX_RETRIES=5

# Enable Web Dashboard
export WEB_DASHBOARD_ENABLED=true
```

## 🧪 **Testing**

### **Run Test Suite**

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Test with MongoDB container
docker run -d --name test-mongo -p 27018:27017 mongo:5.0
export MONGODB_URI=mongodb://localhost:27018/queuectl_test
mvn test
```

### **Manual Testing Scenarios**

```bash
# Test 1: Basic job completion
java -jar queuectl-1.0.0.jar enqueue --command "echo 'test job'"
java -jar queuectl-1.0.0.jar worker start --count 1

# Test 2: Job failure and retry
java -jar queuectl-1.0.0.jar enqueue --command "exit 1"
# Watch job retry with exponential backoff

# Test 3: DLQ functionality
java -jar queuectl-1.0.0.jar enqueue --command "false" --max-retries 2
# Job should move to DLQ after 2 failed attempts

# Test 4: Priority processing
java -jar queuectl-1.0.0.jar enqueue --command "sleep 10" --priority LOW
java -jar queuectl-1.0.0.jar enqueue --command "echo 'priority'" --priority CRITICAL
# CRITICAL job should process first

# Test 5: Scheduled jobs
java -jar queuectl-1.0.0.jar enqueue --command "echo 'future job'" --run-at "2025-11-09T10:00:00Z"
# Job should remain SCHEDULED until specified time
```

## 📊 **Monitoring & Metrics**

### **Built-in Metrics**

- **Job Throughput**: Jobs processed per minute
- **Success Rate**: Percentage of successful job completions
- **Queue Depth**: Number of pending jobs
- **Worker Utilization**: Active workers vs total workers
- **Retry Statistics**: Retry attempts by job priority
- **DLQ Growth**: Rate of jobs moving to Dead Letter Queue

### **Prometheus Integration**

```bash
# Enable Prometheus metrics endpoint
curl http://localhost:8085/actuator/prometheus

# Key metrics available:
# - queuectl_jobs_enqueued_total
# - queuectl_jobs_completed_total
# - queuectl_jobs_execution_time_seconds
# - queuectl_jobs_retried_total
# - queuectl_jobs_dlq_total
```

## 🚨 **Troubleshooting**

### **Common Issues**

#### **MongoDB Connection Failed**
```bash
# Check MongoDB is running
docker ps | grep mongo

# Test connection
mongo mongodb://localhost:27017/queuectl --eval "db.stats()"

# Check application logs
java -jar queuectl-1.0.0.jar status --verbose
```

#### **Jobs Not Processing**
```bash
# Check worker status
java -jar queuectl-1.0.0.jar worker status

# Verify jobs are in correct state
java -jar queuectl-1.0.0.jar list --state PENDING

# Check for job locks (restart if needed)
java -jar queuectl-1.0.0.jar worker stop
java -jar queuectl-1.0.0.jar worker start --count 3
```


##  **Acknowledgments**

- **Spring Boot** - Application framework
- **MongoDB** - Document database
- **Picocli** - CLI framework
- **Micrometer** - Metrics collection
- **Testcontainers** - Integration testing

---
