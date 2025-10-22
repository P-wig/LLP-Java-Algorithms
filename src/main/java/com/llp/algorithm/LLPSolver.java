package com.llp.algorithm;

import com.llp.framework.LLPConfiguration;
import com.llp.framework.LLPEngine;
import com.llp.framework.LLPTerminationDetector;

import java.util.concurrent.*;

/**
 * The LLPSolver provides a high-level API for solving problems using the
 * parallel LLP (Least Lattice Predicate) algorithm framework.
 * 
 * This class wraps the LLPEngine and provides a simpler interface for
 * executing LLP algorithms with sensible defaults.
 * 
 * @param <T> The type representing the state or configuration in the lattice
 */
public class LLPSolver<T> {
    
    private final LLPProblem<T> problem;
    private final LLPConfiguration config;
    private LLPEngine<T> engine;
    
    /**
     * Creates a new LLPSolver for the given problem with a custom configuration.
     * 
     * @param problem The problem to solve
     * @param config The configuration settings
     */
    public LLPSolver(LLPProblem<T> problem, LLPConfiguration config) {
        this.problem = problem;
        this.config = config;
    }
    
    /**
     * Creates a new LLPSolver for the given problem with specified thread count.
     * 
     * @param problem The problem to solve
     * @param numThreads The number of parallel threads to use
     */
    public LLPSolver(LLPProblem<T> problem, int numThreads) {
        this.problem = problem;
        this.config = new LLPConfiguration().setNumThreads(numThreads);
    }
    
    /**
     * Creates a new LLPSolver with default configuration
     * (uses available processor count for threads).
     * 
     * @param problem The problem to solve
     */
    public LLPSolver(LLPProblem<T> problem) {
        this.problem = problem;
        this.config = new LLPConfiguration();
    }
    
    /**
     * Solves the problem using the parallel LLP algorithm framework.
     * 
     * The algorithm structure:
     * 1. Initialize state from problem
     * 2. Create LLPEngine with configuration
     * 3. Execute parallel LLP algorithm:
     *    a. Apply Advance operations in parallel
     *    b. Synchronize threads at barrier
     *    c. Apply Ensure operations in parallel
     *    d. Check convergence and termination conditions
     * 4. Return the solution state
     * 
     * @return The solution state
     * @throws InterruptedException if the solving process is interrupted
     * @throws ExecutionException if an error occurs during execution
     */
    public T solve() throws InterruptedException, ExecutionException {
        // Get initial state from problem
        T initialState = problem.getInitialState();
        
        // Create and configure the engine
        engine = new LLPEngine<>(
            problem, 
            config.getNumThreads(), 
            config.getMaxIterations()
        );
        
        // Execute the parallel LLP algorithm
        T solution = engine.execute(initialState);
        
        return solution;
    }
    
    /**
     * Gets the termination detector from the current engine.
     * Useful for checking execution statistics after solving.
     * 
     * @return The termination detector, or null if solve() hasn't been called
     */
    public LLPTerminationDetector getTerminationDetector() {
        return engine != null ? engine.getTerminationDetector() : null;
    }
    
    /**
     * Gets the configuration used by this solver.
     * 
     * @return The configuration
     */
    public LLPConfiguration getConfiguration() {
        return config;
    }
    
    /**
     * Gets the number of threads used by this solver.
     * 
     * @return The number of threads
     */
    public int getNumThreads() {
        return config.getNumThreads();
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
