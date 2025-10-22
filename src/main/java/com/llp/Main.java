package com.llp;

import com.llp.core.LLPAlgorithm;
import com.llp.core.LLPResult;
import com.llp.core.Problem;
import com.llp.problems.ExampleProblem;

/**
 * Main entry point for the LLP Algorithms application.
 * Demonstrates how to use the LLP algorithm library.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== LLP Algorithm Library Demo ===\n");
        
        // Example 1: Simple number optimization problem
        System.out.println("Example 1: Number Optimization Problem");
        System.out.println("Goal: Find a number close to target value 1000 starting from 0\n");
        
        Problem<Integer> problem = new ExampleProblem(1000, 0);
        
        // Configure algorithm parameters
        int numberOfThreads = 4;  // Number of parallel threads
        int maxIterations = 100;   // Maximum iterations per thread
        
        LLPAlgorithm<Integer> algorithm = new LLPAlgorithm<>(
            problem, 
            numberOfThreads, 
            maxIterations
        );
        
        try {
            long startTime = System.currentTimeMillis();
            LLPResult<Integer> result = algorithm.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("Solution found!");
            System.out.println(problem.formatSolution(result.getSolution()));
            System.out.println(result);
            System.out.println("Time taken: " + (endTime - startTime) + " ms");
            System.out.println("Is goal reached: " + problem.isGoal(result.getSolution()));
            
        } catch (Exception e) {
            System.err.println("Error solving problem: " + e.getMessage());
            e.printStackTrace();
        } finally {
            algorithm.shutdown();
        }
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("\nTo implement your own problem:");
        System.out.println("1. Implement the Problem<T> interface");
        System.out.println("2. Define getInitialState(), isGoal(), getSuccessors(), evaluate()");
        System.out.println("3. Create an LLPAlgorithm instance with your problem");
        System.out.println("4. Call solve() to get the result");
    }
}
