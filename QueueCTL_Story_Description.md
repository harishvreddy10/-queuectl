# QueueCTL - Job Queue System Story Description

## 📖 **Project Overview**

QueueCTL is a CLI-based background job queue system designed to manage and execute background tasks with enterprise-grade reliability features. Think of it as a simplified version of systems like Redis Queue (RQ), Celery, or AWS SQS, but implemented as a standalone Java application.

### **What Problem Does It Solve?**

In modern applications, we often need to:
- Execute long-running tasks without blocking the main application
- Retry failed operations automatically
- Handle system crashes gracefully without losing work
- Scale processing by adding more workers
- Monitor and manage background job execution

### **Real-World Use Cases**
- **Email Processing**: Queue email sending tasks that can be retried if SMTP server is down
- **Image Processing**: Resize/compress images uploaded by users
- **Data Import/Export**: Process large CSV files in background
- **Report Generation**: Generate PDF reports without blocking user interface
- **API Integrations**: Call external APIs with retry logic for transient failures

---

## 🎯 **Core Functionality Requirements**

### **1. Job Management**
- **Enqueue Jobs**: Add new background tasks to the queue
- **Job Persistence**: Jobs survive application restarts
- **Job States**: Track job lifecycle (pending → processing → completed/failed/dead)
- **Job Metadata**: Store creation time, attempts, retry configuration

### **2. Worker System**
- **Multiple Workers**: Run concurrent worker processes
- **Job Locking**: Prevent duplicate job processing
- **Graceful Shutdown**: Complete current jobs before stopping
- **Command Execution**: Execute shell commands and capture results

### **3. Reliability Features**
- **Automatic Retries**: Retry failed jobs with configurable limits
- **Exponential Backoff**: Increase delay between retry attempts
- **Dead Letter Queue (DLQ)**: Store permanently failed jobs
- **Failure Handling**: Distinguish between retryable and permanent failures

### **4. Configuration Management**
- **Retry Policies**: Configurable max retries and backoff strategies
- **Worker Settings**: Configure number of workers and timeouts
- **Storage Options**: Configure persistence mechanism

### **5. Monitoring & Operations**
- **Status Dashboard**: View queue statistics and worker status
- **Job Listing**: Filter and view jobs by state
- **DLQ Management**: View and retry dead letter queue jobs
- **Logging**: Track job execution and system events

---

## 🏗️ **System Architecture**

### **Core Components**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CLI Interface │    │  Job Queue      │    │  Worker Pool    │
│                 │    │                 │    │                 │
│ • Commands      │◄──►│ • Job Storage   │◄──►│ • Job Execution │
│ • Configuration │    │ • State Mgmt    │    │ • Retry Logic   │
│ • Status Display│    │ • Locking       │    │ • Error Handling│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌─────────────────┐
                    │  Persistence    │
                    │                 │
                    │ • MongoDB       │
                    │ • Job History   │
                    │ • Configuration │
                    │ • Real-time     │
                    │   Change Streams│
                    └─────────────────┘
```

### **Data Flow**

1. **Job Submission**: CLI enqueues job → Stored in database with "pending" state
2. **Job Processing**: Worker picks up job → Updates state to "processing" → Executes command
3. **Success Path**: Command succeeds → Update state to "completed" → Log results
4. **Failure Path**: Command fails → Increment attempts → Apply backoff → Retry or move to DLQ
5. **Monitoring**: CLI queries database → Displays current status and statistics

---

## 📋 **Detailed Subtasks Breakdown**

### **Phase 1: Foundation & Core Infrastructure**

#### **1.1 Project Setup & Structure**
- [ ] Initialize Java project with Maven/Gradle
- [ ] Set up project structure with proper package organization
- [ ] Configure logging framework (SLF4J + Logback)
- [ ] Add essential dependencies (CLI framework, database, JSON processing)
- [ ] Create basic configuration management system

#### **1.2 Data Models & Persistence**
- [ ] Design Job entity with all required fields
- [ ] Create JobState enum (PENDING, PROCESSING, COMPLETED, FAILED, DEAD)
- [ ] Set up MongoDB connection and configuration
- [ ] Create JobRepository with MongoDB operations (Spring Data MongoDB)
- [ ] Implement MongoDB indexes for performance (job state, timestamps, priorities)
- [ ] Add MongoDB change streams for real-time monitoring
- [ ] Implement atomic job locking using MongoDB findAndModify operations

#### **1.3 CLI Framework Setup**
- [ ] Set up CLI framework (Picocli recommended for Java)
- [ ] Create main QueueCTL command with subcommands
- [ ] Implement help system and command validation
- [ ] Add configuration file support (YAML/Properties)
- [ ] Create CLI output formatting utilities

### **Phase 2: Core Job Queue Operations**

#### **2.1 Job Enqueue System**
- [ ] Implement `queuectl enqueue` command with priority support
- [ ] Add JSON job specification parsing and validation
- [ ] Create job ID generation (UUID-based)
- [ ] Add job scheduling with `run_at` timestamps (BONUS FEATURE)
- [ ] Implement multi-level priority queues (CRITICAL, HIGH, MEDIUM, LOW)
- [ ] Add job timeout configuration per job (BONUS FEATURE)

#### **2.2 Job Execution Engine**
- [ ] Create CommandExecutor for running shell commands
- [ ] Implement process management and timeout handling (BONUS FEATURE)
- [ ] Add command output capture and streaming logging (BONUS FEATURE)
- [ ] Create exit code interpretation logic
- [ ] Add security considerations for command execution
- [ ] Implement job output persistence in MongoDB GridFS (BONUS FEATURE)

#### **2.3 Worker Management System**
- [ ] Design Worker class with job polling mechanism
- [ ] Implement `queuectl worker start/stop` commands
- [ ] Create WorkerPool for managing multiple workers
- [ ] Add graceful shutdown with signal handling
- [ ] Implement worker health monitoring and restart logic

### **Phase 3: Reliability & Error Handling**

#### **3.1 Retry Mechanism**
- [ ] Implement exponential backoff algorithm
- [ ] Create RetryPolicy configuration system
- [ ] Add job attempt tracking and state transitions
- [ ] Implement retry scheduling with delays
- [ ] Add configurable retry limits per job type

#### **3.2 Dead Letter Queue (DLQ)**
- [ ] Create DLQ storage and management
- [ ] Implement automatic job movement to DLQ
- [ ] Add `queuectl dlq list` command
- [ ] Implement `queuectl dlq retry` functionality
- [ ] Create DLQ cleanup and archival policies

#### **3.3 Concurrency & Locking**
- [ ] Implement distributed locking for job processing
- [ ] Add database-level job reservation system
- [ ] Handle worker crash scenarios and job recovery
- [ ] Implement heartbeat mechanism for active jobs
- [ ] Add deadlock detection and resolution

### **Phase 4: Monitoring & Operations**

#### **4.1 Status & Monitoring**
- [ ] Implement `queuectl status` command with comprehensive statistics (BONUS FEATURE)
- [ ] Create `queuectl list` with filtering options
- [ ] Add job execution history and metrics collection (BONUS FEATURE)
- [ ] Implement real-time status updates using MongoDB Change Streams
- [ ] Create performance monitoring and alerting (BONUS FEATURE)
- [ ] Build minimal web dashboard for monitoring (BONUS FEATURE)

#### **4.2 Configuration Management**
- [ ] Implement `queuectl config` commands
- [ ] Add runtime configuration updates
- [ ] Create configuration validation and defaults
- [ ] Add environment-specific configuration profiles
- [ ] Implement configuration backup and restore

#### **4.3 Bonus Features Implementation (HIGH PRIORITY)**
- [ ] **Job Timeout Handling**: Configurable per-job timeouts with automatic cancellation
- [ ] **Job Priority Queues**: Multi-level priority system (HIGH, MEDIUM, LOW, CRITICAL)
- [ ] **Scheduled/Delayed Jobs**: Support `run_at` field for future execution
- [ ] **Job Output Logging**: Capture and store command stdout/stderr with streaming
- [ ] **Metrics & Execution Stats**: Comprehensive performance and success rate tracking
- [ ] **Minimal Web Dashboard**: Real-time monitoring UI with job statistics and controls

### **Phase 5: Testing & Documentation**

#### **5.1 Testing Strategy**
- [ ] Create unit tests for core components
- [ ] Add integration tests for CLI commands
- [ ] Implement end-to-end workflow tests
- [ ] Create performance and load testing
- [ ] Add chaos engineering tests (worker crashes, database failures)

#### **5.2 Documentation & Demo**
- [ ] Write comprehensive README with setup instructions
- [ ] Create usage examples and tutorials
- [ ] Document architecture and design decisions
- [ ] Record CLI demo video
- [ ] Create troubleshooting guide

---

## 🗄️ **Why MongoDB for QueueCTL?**

### **MongoDB Advantages for Job Queue Systems**

#### **1. Document-Based Storage**
```json
{
  "_id": "job_12345",
  "command": "echo 'Hello World'",
  "state": "pending",
  "attempts": 0,
  "maxRetries": 3,
  "priority": "HIGH",
  "runAt": "2025-11-08T15:30:00Z",
  "createdAt": "2025-11-08T10:30:00Z",
  "updatedAt": "2025-11-08T10:30:00Z",
  "metadata": {
    "userId": "user123",
    "tags": ["email", "notification"],
    "timeout": 300
  },
  "executionHistory": [
    {
      "attempt": 1,
      "startedAt": "2025-11-08T10:31:00Z",
      "finishedAt": "2025-11-08T10:31:05Z",
      "exitCode": 1,
      "stdout": "",
      "stderr": "Connection timeout"
    }
  ]
}
```

#### **2. Atomic Operations & Concurrency**
- **findAndModify**: Atomic job claiming prevents race conditions
- **Optimistic Locking**: Version-based updates for safe concurrent access
- **Transactions**: ACID compliance for complex job state transitions
- **Change Streams**: Real-time notifications for job state changes

#### **3. Performance & Scalability**
- **Compound Indexes**: Fast queries on state + priority + runAt
- **Sharding**: Horizontal scaling for millions of jobs
- **Replica Sets**: High availability and read scaling
- **GridFS**: Store large job outputs and artifacts

#### **4. Advanced Querying**
```java
// Priority-based job selection with MongoDB
Query query = Query.query(
    Criteria.where("state").is("pending")
        .and("runAt").lte(Instant.now())
        .and("priority").in("CRITICAL", "HIGH")
).with(Sort.by(Sort.Direction.DESC, "priority", "createdAt"));
```

### **MongoDB Integration Architecture**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   QueueCTL CLI  │    │  Spring Boot    │    │   MongoDB       │
│                 │    │  Application    │    │                 │
│ • Commands      │◄──►│ • Job Service   │◄──►│ • Jobs Collection│
│ • Config Mgmt   │    │ • Worker Pool   │    │ • DLQ Collection │
│ • Monitoring    │    │ • Scheduler     │    │ • Config Coll.  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                    ┌─────────────────┐    ┌─────────────────┐
                    │  Web Dashboard  │    │  Change Streams │
                    │                 │    │                 │
                    │ • Real-time UI  │    │ • Live Updates  │
                    │ • Job Controls  │    │ • Event Triggers│
                    │ • Metrics       │    │ • Notifications │
                    └─────────────────┘    └─────────────────┘
```

---

## ⚠️ **CRITICAL: Disqualification Prevention**

### **🚨 Must-Have Features (Automatic Disqualification if Missing)**

#### **1. Retry & DLQ Functionality**
```java
// REQUIRED: Exponential backoff implementation
@Component
public class RetryService {
    public Duration calculateBackoff(int attempts, Duration baseDelay) {
        return baseDelay.multipliedBy((long) Math.pow(2, attempts));
    }
    
    public void scheduleRetry(Job job) {
        if (job.getAttempts() >= job.getMaxRetries()) {
            moveToDeadLetterQueue(job);
        } else {
            Duration delay = calculateBackoff(job.getAttempts(), Duration.ofSeconds(1));
            scheduleJobExecution(job, Instant.now().plus(delay));
        }
    }
}
```

#### **2. Race Condition Prevention**
```java
// REQUIRED: Atomic job claiming with MongoDB
@Service
public class JobClaimService {
    public Optional<Job> claimNextJob(String workerId) {
        Query query = Query.query(
            Criteria.where("state").is(JobState.PENDING)
                .and("runAt").lte(Instant.now())
        ).with(Sort.by("priority", "createdAt"));
        
        Update update = Update.update("state", JobState.PROCESSING)
            .set("workerId", workerId)
            .set("claimedAt", Instant.now())
            .inc("version", 1);
            
        // Atomic operation prevents duplicate processing
        return Optional.ofNullable(
            mongoTemplate.findAndModify(query, update, Job.class)
        );
    }
}
```

#### **3. Data Persistence Across Restarts**
```java
// REQUIRED: Robust persistence configuration
@Configuration
public class MongoConfig {
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
    
    // Ensure jobs survive application crashes
    @EventListener
    public void handleShutdown(ContextClosedEvent event) {
        // Mark all PROCESSING jobs as PENDING for retry
        jobService.resetProcessingJobsOnShutdown();
    }
}
```

#### **4. Configuration Management (No Hardcoded Values)**
```java
// REQUIRED: Externalized configuration
@ConfigurationProperties("queuectl")
@Component
public class QueueConfig {
    private int maxWorkers = 5;
    private int maxRetries = 3;
    private Duration baseBackoffDelay = Duration.ofSeconds(1);
    private Duration jobTimeout = Duration.ofMinutes(30);
    
    // All values configurable via application.yml or CLI
}
```

### **🛡️ Common Pitfalls & Prevention Strategies**

#### **1. Race Conditions**
- ❌ **Wrong**: Simple SELECT + UPDATE operations
- ✅ **Correct**: MongoDB findAndModify with atomic updates
- ✅ **Testing**: Concurrent worker tests with shared job queue

#### **2. Job Loss on Restart**
- ❌ **Wrong**: In-memory job storage or incomplete persistence
- ✅ **Correct**: All job state changes persisted immediately
- ✅ **Testing**: Kill application during job processing and verify recovery

#### **3. Infinite Retry Loops**
- ❌ **Wrong**: No retry limits or missing DLQ
- ✅ **Correct**: Configurable max retries with automatic DLQ movement
- ✅ **Testing**: Force job failures and verify DLQ behavior

#### **4. Poor Documentation**
- ❌ **Wrong**: Minimal README with missing setup steps
- ✅ **Correct**: Comprehensive documentation with examples
- ✅ **Required**: Setup, usage, architecture, troubleshooting sections

---

## 🚀 **Java-Specific Enhancements**

### **Modern Java Features to Leverage**

#### **1. Concurrency & Performance**
- **Virtual Threads (Java 19+)**: Use for worker threads to handle thousands of concurrent jobs
- **CompletableFuture**: For asynchronous job processing and chaining
- **Structured Concurrency**: Manage worker lifecycle and cancellation
- **Project Loom**: Lightweight threading for better resource utilization

#### **2. Language Features**
- **Records**: For immutable job data transfer objects
- **Pattern Matching**: For job state transitions and command parsing
- **Switch Expressions**: Clean job state handling logic
- **Text Blocks**: For SQL queries and JSON templates

#### **3. Enterprise Integration**
- **Spring Boot**: Optional web dashboard and REST API
- **Micrometer**: Metrics collection and monitoring integration
- **Jackson**: Advanced JSON processing with custom serializers
- **HikariCP**: High-performance database connection pooling

#### **4. Advanced Architecture Patterns**
- **Event Sourcing**: Track all job state changes for audit trails
- **CQRS**: Separate read/write models for better performance
- **Circuit Breaker**: Prevent cascading failures in job execution
- **Bulkhead Pattern**: Isolate different job types for better resilience

### **Production-Ready Features**

#### **1. Observability**
```java
// Metrics integration
@Timed(name = "job.execution.time")
@Counted(name = "job.execution.count")
public JobResult executeJob(Job job) {
    // Implementation with automatic metrics
}
```

#### **2. Configuration Management**
```java
// Type-safe configuration with validation
@ConfigurationProperties("queuectl")
@Validated
public class QueueConfig {
    @Min(1) @Max(100)
    private int maxWorkers = 5;
    
    @NotNull
    private RetryPolicy retryPolicy = new RetryPolicy();
}
```

#### **3. Health Checks & Monitoring**
```java
// Built-in health checks
@Component
public class QueueHealthIndicator implements HealthIndicator {
    public Health health() {
        // Check worker status, database connectivity, etc.
    }
}
```

---

## 🎯 **Success Criteria**

### **Functional Requirements**
- ✅ All CLI commands work as specified
- ✅ Jobs persist across application restarts
- ✅ Multiple workers process jobs concurrently without conflicts
- ✅ Failed jobs retry with exponential backoff
- ✅ Permanently failed jobs move to DLQ
- ✅ Configuration is manageable via CLI

### **Non-Functional Requirements**
- ✅ **Performance**: Handle 1000+ jobs per minute with 10 workers
- ✅ **Reliability**: 99.9% job completion rate under normal conditions
- ✅ **Scalability**: Support horizontal scaling with multiple instances
- ✅ **Maintainability**: Clean, well-documented, testable code
- ✅ **Usability**: Intuitive CLI with helpful error messages

### **Quality Gates**
- ✅ **Code Coverage**: Minimum 80% test coverage
- ✅ **Performance**: Job processing latency < 100ms overhead
- ✅ **Memory**: Stable memory usage under continuous load
- ✅ **Documentation**: Complete setup and usage documentation
- ✅ **Demo**: Working video demonstration of all features

---

## 🔄 **Implementation Timeline**

| **Phase** | **Duration** | **Key Deliverables** |
|-----------|--------------|---------------------|
| **Phase 1** | 2-3 days | Project setup, data models, CLI framework |
| **Phase 2** | 3-4 days | Job enqueue, worker system, basic execution |
| **Phase 3** | 3-4 days | Retry logic, DLQ, concurrency handling |
| **Phase 4** | 2-3 days | Monitoring, configuration, advanced features |
| **Phase 5** | 2-3 days | Testing, documentation, demo recording |

**Total Estimated Time**: 12-17 days for a comprehensive implementation

---

## 💡 **Innovation Opportunities**

### **1. AI-Powered Job Optimization**
- Machine learning for optimal retry strategies
- Predictive job failure detection
- Intelligent job scheduling based on historical data

### **2. Cloud-Native Features**
- Kubernetes operator for auto-scaling workers
- Cloud storage integration (S3, GCS) for job artifacts
- Distributed tracing with OpenTelemetry

### **3. Developer Experience**
- IDE plugins for job development and testing
- Job DSL for complex workflow definitions
- Visual job flow designer and debugger

### **4. Enterprise Integration**
- LDAP/OAuth integration for authentication
- Audit logging for compliance requirements
- Multi-tenant job isolation and resource quotas

---

## 🌟 **Detailed Bonus Features Implementation**

### **1. Job Timeout Handling**

#### **Implementation Strategy**
```java
@Entity
public class Job {
    private Duration timeout = Duration.ofMinutes(30); // Default timeout
    private Instant timeoutAt; // Calculated when job starts
    
    // Timeout monitoring service
    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void checkTimeouts() {
        List<Job> timedOutJobs = jobRepository.findTimedOutJobs(Instant.now());
        timedOutJobs.forEach(this::handleTimeout);
    }
}
```

#### **CLI Commands**
```bash
# Set timeout for specific job
queuectl enqueue '{"command":"long-task.sh","timeout":"10m"}'

# Configure default timeout
queuectl config set default-timeout 15m

# List timed-out jobs
queuectl list --state timeout
```

### **2. Job Priority Queues**

#### **Priority Levels**
```java
public enum JobPriority {
    CRITICAL(1000),  // System maintenance, alerts
    HIGH(100),       // User-facing operations
    MEDIUM(10),      // Background processing
    LOW(1);          // Cleanup, analytics
    
    private final int weight;
}
```

#### **Priority-Based Processing**
```java
// MongoDB query with priority ordering
Query priorityQuery = Query.query(
    Criteria.where("state").is(JobState.PENDING)
        .and("runAt").lte(Instant.now())
).with(Sort.by(
    Sort.Direction.DESC, "priority.weight",
    Sort.Direction.ASC, "createdAt"
));
```

#### **CLI Commands**
```bash
# Enqueue with priority
queuectl enqueue '{"command":"critical-backup.sh","priority":"CRITICAL"}'

# List jobs by priority
queuectl list --priority HIGH --state pending

# Priority statistics
queuectl status --priority-breakdown
```

### **3. Scheduled/Delayed Jobs (`run_at`)**

#### **Scheduler Implementation**
```java
@Component
public class JobScheduler {
    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    public void processScheduledJobs() {
        List<Job> readyJobs = jobRepository.findJobsReadyToRun(Instant.now());
        readyJobs.forEach(job -> {
            job.setState(JobState.PENDING);
            jobRepository.save(job);
        });
    }
}
```

#### **CLI Commands**
```bash
# Schedule job for future execution
queuectl enqueue '{"command":"daily-report.sh","run_at":"2025-11-09T06:00:00Z"}'

# Schedule recurring job (bonus)
queuectl enqueue '{"command":"cleanup.sh","cron":"0 2 * * *"}'

# List scheduled jobs
queuectl list --state scheduled --sort run_at
```

### **4. Job Output Logging**

#### **Output Capture & Storage**
```java
@Service
public class JobOutputService {
    public JobResult executeWithLogging(Job job) {
        ProcessBuilder pb = new ProcessBuilder(job.getCommand().split(" "));
        
        // Real-time output streaming
        Process process = pb.start();
        
        // Capture stdout and stderr separately
        CompletableFuture<String> stdout = captureStream(process.getInputStream());
        CompletableFuture<String> stderr = captureStream(process.getErrorStream());
        
        // Store in MongoDB GridFS for large outputs
        String outputId = gridFsTemplate.store(
            new ByteArrayInputStream(stdout.get().getBytes()),
            job.getId() + "_output.log"
        ).toString();
        
        return JobResult.builder()
            .exitCode(process.exitValue())
            .outputId(outputId)
            .build();
    }
}
```

#### **CLI Commands**
```bash
# View job output
queuectl logs job_12345

# Stream live output (for running jobs)
queuectl logs job_12345 --follow

# Download output to file
queuectl logs job_12345 --download output.log
```

### **5. Metrics & Execution Stats**

#### **Comprehensive Metrics Collection**
```java
@Component
public class MetricsService {
    private final MeterRegistry meterRegistry;
    
    // Job execution metrics
    @EventListener
    public void onJobCompleted(JobCompletedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("job.execution.time")
            .tag("status", event.getJob().getState().name())
            .tag("priority", event.getJob().getPriority().name())
            .register(meterRegistry));
            
        Counter.builder("job.completed.total")
            .tag("status", event.getJob().getState().name())
            .register(meterRegistry)
            .increment();
    }
}
```

#### **Statistics Dashboard**
```bash
# Overall system stats
queuectl stats

# Detailed metrics
queuectl stats --detailed --time-range 24h

# Export metrics for external monitoring
queuectl stats --export prometheus
```

### **6. Minimal Web Dashboard**

#### **Spring Boot Web Interface**
```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    @GetMapping("/stats")
    public DashboardStats getStats() {
        return DashboardStats.builder()
            .totalJobs(jobService.getTotalJobs())
            .pendingJobs(jobService.getPendingJobs())
            .processingJobs(jobService.getProcessingJobs())
            .completedJobs(jobService.getCompletedJobs())
            .failedJobs(jobService.getFailedJobs())
            .deadJobs(jobService.getDeadJobs())
            .activeWorkers(workerService.getActiveWorkers())
            .build();
    }
    
    @GetMapping("/jobs")
    public Page<Job> getJobs(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) JobState state) {
        return jobService.getJobs(PageRequest.of(page, size), state);
    }
}
```

#### **Real-Time Updates with WebSocket**
```java
@Component
public class JobEventListener {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @EventListener
    public void onJobStateChange(JobStateChangeEvent event) {
        // Send real-time updates to web dashboard
        messagingTemplate.convertAndSend("/topic/jobs", event.getJob());
    }
}
```

#### **Dashboard Features**
- **Real-time job statistics** with auto-refresh
- **Job queue visualization** with state-based filtering
- **Worker status monitoring** with health indicators
- **DLQ management** with retry capabilities
- **Performance charts** showing throughput and success rates
- **Job output viewer** with syntax highlighting

#### **Access Dashboard**
```bash
# Start web dashboard (embedded in main application)
queuectl web --port 8080

# Open dashboard in browser
queuectl web --open

# Dashboard with authentication
queuectl web --auth --users admin:password
```

---

## 📊 **Enhanced Success Metrics**

### **Bonus Feature Targets**
- ✅ **Job Timeout**: 99% of jobs respect timeout limits
- ✅ **Priority Processing**: CRITICAL jobs processed within 5 seconds
- ✅ **Scheduled Jobs**: ±1 second accuracy for scheduled execution
- ✅ **Output Logging**: Support for 100MB+ job outputs
- ✅ **Metrics Collection**: <1ms overhead per job for metrics
- ✅ **Web Dashboard**: <2 second page load times with 1000+ jobs

### **Performance Benchmarks**
- **Throughput**: 10,000+ jobs/minute with 50 workers
- **Latency**: <50ms job pickup time under normal load
- **Scalability**: Support 1M+ jobs in queue without performance degradation
- **Reliability**: 99.99% job completion rate with proper retry handling

This comprehensive breakdown should give you a clear roadmap for implementing QueueCTL with modern Java practices, MongoDB integration, and enterprise-grade features that will definitely earn bonus points!
