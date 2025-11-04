#!/bin/bash
# Enhanced Test script for LLP Java Algorithms project

echo "========================================"
echo "   LLP Java Algorithms Test Suite      "
echo "========================================"
echo

# Function to create visual separator
print_separator() {
    echo
    echo "=========================================="
    echo "           $1"
    echo "=========================================="
    echo
}

# Function to run a specific problem
run_problem() {
    local class_name=$1
    local problem_name=$2
    
    print_separator "$problem_name"
    
    echo "Running: $class_name"
    echo "Timestamp: $(date)"
    echo
    
    # Run the Java class and capture exit code
    mvn exec:java -Dexec.mainClass="com.llp.problems.$class_name" -q
    local exit_code=$?
    
    echo
    if [ $exit_code -eq 0 ]; then
        echo "SUCCESS: $problem_name completed successfully"
    else
        echo "FAILED: $problem_name failed with exit code $exit_code"
    fi
    
    echo
    echo "End of $problem_name"
    echo "==========================================+"
    echo
    echo
    echo
}

# Ensure project is built
echo "Ensuring project is compiled..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "COMPILATION FAILED! Please run ./build.sh first."
    exit 1
fi

echo "Project compiled successfully"
echo

# Introduction
print_separator "ALGORITHM DEMONSTRATIONS"
echo "This test suite demonstrates all implemented LLP algorithms."
echo "Each algorithm will run with example data and show results."
echo "Please wait for each algorithm to complete before the next begins."
echo
echo "Algorithms to be demonstrated:"
echo "  1. Bellman-Ford Algorithm (Single-source shortest paths)"
echo "  2. Boruvka's Algorithm (Minimum Spanning Tree)"
echo "  3. Connected Components (Graph connectivity)"
echo "  4. Johnson's Algorithm (All-pairs shortest paths)"
echo "  5. Parallel Prefix (Array prefix computation)"
echo "  6. Stable Marriage (Matching algorithm)"
echo
echo "Starting in 3 seconds..."
sleep 3

# Run each problem with clear separation
run_problem "BellmanFordProblem" "1. BELLMAN-FORD ALGORITHM"
sleep 2

run_problem "BoruvkaProblem" "2. BORUVKA'S ALGORITHM"
sleep 2

run_problem "ConnectedComponentsProblem" "3. CONNECTED COMPONENTS"
sleep 2

run_problem "JohnsonProblem" "4. JOHNSON'S ALGORITHM"
sleep 2

run_problem "ParallelPrefixProblem" "5. PARALLEL PREFIX"
sleep 2

run_problem "StableMarriageProblem" "6. STABLE MARRIAGE"

# Final summary
print_separator "TEST SUITE COMPLETE"
echo "All LLP algorithm demonstrations have finished."
echo "Each algorithm was executed with sample data to show:"
echo "  - Algorithm correctness"
echo "  - Parallel execution capability"
echo "  - Performance metrics"
echo "  - Result validation"
echo
echo "For detailed analysis of any specific algorithm,"
echo "you can run individual classes manually:"
echo
echo "Example:"
echo "  mvn exec:java -Dexec.mainClass='com.llp.problems.BoruvkaProblem'"
echo
echo "Timestamp: $(date)"
echo "========================================"
