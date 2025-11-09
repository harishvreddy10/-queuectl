@echo off
echo ========================================
echo QueueCTL Comprehensive Fix Verification
echo ========================================

echo.
echo 🧹 Step 1: Clean up any existing jobs
echo ----------------------------------------
echo Clearing MongoDB jobs collection...
mongosh --quiet --eval "db.jobs.deleteMany({})" queuectl

echo.
echo 📊 Step 2: Verify clean state
echo ----------------------------------------
java -jar target/queuectl-1.0.0.jar status

echo.
echo 🚀 Step 3: Start workers in daemon mode
echo ----------------------------------------
echo Starting 2 workers in daemon mode...
start /B java -jar target/queuectl-1.0.0.jar worker start --count 2 --daemon

echo Waiting 3 seconds for workers to initialize...
timeout /t 3 /nobreak >nul

echo.
echo 📝 Step 4: Enqueue test jobs
echo ----------------------------------------
echo Enqueueing success job...
java -jar target/queuectl-1.0.0.jar enqueue --command "echo SUCCESS_JOB_COMPLETED"

echo Enqueueing failure job...
java -jar target/queuectl-1.0.0.jar enqueue --command "exit 1" --max-retries 1

echo Enqueueing priority job...
java -jar target/queuectl-1.0.0.jar enqueue --command "echo HIGH_PRIORITY_JOB" --priority HIGH

echo.
echo ⏱️ Step 5: Wait for processing
echo ----------------------------------------
echo Waiting 10 seconds for workers to process jobs...
timeout /t 10 /nobreak >nul

echo.
echo 📋 Step 6: Check results
echo ----------------------------------------
echo Current job status:
java -jar target/queuectl-1.0.0.jar status

echo.
echo Recent jobs:
java -jar target/queuectl-1.0.0.jar list --limit 5

echo.
echo 🔍 Step 7: Verify job state transitions
echo ----------------------------------------
echo Checking MongoDB for job states...
mongosh --quiet --eval "db.jobs.find({}, {_id:1, command:1, state:1, attempts:1}).sort({createdAt:-1}).limit(5)" queuectl

echo.
echo 🛑 Step 8: Cleanup
echo ----------------------------------------
echo Stopping workers...
taskkill /F /IM java.exe >nul 2>&1

echo.
echo ========================================
echo Test Results Summary:
echo ========================================
echo ✅ SUCCESS: If you see COMPLETED or FAILED states above
echo ❌ FAILURE: If all jobs are still PENDING
echo.
echo Expected outcomes:
echo - SUCCESS_JOB_COMPLETED should be COMPLETED
echo - exit 1 should be FAILED (after retry)
echo - HIGH_PRIORITY_JOB should be COMPLETED
echo ========================================
pause
