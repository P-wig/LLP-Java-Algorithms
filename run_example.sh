#!/bin/bash
# Run script for the simple LLP example

echo "Running Simple LLP Example..."
mvn compile exec:java -Dexec.mainClass="com.llp.examples.SimpleLLPExample"
