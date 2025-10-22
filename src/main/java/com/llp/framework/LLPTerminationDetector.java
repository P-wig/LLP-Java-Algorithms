package com.llp.framework;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detects termination conditions for parallel LLP execution.
 * Tracks convergence, iteration limits, and other stopping criteria.
 */
public class LLPTerminationDetector {
    
    private final AtomicBoolean converged;
    private final AtomicInteger iterationCount;
    private final int maxIterations;
    private final AtomicBoolean forceStop;
    
    /**
     * Creates a new termination detector with default settings.
     */
    public LLPTerminationDetector() {
        this(Integer.MAX_VALUE);
    }
    
    /**
     * Creates a new termination detector with a maximum iteration limit.
     * 
     * @param maxIterations Maximum number of iterations before forcing termination
     */
    public LLPTerminationDetector(int maxIterations) {
        this.converged = new AtomicBoolean(false);
        this.iterationCount = new AtomicInteger(0);
        this.maxIterations = maxIterations;
        this.forceStop = new AtomicBoolean(false);
    }
    
    /**
     * Marks the algorithm as converged.
     */
    public void markConverged() {
        converged.set(true);
    }
    
    /**
     * Resets the convergence flag.
     */
    public void resetConvergence() {
        converged.set(false);
    }
    
    /**
     * Checks if the algorithm has converged.
     * 
     * @return true if converged, false otherwise
     */
    public boolean hasConverged() {
        return converged.get();
    }
    
    /**
     * Increments the iteration counter.
     * 
     * @return The new iteration count
     */
    public int incrementIteration() {
        return iterationCount.incrementAndGet();
    }
    
    /**
     * Gets the current iteration count.
     * 
     * @return The iteration count
     */
    public int getIterationCount() {
        return iterationCount.get();
    }
    
    /**
     * Resets the iteration counter to zero.
     */
    public void resetIterationCount() {
        iterationCount.set(0);
    }
    
    /**
     * Checks if the maximum iteration limit has been reached.
     * 
     * @return true if limit reached, false otherwise
     */
    public boolean maxIterationsReached() {
        return iterationCount.get() >= maxIterations;
    }
    
    /**
     * Forces the algorithm to stop immediately.
     */
    public void forceStop() {
        forceStop.set(true);
    }
    
    /**
     * Checks if a force stop has been requested.
     * 
     * @return true if force stop requested, false otherwise
     */
    public boolean shouldStop() {
        return forceStop.get();
    }
    
    /**
     * Checks if the algorithm should terminate based on all conditions.
     * 
     * @return true if any termination condition is met, false otherwise
     */
    public boolean shouldTerminate() {
        return hasConverged() || maxIterationsReached() || shouldStop();
    }
    
    /**
     * Resets all termination flags and counters.
     */
    public void reset() {
        converged.set(false);
        iterationCount.set(0);
        forceStop.set(false);
    }
}
