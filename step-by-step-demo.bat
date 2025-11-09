@echo off
setlocal enabledelayedexpansion

color 0A
title QueueCTL - Step-by-Step Interactive Demo

echo.
echo ████████████████████████████████████████████████████████████████
echo ██                                                            ██
echo ██              QUEUECTL STEP-BY-STEP DEMO                   ██
echo ██            Interactive Code Walkthrough                    ██
echo ██                                                            ██
echo ████████████████████████████████████████████████████████████████
echo.
echo 🎯 This interactive demo will show you:
echo    • Each command execution step-by-step
echo    • What happens behind the scenes in the code
echo    • Database state changes in MongoDB
echo    • Worker behavior and job processing
echo.
echo Press any key to start the interactive walkthrough...
pause >nul
cls

REM ========================================
REM STEP 1: System Initialization
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │                    STEP 1: INITIALIZATION                  │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Clear any existing jobs to start with a clean system
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar clear --force
echo.
echo 🔍 CODE EXPLANATION:
echo    1. Spring Boot application starts up
echo    2. MongoDB connection established
echo    3. JobService.clearAllJobs() method called
echo    4. MongoDB: db.jobs.deleteMany({}) - removes all documents
echo    5. Returns success message
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Clearing existing jobs...
java -jar target/queuectl-1.0.0.jar clear --force

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • All job documents removed from MongoDB 'jobs' collection
echo    • System is now in clean state for demo
echo.
echo Press any key to continue to status check...
pause >nul
cls

REM ========================================
REM STEP 2: Initial Status Check
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │                 STEP 2: INITIAL STATUS CHECK               │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Check system status to confirm empty state
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar status
echo.
echo 🔍 CODE EXPLANATION:
echo    1. StatusCommand.call() method invoked
echo    2. JobService.getJobStatistics() called
echo    3. MongoDB queries:
echo       - db.jobs.countDocuments({state: "PENDING"})
echo       - db.jobs.countDocuments({state: "PROCESSING"})
echo       - db.jobs.countDocuments({state: "COMPLETED"})
echo       - db.jobs.countDocuments({state: "FAILED"})
echo       - db.jobs.countDocuments({state: "DEAD"})
echo    4. WorkerService.getWorkerStatus() called
echo    5. Formats and displays statistics
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Checking system status...
java -jar target/queuectl-1.0.0.jar status

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • MongoDB queries executed to count jobs by state
echo    • All counts should be 0 (empty system)
echo    • No workers currently running
echo.
echo Press any key to continue to job creation...
pause >nul
cls

REM ========================================
REM STEP 3: Create First Job (Quick Success)
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │               STEP 3: CREATE QUICK SUCCESS JOB             │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Create a job that will complete quickly (2 seconds)
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar enqueue --command "echo QUICK SUCCESS: Task completed rapidly && timeout /t 2 /nobreak"
echo    NOTE: Removed '>nul' redirection to avoid Windows cmd.exe redirection issues
echo.
echo 🔍 CODE EXPLANATION:
echo    1. EnqueueCommand.call() method invoked
echo    2. EnqueueCommand.buildJobFromParameters() called
echo    3. Job object created with:
echo       - id: UUID.randomUUID().toString()
echo       - command: "echo QUICK SUCCESS... && timeout /t 2 /nobreak"
echo       - state: JobState.PENDING
echo       - priority: JobPriority.MEDIUM (default)
echo       - createdAt: Instant.now()
echo       - maxRetries: 3 (default)
echo    4. JobService.enqueueJob(job) called
echo    5. MongoDB: db.jobs.insertOne(jobDocument)
echo    6. Job persisted with PENDING state
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Creating quick success job...
REM Removed '>nul' redirection to avoid Windows cmd.exe redirection issues
REM The command will still succeed, just with visible output
java -jar target/queuectl-1.0.0.jar enqueue --command "echo QUICK SUCCESS: Task completed rapidly && timeout /t 2 /nobreak"

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • New job document inserted into MongoDB
echo    • Job assigned unique UUID
echo    • State set to PENDING
echo    • Ready for worker to pick up
echo.
echo ⚠️  NOTE: Removed '>nul' redirection to avoid Windows cmd.exe redirection issues
echo    that could cause "Input redirection is not supported" errors
echo    The command will succeed, output will be visible in worker logs
echo.
echo Press any key to continue to retry job creation...
pause >nul
cls

REM ========================================
REM STEP 4: Create Retry Job
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │                STEP 4: CREATE SMART RETRY JOB              │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Create a job that will fail once, then succeed on retry
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar enqueue --command "if exist retry_marker.tmp (echo RETRY SUCCESS && del retry_marker.tmp) else (echo RETRY ATTEMPT && echo. > retry_marker.tmp && exit 1)" --max-retries 2
echo.
echo 🔍 CODE EXPLANATION:
echo    1. Same EnqueueCommand flow as before
echo    2. Job created with maxRetries: 2 (custom value)
echo    3. Command logic:
echo       - First run: retry_marker.tmp doesn't exist → creates file → exit 1 (failure)
echo       - Second run: retry_marker.tmp exists → deletes file → exit 0 (success)
echo    4. MongoDB document includes maxRetries: 2
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Creating smart retry job...
REM Removed emojis to avoid Windows cmd.exe encoding issues
java -jar target/queuectl-1.0.0.jar enqueue --command "if exist retry_marker.tmp (echo RETRY SUCCESS: Task recovered successfully && del retry_marker.tmp) else (echo RETRY ATTEMPT: First attempt failed, will retry... && echo. > retry_marker.tmp && exit 1)" --max-retries 2

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • Job created with custom maxRetries configuration
echo    • Command designed to fail first, succeed second time
echo    • Will demonstrate retry mechanism in action
echo.
echo Press any key to continue to failure job creation...
pause >nul
cls

REM ========================================
REM STEP 5: Create Failure Job (DLQ)
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │             STEP 5: CREATE PERMANENT FAILURE JOB           │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Create a job that will always fail and go to Dead Letter Queue
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar enqueue --command "echo CRITICAL ERROR: Unrecoverable failure detected && exit 1" --max-retries 1
echo.
echo 🔍 CODE EXPLANATION:
echo    1. Job created with maxRetries: 1
echo    2. Command always exits with code 1 (failure)
echo    3. After 1 retry attempt, job will be moved to DLQ
echo    4. Demonstrates Dead Letter Queue functionality
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Creating permanent failure job...
REM Removed emoji to avoid Windows cmd.exe encoding issues
java -jar target/queuectl-1.0.0.jar enqueue --command "echo CRITICAL ERROR: Unrecoverable failure detected && exit 1" --max-retries 1

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • Job created that will always fail
echo    • Will retry once, then move to DLQ
echo    • Demonstrates fault tolerance
echo.
echo Press any key to check current job queue...
pause >nul
cls

REM ========================================
REM STEP 6: List Current Jobs
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │                 STEP 6: LIST CURRENT JOBS                  │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    List all jobs to see them in PENDING state
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar list --limit 10
echo.
echo 🔍 CODE EXPLANATION:
echo    1. ListCommand.call() method invoked
echo    2. JobService.getJobs() called with pagination
echo    3. MongoDB query: db.jobs.find({}).sort({createdAt: -1}).limit(10)
echo    4. Results formatted and displayed
echo    5. All jobs should show state: PENDING
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Listing current jobs...
java -jar target/queuectl-1.0.0.jar list --limit 10

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • MongoDB query executed to fetch jobs
echo    • All jobs currently in PENDING state
echo    • Jobs waiting for workers to process them
echo.
echo Press any key to start workers...
pause >nul
cls

REM ========================================
REM STEP 7: Start Workers
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │                   STEP 7: START WORKERS                    │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Start 2 workers to process the jobs
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar worker start --count 2
echo.
echo 🔍 CODE EXPLANATION:
echo    1. WorkerCommand.StartCommand.call() invoked
echo    2. WorkerService.startWorkers(2, false) called
echo    3. ExecutorService created with 2 threads
echo    4. For each worker:
echo       - Worker.create() called
echo       - Worker added to ConcurrentHashMap
echo       - ExecutorService.submit(worker) starts worker thread
echo    5. Each worker enters polling loop:
echo       - JobService.claimNextJob(workerId) called
echo       - MongoDB: findAndModify to atomically claim job
echo       - Job state changed: PENDING → PROCESSING
echo       - CommandExecutor.executeJob() runs the command
echo       - Job result processed and state updated
echo.
echo 🚨 IMPORTANT: Workers will start in a separate window!
echo    Watch that window to see real-time job processing.
echo.
echo Press any key to start workers...
pause >nul

echo [EXECUTING] Starting 2 workers in separate window...
start "🚀 QueueCTL Workers - Live Processing" cmd /k "echo ████████████████████████████████████████ && echo ██        WORKERS ARE PROCESSING       ██ && echo ████████████████████████████████████████ && echo. && echo Watch jobs change: PENDING → PROCESSING → COMPLETED/FAILED && echo. && java -jar target/queuectl-1.0.0.jar worker start --count 2"

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • 2 worker threads started in separate window
echo    • Each worker polling MongoDB for PENDING jobs
echo    • Workers will atomically claim and process jobs
echo    • Job states will change in real-time
echo.
echo 🔍 BEHIND THE SCENES (Worker Processing Loop):
echo    1. Worker.processNextJob() called continuously
echo    2. JobService.claimNextJob() uses MongoDB findAndModify:
echo       Query: {state: "PENDING", runAt: {$lte: now}}
echo       Update: {$set: {state: "PROCESSING", workerId: "worker-123", claimedAt: now}}
echo    3. CommandExecutor.executeJob() creates ProcessBuilder
echo    4. Process executed with timeout monitoring
echo    5. Exit code captured and job state updated accordingly
echo.
echo Press any key to monitor job processing...
pause >nul
cls

REM ========================================
REM STEP 8: Monitor Job Processing
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │               STEP 8: MONITOR JOB PROCESSING               │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Monitor jobs as they transition through states
echo.
echo 🔍 WHAT'S HAPPENING IN THE CODE RIGHT NOW:
echo.
echo 🔄 WORKER PROCESSING CYCLE:
echo    1. Worker calls JobService.claimNextJob(workerId)
echo    2. MongoDB atomic operation:
echo       db.jobs.findOneAndUpdate(
echo         {state: "PENDING", runAt: {$lte: new Date()}},
echo         {$set: {state: "PROCESSING", workerId: workerId, claimedAt: new Date()}},
echo         {sort: {priority: -1, createdAt: 1}}
echo       )
echo    3. If job claimed, CommandExecutor.executeJob() called
echo    4. ProcessBuilder creates new process for command
echo    5. Process output captured, exit code monitored
echo    6. Based on exit code:
echo       - 0: JobService.completeJob() → state: COMPLETED
echo       - Non-0: JobService.failJob() → retry logic or DLQ
echo.

for /L %%i in (1,1,5) do (
    echo ┌─ Monitoring Cycle %%i/5 ─ !time! ─────────────────────┐
    echo │
    echo │ 📊 Current Job States:
    java -jar target/queuectl-1.0.0.jar list --limit 10
    echo │
    echo │ 🖥️  System Status:
    java -jar target/queuectl-1.0.0.jar status
    echo │
    echo └────────────────────────────────────────────────────────────┘
    echo.
    
    if %%i LSS 5 (
        echo ⏱️  Next check in 4 seconds... (Watch the worker window!)
        timeout /t 4 /nobreak >nul
        echo.
    )
)

echo Press any key to see FAILED state and retry logic in action...
pause >nul
cls

REM ========================================
REM STEP 8A: FAILED State & Retry Logic Demonstration
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │        STEP 8A: FAILED STATE & RETRY LOGIC DEMO            │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Watch jobs fail and see the retry mechanism in action
echo.

echo 🔍 UNDERSTANDING THE RETRY FLOW:
echo.
echo When a job fails, here's what happens in the code:
echo.
echo 1️⃣  JOB FAILS (Exit code != 0):
echo    - Worker detects non-zero exit code
echo    - Worker calls: JobService.failJob(jobId, exitCode, errorMessage)
echo.
echo 2️⃣  FAILED STATE SET (Briefly):
echo    - Job state temporarily set to FAILED
echo    - Execution history updated with failure details
echo    - MongoDB: db.jobs.updateOne({_id: jobId}, {$set: {state: "FAILED"}})
echo.
echo 3️⃣  RETRY DECISION LOGIC:
echo    - Code checks: if (job.getAttempts() < job.getMaxRetries())
echo    - If TRUE: Schedule retry with exponential backoff
echo    - If FALSE: Move to Dead Letter Queue (DEAD state)
echo.
echo 4️⃣  EXPONENTIAL BACKOFF CALCULATION:
echo    - Delay = baseDelay * (2 ^ attempts)
echo    - Attempt 1: delay = 1s * 2^1 = 2 seconds
echo    - Attempt 2: delay = 1s * 2^2 = 4 seconds
echo    - Attempt 3: delay = 1s * 2^3 = 8 seconds
echo    - nextRetryTime = Instant.now() + delay
echo.
echo 5️⃣  JOB PREPARED FOR RETRY:
echo    - Job.prepareForRetry(baseDelay) called:
echo      • attempts++ (increment retry count)
echo      • state = PENDING (back to pending for retry)
echo      • runAt = nextRetryTime (scheduled for future)
echo      • workerId = null (cleared for next worker)
echo    - MongoDB: db.jobs.updateOne({_id: jobId}, {
echo        $set: {state: "PENDING", runAt: nextRetryTime},
echo        $inc: {attempts: 1}
echo      })
echo.
echo 6️⃣  RETRY EXECUTION:
echo    - When runAt time arrives, job becomes available again
echo    - Worker picks up job (PENDING state)
echo    - Process repeats until success or max retries
echo.

echo Let's check for FAILED jobs right now:
echo 💻 COMMAND: java -jar target/queuectl-1.0.0.jar list --state FAILED
echo.
java -jar target/queuectl-1.0.0.jar list --state FAILED
echo.

echo ⚠️  NOTE: FAILED state is very brief!
echo    Jobs move quickly from FAILED → PENDING (for retry)
echo    or FAILED → DEAD (if max retries exceeded)
echo.

echo Let's also check PENDING jobs to see retry scheduling:
echo 💻 COMMAND: java -jar target/queuectl-1.0.0.jar list --state PENDING
echo.
java -jar target/queuectl-1.0.0.jar list --state PENDING
echo.

echo 🔍 LOOK FOR:
echo    • Jobs with attempts > 0 (have been retried)
echo    • Jobs with runAt set to future time (scheduled retry)
echo    • This shows exponential backoff in action!
echo.

echo Press any key to continue monitoring retry process...
pause >nul

echo.
echo 🔄 MONITORING RETRY PROCESS (Watch jobs retry):
echo.

for /L %%i in (1,1,6) do (
    echo ┌─ Retry Monitoring Cycle %%i/6 ─ !time! ─────────────────┐
    echo │
    echo │ 📊 FAILED Jobs (if any):
    java -jar target/queuectl-1.0.0.jar list --state FAILED 2>nul | findstr /C:"│" || echo    (No jobs currently in FAILED state)
    echo │
    echo │ ⏳ PENDING Jobs (including retries):
    java -jar target/queuectl-1.0.0.jar list --state PENDING --limit 5
    echo │
    echo │ ✅ COMPLETED Jobs (successful retries):
    java -jar target/queuectl-1.0.0.jar list --state COMPLETED --limit 3
    echo │
    echo └────────────────────────────────────────────────────────────┘
    echo.
    
    if %%i LSS 6 (
        echo ⏱️  Next check in 5 seconds... (Watch retry mechanism!)
        timeout /t 5 /nobreak >nul
        echo.
    )
)

echo.
echo ✅ WHAT YOU JUST SAW:
echo    • Jobs transitioning: PROCESSING → FAILED → PENDING (retry)
echo    • Exponential backoff delays (runAt field set to future)
echo    • Retry attempts incrementing
echo    • Successful retries completing
echo    • Failed retries moving to DLQ after max attempts
echo.

echo Press any key to check final results...
pause >nul
cls

REM ========================================
REM STEP 9: Final Results Analysis
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │              STEP 9: FINAL RESULTS ANALYSIS                │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Examine final job states after processing
echo.

echo 🎯 COMPLETED JOBS:
echo 💻 COMMAND: java -jar target/queuectl-1.0.0.jar list --state COMPLETED
echo 🔍 CODE: MongoDB query: db.jobs.find({state: "COMPLETED"})
echo.
java -jar target/queuectl-1.0.0.jar list --state COMPLETED
echo.
echo Press any key to check failed jobs...
pause >nul

echo ⚠️  FAILED Jobs (Temporary failures, can retry):
echo 💻 COMMAND: java -jar target/queuectl-1.0.0.jar list --state FAILED
echo 🔍 CODE: MongoDB query: db.jobs.find({state: "FAILED"})
echo    NOTE: FAILED state is brief - jobs quickly move to PENDING (retry) or DEAD (DLQ)
echo.
java -jar target/queuectl-1.0.0.jar list --state FAILED
echo.

echo 🔍 TROUBLESHOOTING: If jobs show as DEAD unexpectedly:
echo    • Check job errorMessage: Shows why job failed
echo    • Check exitCode: Non-zero means command failed
echo    • Check attempts: If attempts >= maxRetries, job moved to DLQ
echo    • Common causes:
echo      - Command syntax errors (e.g., redirection issues with '>nul')
echo      - Unicode/emoji encoding issues (Windows cmd.exe)
echo      - Command timeout (default: 30 minutes)
echo      - Security validation failure
echo      - Input/output redirection not supported in ProcessBuilder context
echo.

echo Press any key to check Dead Letter Queue...
pause >nul

echo 💀 DEAD LETTER QUEUE (Permanent Failures):
echo 💻 COMMAND: java -jar target/queuectl-1.0.0.jar dlq list
echo 🔍 CODE: MongoDB query: db.dlq.find({}) (separate collection for failed jobs)
echo.
java -jar target/queuectl-1.0.0.jar dlq list
echo.
echo Press any key to see final system status...
pause >nul

echo 📈 FINAL SYSTEM STATUS:
echo 💻 COMMAND: java -jar target/queuectl-1.0.0.jar status
echo 🔍 CODE: Aggregated statistics from all MongoDB collections
echo.
java -jar target/queuectl-1.0.0.jar status
echo.

REM ========================================
REM STEP 9A: Stop Workers Gracefully
REM ========================================
echo Press any key to demonstrate worker shutdown...
pause >nul

echo ┌─────────────────────────────────────────────────────────────┐
echo │               STEP 9A: STOP WORKERS GRACEFULLY             │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Demonstrate graceful worker shutdown
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar worker stop
echo.
echo 🔍 CODE EXPLANATION:
echo    1. WorkerCommand.StopCommand.call() invoked
echo    2. WorkerService.stopWorkersGracefully(30) called
echo    3. For each worker:
echo       - Worker.shutdown() sets shutdown flag
echo       - Current job allowed to complete
echo       - Worker thread interrupted gracefully
echo    4. ExecutorService.shutdown() called
echo    5. ExecutorService.awaitTermination(30, SECONDS)
echo    6. If timeout exceeded, force shutdown with shutdownNow()
echo    7. Workers removed from ConcurrentHashMap
echo.
echo Press any key to execute this command...
pause >nul

echo [EXECUTING] Stopping workers gracefully...
java -jar target/queuectl-1.0.0.jar worker stop

echo.
echo ✅ WHAT JUST HAPPENED:
echo    • Workers received shutdown signal
echo    • Current jobs completed before stopping
echo    • ExecutorService terminated gracefully
echo    • All worker threads cleaned up
echo.

REM ========================================
REM STEP 9B: DLQ Retry Demonstration
REM ========================================
echo Press any key to demonstrate DLQ retry functionality...
pause >nul

echo ┌─────────────────────────────────────────────────────────────┐
echo │              STEP 9B: DLQ RETRY DEMONSTRATION              │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 📋 WHAT WE'RE DOING:
echo    Demonstrate retrying a job from Dead Letter Queue
echo.

echo 💻 FIRST, let's see what jobs are in DLQ:
echo    java -jar target/queuectl-1.0.0.jar dlq list
echo.
echo 🔍 CODE EXPLANATION:
echo    1. DLQCommand.ListCommand.call() invoked
echo    2. DLQService.getDeadLetterJobs() called
echo    3. MongoDB query: db.dlq.find({}).sort({movedToDlqAt: -1})
echo    4. Results formatted and displayed with failure reasons
echo.
echo Press any key to list DLQ jobs...
pause >nul

echo [EXECUTING] Listing Dead Letter Queue jobs...
java -jar target/queuectl-1.0.0.jar dlq list

echo.
echo Now let's retry the first job from DLQ (if any exists)...
echo.
echo 💻 COMMAND TO EXECUTE:
echo    java -jar target/queuectl-1.0.0.jar dlq retry [job-id]
echo.
echo 🔍 CODE EXPLANATION:
echo    1. DLQCommand.RetryCommand.call() invoked
echo    2. DLQService.retryJob(jobId) called
echo    3. MongoDB operations:
echo       - Find job in DLQ: db.dlq.findOne({_id: jobId})
echo       - Reset job state: state = PENDING, attempts = 0
echo       - Move back to main queue: db.jobs.insertOne(resetJob)
echo       - Remove from DLQ: db.dlq.deleteOne({_id: jobId})
echo    4. Job becomes available for workers again
echo.

REM Get the first job ID from DLQ for retry demonstration
echo [GETTING] First job ID from DLQ for retry demonstration...
for /f "tokens=2 delims=|" %%i in ('java -jar target/queuectl-1.0.0.jar dlq list --format csv 2^>nul ^| findstr /v "ID" ^| head -1') do (
    set "FIRST_JOB_ID=%%i"
)

if defined FIRST_JOB_ID (
    echo Found job to retry: !FIRST_JOB_ID!
    echo.
    echo Press any key to retry this job...
    pause >nul
    
    echo [EXECUTING] Retrying job from DLQ: !FIRST_JOB_ID!
    java -jar target/queuectl-1.0.0.jar dlq retry !FIRST_JOB_ID!
    
    echo.
    echo ✅ WHAT JUST HAPPENED:
    echo    • Job moved from DLQ back to main queue
    echo    • Job state reset to PENDING
    echo    • Attempt count reset to 0
    echo    • Job ready for workers to process again
    echo.
    
    echo Let's verify the job is back in the main queue:
    java -jar target/queuectl-1.0.0.jar list --state PENDING
    
) else (
    echo No jobs found in DLQ to retry.
    echo This means all jobs either completed successfully or are still processing.
    echo.
    echo ✅ WHAT THIS MEANS:
    echo    • Your retry logic worked perfectly
    echo    • Jobs that could be recovered were recovered
    echo    • Only truly unrecoverable jobs remain in DLQ
)

echo.

REM ========================================
REM STEP 10: Code Architecture Summary
REM ========================================
echo.
echo ┌─────────────────────────────────────────────────────────────┐
echo │            STEP 10: CODE ARCHITECTURE SUMMARY              │
echo └─────────────────────────────────────────────────────────────┘
echo.

echo 🏗️ WHAT YOU JUST WITNESSED AT THE CODE LEVEL:
echo.
echo 📦 KEY COMPONENTS THAT EXECUTED:
echo    1. CLI Layer (Picocli):
echo       - EnqueueCommand, WorkerCommand, StatusCommand, ListCommand, DLQCommand
echo       - Parameter parsing and validation
echo.
echo    2. Service Layer (Spring):
echo       - JobService: Job lifecycle management
echo       - WorkerService: Worker pool management  
echo       - CommandExecutor: Process execution
echo       - ConfigService: Runtime configuration
echo.
echo    3. Repository Layer (Spring Data MongoDB):
echo       - JobRepository: CRUD operations with atomic updates
echo       - DLQRepository: Dead letter queue management
echo       - MongoDB Change Streams for real-time updates
echo.
echo    4. Domain Layer:
echo       - Job entity with state transitions
echo       - Worker threads with polling mechanism
echo       - Retry logic with exponential backoff
echo.
echo 🔄 STATE TRANSITION FLOW YOU SAW:
echo    PENDING → PROCESSING → COMPLETED/FAILED → DEAD
echo.
echo 🛡️ CONCURRENCY SAFETY MECHANISMS:
echo    • MongoDB findAndModify for atomic job claiming
echo    • ConcurrentHashMap for worker management
echo    • Optimistic locking for job updates
echo    • Graceful shutdown with job completion
echo.
echo 📊 DATABASE OPERATIONS EXECUTED:
echo    • insertOne(): Job creation
echo    • findOneAndUpdate(): Atomic job claiming
echo    • updateOne(): State transitions
echo    • find(): Job listing and filtering
echo    • countDocuments(): Statistics gathering
echo.

echo [CLEANUP] Stopping workers...
taskkill /f /im java.exe >nul 2>&1
del retry_marker.tmp >nul 2>&1

echo.
echo ████████████████████████████████████████████████████████████████
echo ██                                                            ██
echo ██              STEP-BY-STEP DEMO COMPLETED                  ██
echo ██           You now understand the internals!               ██
echo ██                                                            ██
echo ████████████████████████████████████████████████████████████████
echo.
echo 🎓 CONGRATULATIONS! You've seen:
echo    • Complete job lifecycle with code explanations
echo    • MongoDB operations and atomic updates
echo    • Worker concurrency and job claiming
echo    • Retry mechanisms and DLQ functionality
echo    • Enterprise-grade error handling
echo.
echo Press any key to exit...
pause >nul
