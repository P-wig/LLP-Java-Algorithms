package com.llp.algorithm;

import com.llp.framework.LLPEngine;
import java.util.concurrent.*;

/**
 * Simplified LLPSolver without configuration complexity.
 * Provides a clean API for solving LLP problems with sensible defaults.
 * 
 * @param <T> The type representing the state or configuration in the lattice
 */
public class LLPSolver<T> {
    
    private final LLPProblem<T> problem;
    private final int numThreads;
    private final int maxIterations;
    private LLPEngine<T> engine;
    
    /**
     * Creates a new LLPSolver with default settings.
     * Uses available processor count for threads and 1000 max iterations.
     * 
     * @param problem The problem to solve
     */
    public LLPSolver(LLPProblem<T> problem) {
        this(problem, Runtime.getRuntime().availableProcessors(), 1000);
    }
    
    /**
     * Creates a new LLPSolver with specified thread count.
     * Uses 1000 max iterations.
     * 
     * @param problem The problem to solve
     * @param numThreads The number of parallel threads to use
     */
    public LLPSolver(LLPProblem<T> problem, int numThreads) {
        this(problem, numThreads, 1000);
    }
    
    /**
     * Creates a new LLPSolver with specified thread count and max iterations.
     * 
     * @param problem The problem to solve
     * @param numThreads The number of parallel threads to use
     * @param maxIterations The maximum number of iterations
     */
    public LLPSolver(LLPProblem<T> problem, int numThreads, int maxIterations) {
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads must be at least 1");
        }
        if (maxIterations < 1) {
            throw new IllegalArgumentException("Max iterations must be at least 1");
        }
        
        this.problem = problem;
        this.numThreads = numThreads;
        this.maxIterations = maxIterations;
    }
    
    /**
     * Solves the problem using the parallel LLP algorithm framework.
     * 
     * @return The solution state
     * @throws InterruptedException if the solving process is interrupted
     * @throws ExecutionException if an error occurs during execution
     */
    public T solve() throws InterruptedException, ExecutionException {
        // Get initial state from problem
        T initialState = problem.getInitialState();
        
        // Create and configure the engine
        engine = new LLPEngine<>(problem, numThreads, maxIterations);
        
        // Execute the parallel LLP algorithm
        T solution = engine.execute(initialState);
        
        return solution;
    }
    
    /**
     * Gets execution statistics after solving.
     * 
     * @return A simple object with iteration count and convergence status
     */
    public ExecutionStats getExecutionStats() {
        if (engine == null) return null;
        return new ExecutionStats(engine.getIterationCount(), engine.hasConverged());
    }

    // Simple stats class
    public static class ExecutionStats {
        public final int iterationCount;
        public final boolean converged;
        
        public ExecutionStats(int iterationCount, boolean converged) {
            this.iterationCount = iterationCount;
            this.converged = converged;
        }
        
        public int getIterationCount() { return iterationCount; }
        public boolean hasConverged() { return converged; }
    }
    
    /**
     * Gets the number of threads used by this solver.
     * 
     * @return The number of threads
     */
    public int getNumThreads() {
        return numThreads;
    }
    
    /**
     * Gets the maximum iterations used by this solver.
     * 
     * @return The maximum iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }
    
    /**
     * Shuts down the solver and releases all resources.
     * Should be called when the solver is no longer needed.
     */
    public void shutdown() {
        if (engine != null) {
            engine.shutdown();
        }
    }
}
