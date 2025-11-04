#!/bin/bash
# Enhanced Build script for LLP Java Algorithms project

echo "========================================"
echo "    LLP Java Algorithms Build Script    "
echo "========================================"
echo

echo "Cleaning previous build artifacts..."
mvn clean

echo
echo "Compiling all source files..."
mvn compile

if [ $? -eq 0 ]; then
    echo
    echo "BUILD SUCCESSFUL!"
    echo "All Java source files compiled successfully."
    echo
    echo "Available Problems:"
    echo "  - BellmanFordProblem"
    echo "  - BoruvkaProblem" 
    echo "  - ConnectedComponentsProblem"
    echo "  - JohnsonProblem"
    echo "  - ParallelPrefixProblem"
    echo "  - StableMarriageProblem"
    echo
    echo "Use './test.sh' to run all algorithms with demonstrations"
    echo "========================================"
else
    echo
    echo "BUILD FAILED!"
    echo "Please check the compilation errors above."
    echo "========================================"
    exit 1
fi