#!/bin/bash
# Test script for LLP Java Algorithms project

echo "Running tests..."
mvn test

if [ $? -eq 0 ]; then
    echo "All tests passed!"
else
    echo "Some tests failed!"
    exit 1
fi
