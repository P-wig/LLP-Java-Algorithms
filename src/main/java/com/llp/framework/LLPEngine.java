package com.llp.framework;

import com.llp.algorithm.LLPProblem;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal streams-based execution engine for parallel LLP algorithms.
 */
public class LLPEngine<T> {
    
    private final LLPProblem<T> problem;
    private final int parallelism;
    private final int maxIterations;
    
    // Simple termination tracking
    private int iterationCount = 0;
    private boolean converged = false;
    
    public LLPEngine(LLPProblem<T> problem, int parallelism, int maxIterations) {
        this.problem = problem;
        this.parallelism = parallelism;
        this.maxIterations = maxIterations;
        
        // Set parallelism for streams
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
                          String.valueOf(parallelism));
    }
    
    /**
     * Execute using streams-based parallel approach.
     */
    public T execute(T initialState) {
        AtomicReference<T> currentState = new AtomicReference<>(initialState);
        T previousState = initialState;
        
        System.out.println("Starting LLP execution with " + parallelism + " threads...");
        
        for (iterationCount = 0; iterationCount < maxIterations; iterationCount++) {
            
            T current = currentState.get();  // Get current state
            
            // Check if current state is forbidden
            if (problem.Forbidden(current)) {
                // Fix violations with parallel Ensure
                T afterEnsure = IntStream.range(0, parallelism)
                    .parallel()
                    .mapToObj(threadId -> problem.EnsureWithContext(current, threadId, parallelism))
                    .reduce(current, problem::merge);  // USE PROBLEM'S MERGE METHOD
                currentState.set(afterEnsure);
                
                System.out.println("  Iteration " + iterationCount + ": Fixed forbidden state");
                
            } else {
                // Make progress with parallel Advance
                T afterAdvance = IntStream.range(0, parallelism)
                    .parallel()
                    .mapToObj(threadId -> {
                        // Each thread works on different part
                        return problem.AdvanceWithContext(current, threadId, parallelism);
                    })
                    .reduce(current, problem::merge);  // USE PROBLEM'S MERGE METHOD
                currentState.set(afterAdvance);
                
                // Check if Advance created violations and fix them
                T advanced = currentState.get();
                if (problem.Forbidden(advanced)) {
                    T afterEnsure = IntStream.range(0, parallelism)
                        .parallel()
                        .mapToObj(i -> problem.Ensure(advanced))
                        .reduce(advanced, problem::merge);  // USE PROBLEM'S MERGE METHOD
                    currentState.set(afterEnsure);
                    
                    System.out.println("  Iteration " + iterationCount + ": Advanced then fixed violation");
                } else {
                    System.out.println("  Iteration " + iterationCount + ": Advanced without violation");
                }
            }
            
            // Check for solution - get fresh state reference
            T finalState = currentState.get();
            if (problem.isSolution(finalState)) {
                System.out.println("  ✓ Solution found at iteration " + iterationCount);
                converged = true;
                break;
            }
            
            // Check for convergence (no progress)
            if (finalState.equals(previousState)) {
                System.out.println("  ✓ Converged (no change) at iteration " + iterationCount);
                converged = true;
                break;
            }
            
            previousState = finalState;  // Update for next iteration
        }
        
        if (!converged) {
            System.out.println("  ! Reached max iterations: " + maxIterations);
        }
        
        return currentState.get();
    }
    
    // Getters for statistics
    public int getIterationCount() { return iterationCount; }
    public boolean hasConverged() { return converged; }
    public int getNumThreads() { return parallelism; }
    public void shutdown() { /* No cleanup needed for streams */ }
}
