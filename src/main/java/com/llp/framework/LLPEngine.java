package com.llp.framework;

import com.llp.algorithm.LLPProblem;
import java.util.concurrent.*;

/**
 * Minimal execution engine for parallel LLP algorithms.
 * 
 * Uses the unified LLP interface where all operations accept threadId and totalThreads
 * parameters, enabling seamless single-threaded and multi-threaded execution.
 */
public class LLPEngine<T> {
    
    private final LLPProblem<T> problem;
    private final int parallelism;
    private final int maxIterations;
    private ExecutorService executor;
    
    // Simple termination tracking
    private int iterationCount = 0;
    private boolean converged = false;
    
    public LLPEngine(LLPProblem<T> problem, int parallelism, int maxIterations) {
        this.problem = problem;
        this.parallelism = parallelism;
        this.maxIterations = maxIterations;
        this.executor = parallelism > 1 ? Executors.newFixedThreadPool(parallelism) : null;
    }
    
    /**
     * Execute the LLP algorithm: only advance forbidden states.
     */
    public T execute(T initialState) {
        T currentState = initialState;
        
        for (iterationCount = 0; iterationCount < maxIterations; iterationCount++) {
            // Only advance if the state is forbidden
            if (!problem.Forbidden(currentState)) {
                converged = true;
                return currentState;
            }
            
            // Apply Advance to fix the forbidden state
            currentState = advanceInParallel(currentState);
        }
        
        return currentState;
    }
    
    /**
     * Coordinate TRUE parallel execution.
     */
    private T advanceInParallel(T initialState) {
        if (parallelism == 1) {
            return problem.Advance(initialState, 0, 1);
        }
        
        // ALL threads work on the SAME state object
        CompletableFuture<Void>[] futures = new CompletableFuture[parallelism];
        
        for (int threadId = 0; threadId < parallelism; threadId++) {
            final int id = threadId;
            futures[id] = CompletableFuture.runAsync(() -> {
                // Each thread modifies the SAME state object
                problem.Advance(initialState, id, parallelism);
            }, executor);
        }
        
        // Wait for all threads to complete
        try {
            CompletableFuture.allOf(futures).get();
            return initialState; // Return the SAME object (now modified)
            
        } catch (Exception e) {
            throw new RuntimeException("Error in parallel execution", e);
        }
    }
    
    // Getters for statistics
    public int getIterationCount() { return iterationCount; }
    public boolean hasConverged() { return converged; }
    public int getNumThreads() { return parallelism; }
    
    public void shutdown() { 
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
