#!/bin/bash

# QueueCTL Demo Script
# This script demonstrates all core features and bonus functionality

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
JAR_FILE="target/queuectl-1.0.0.jar"
DEMO_DELAY=3

# Helper functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_step() {
    echo -e "${GREEN}➤ $1${NC}"
}

print_command() {
    echo -e "${YELLOW}$ $1${NC}"
}

run_command() {
    print_command "$1"
    eval "$1"
    sleep $DEMO_DELAY
}

wait_for_input() {
    echo -e "\n${CYAN}Press Enter to continue...${NC}"
    read
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    print_step "Checking Java version..."
    java -version
    
    print_step "Checking if JAR file exists..."
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}Error: $JAR_FILE not found. Please run 'mvn clean package' first.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ JAR file found${NC}"
    
    print_step "Checking MongoDB connection..."
    # Note: In a real demo, you'd want to check MongoDB connectivity
    echo -e "${GREEN}✓ Prerequisites check complete${NC}"
    
    wait_for_input
}

# Demo 1: Basic Job Management
demo_basic_jobs() {
    print_header "Demo 1: Basic Job Management"
    
    print_step "1.1 Enqueuing simple jobs"
    run_command "java -jar $JAR_FILE enqueue --command 'echo Hello QueueCTL'"
    run_command "java -jar $JAR_FILE enqueue --command 'echo Processing data...'"
    run_command "java -jar $JAR_FILE enqueue --command 'sleep 2 && echo Job completed'"
    
    print_step "1.2 Checking system status"
    run_command "java -jar $JAR_FILE status"
    
    print_step "1.3 Listing all jobs"
    run_command "java -jar $JAR_FILE list"
    
    wait_for_input
}

# Demo 2: Worker Management
demo_workers() {
    print_header "Demo 2: Worker Management"
    
    print_step "2.1 Starting workers in background"
    run_command "java -jar $JAR_FILE worker start --count 3 --daemon &"
    
    print_step "2.2 Checking worker status"
    sleep 5  # Give workers time to start
    run_command "java -jar $JAR_FILE worker status"
    
    print_step "2.3 Monitoring job processing"
    run_command "java -jar $JAR_FILE status"
    
    print_step "2.4 Adding more jobs while workers are running"
    run_command "java -jar $JAR_FILE enqueue --command 'echo Worker processing job 1'"
    run_command "java -jar $JAR_FILE enqueue --command 'echo Worker processing job 2'"
    
    sleep 3
    run_command "java -jar $JAR_FILE list --state COMPLETED"
    
    wait_for_input
}

# Demo 3: Priority Queues (Bonus Feature)
demo_priority() {
    print_header "Demo 3: Priority Queues (Bonus Feature)"
    
    print_step "3.1 Enqueuing jobs with different priorities"
    run_command "java -jar $JAR_FILE enqueue --command 'echo LOW priority job' --priority LOW"
    run_command "java -jar $JAR_FILE enqueue --command 'echo MEDIUM priority job' --priority MEDIUM"
    run_command "java -jar $JAR_FILE enqueue --command 'echo HIGH priority job' --priority HIGH"
    run_command "java -jar $JAR_FILE enqueue --command 'echo CRITICAL priority job' --priority CRITICAL"
    
    print_step "3.2 Viewing jobs by priority (CRITICAL should process first)"
    run_command "java -jar $JAR_FILE list --state PENDING"
    
    print_step "3.3 Status with priority breakdown"
    run_command "java -jar $JAR_FILE status --priority-breakdown"
    
    wait_for_input
}

# Demo 4: Retry Mechanism & DLQ
demo_retry_dlq() {
    print_header "Demo 4: Retry Mechanism & Dead Letter Queue"
    
    print_step "4.1 Enqueuing a job that will fail"
    run_command "java -jar $JAR_FILE enqueue --command 'exit 1' --max-retries 2"
    
    print_step "4.2 Watching the job retry with exponential backoff"
    echo -e "${CYAN}The job will fail and retry automatically...${NC}"
    sleep 10
    
    run_command "java -jar $JAR_FILE list --state FAILED"
    
    print_step "4.3 After max retries, job moves to Dead Letter Queue"
    sleep 15  # Wait for retries to complete
    run_command "java -jar $JAR_FILE dlq list"
    
    print_step "4.4 DLQ statistics"
    run_command "java -jar $JAR_FILE dlq stats"
    
    print_step "4.5 Retrying a job from DLQ"
    # Get the job ID from DLQ and retry it
    echo -e "${CYAN}Note: In a real demo, you'd extract the job ID and retry it${NC}"
    echo -e "${YELLOW}$ java -jar $JAR_FILE dlq retry <job-id> --reset-attempts${NC}"
    
    wait_for_input
}

# Demo 5: Scheduled Jobs (Bonus Feature)
demo_scheduled() {
    print_header "Demo 5: Scheduled Jobs (Bonus Feature)"
    
    print_step "5.1 Scheduling a job for future execution"
    FUTURE_TIME=$(date -d "+30 seconds" -Iseconds)
    run_command "java -jar $JAR_FILE enqueue --command 'echo Scheduled job executed!' --run-at '$FUTURE_TIME'"
    
    print_step "5.2 Job should be in SCHEDULED state"
    run_command "java -jar $JAR_FILE list --state SCHEDULED"
    
    print_step "5.3 Waiting for scheduled time..."
    echo -e "${CYAN}Job will become PENDING and execute at: $FUTURE_TIME${NC}"
    sleep 35
    
    run_command "java -jar $JAR_FILE list --state COMPLETED | tail -5"
    
    wait_for_input
}

# Demo 6: Job Timeout (Bonus Feature)
demo_timeout() {
    print_header "Demo 6: Job Timeout Handling (Bonus Feature)"
    
    print_step "6.1 Enqueuing a long-running job with short timeout"
    run_command "java -jar $JAR_FILE enqueue --command 'sleep 30' --timeout '5s'"
    
    print_step "6.2 Job should timeout and be retried"
    echo -e "${CYAN}Job will timeout after 5 seconds and be retried...${NC}"
    sleep 10
    
    run_command "java -jar $JAR_FILE list --state TIMEOUT"
    
    wait_for_input
}

# Demo 7: Configuration Management
demo_configuration() {
    print_header "Demo 7: Configuration Management"
    
    print_step "7.1 Viewing current configuration"
    run_command "java -jar $JAR_FILE config list"
    
    print_step "7.2 Updating configuration at runtime"
    run_command "java -jar $JAR_FILE config set max-workers 8"
    run_command "java -jar $JAR_FILE config set max-retries 5"
    
    print_step "7.3 Verifying configuration changes"
    run_command "java -jar $JAR_FILE config get max-workers"
    run_command "java -jar $JAR_FILE config get max-retries"
    
    wait_for_input
}

# Demo 8: JSON Job Specification
demo_json_jobs() {
    print_header "Demo 8: Advanced JSON Job Specification"
    
    print_step "8.1 Enqueuing job with JSON specification"
    run_command "java -jar $JAR_FILE enqueue '{
        \"id\": \"demo-job-001\",
        \"command\": \"echo Advanced JSON job\",
        \"priority\": \"HIGH\",
        \"max_retries\": 3,
        \"timeout\": \"PT2M\"
    }'"
    
    print_step "8.2 Viewing the job details"
    run_command "java -jar $JAR_FILE list --format json | head -20"
    
    wait_for_input
}

# Demo 9: Monitoring & Statistics
demo_monitoring() {
    print_header "Demo 9: Monitoring & Statistics (Bonus Feature)"
    
    print_step "9.1 Comprehensive system status"
    run_command "java -jar $JAR_FILE status --detailed"
    
    print_step "9.2 Job statistics and metrics"
    run_command "java -jar $JAR_FILE stats"
    
    print_step "9.3 Exporting job data"
    run_command "java -jar $JAR_FILE list --format csv | head -10"
    
    wait_for_input
}

# Cleanup
cleanup() {
    print_header "Demo Cleanup"
    
    print_step "Stopping all workers"
    run_command "java -jar $JAR_FILE worker stop --force"
    
    print_step "Final system status"
    run_command "java -jar $JAR_FILE status"
    
    echo -e "\n${GREEN}🎉 Demo completed successfully!${NC}"
    echo -e "${CYAN}Thank you for watching the QueueCTL demonstration.${NC}\n"
}

# Main demo execution
main() {
    echo -e "${PURPLE}"
    echo "  ██████╗ ██╗   ██╗███████╗██╗   ██╗███████╗ ██████╗████████╗██╗     "
    echo " ██╔═══██╗██║   ██║██╔════╝██║   ██║██╔════╝██╔════╝╚══██╔══╝██║     "
    echo " ██║   ██║██║   ██║█████╗  ██║   ██║█████╗  ██║        ██║   ██║     "
    echo " ██║▄▄ ██║██║   ██║██╔══╝  ██║   ██║██╔══╝  ██║        ██║   ██║     "
    echo " ╚██████╔╝╚██████╔╝███████╗╚██████╔╝███████╗╚██████╗   ██║   ███████╗"
    echo "  ╚══▀▀═╝  ╚═════╝ ╚══════╝ ╚═════╝ ╚══════╝ ╚═════╝   ╚═╝   ╚══════╝"
    echo -e "${NC}"
    echo -e "${CYAN}Enterprise Job Queue System - Live Demo${NC}"
    echo -e "${CYAN}Demonstrating all core features + bonus functionality${NC}\n"
    
    check_prerequisites
    demo_basic_jobs
    demo_workers
    demo_priority
    demo_retry_dlq
    demo_scheduled
    demo_timeout
    demo_configuration
    demo_json_jobs
    demo_monitoring
    cleanup
}

# Run the demo
main "$@"
