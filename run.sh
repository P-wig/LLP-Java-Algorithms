#!/bin/bash

# Build and run script for LLP Algorithms

echo "Building LLP Algorithms project..."
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful! Running the program..."
    echo "========================================"
    java -jar target/llp-algorithms-1.0-SNAPSHOT-jar-with-dependencies.jar
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi
