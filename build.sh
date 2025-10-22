#!/bin/bash
# Build script for LLP Java Algorithms project

echo "Building LLP Java Algorithms..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
    exit 1
fi
