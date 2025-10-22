package com.llp.framework;

import com.llp.algorithm.LLPProblem;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The core execution engine for parallel LLP algorithms.
 * Orchestrates parallel execution of Forbidden, Ensure, and Advance operations
 * across multiple threads with proper synchronization.
 * 
 * @param <T> The type representing the problem state
 */
public class LLPEngine<T> {
    
    private final LLPProblem<T> problem;
    private final ExecutorService executor;
    private final int numThreads;
    private final LLPTerminationDetector terminationDetector;
    
    /**
     * Creates a new LLP engine for the given problem.
     * 
     * @param problem The LLP problem to solve
     * @param numThreads Number of parallel threads to use
     */
    public LLPEngine(LLPProblem<T> problem, int numThreads) {
        this(problem, numThreads, Integer.MAX_VALUE);
    }
    
    /**
     * Creates a new LLP engine with iteration limit.
     * 
     * @param problem The LLP problem to solve
     * @param numThreads Number of parallel threads to use
     * @param maxIterations Maximum iterations before termination
     */
    public LLPEngine(LLPProblem<T> problem, int numThreads, int maxIterations) {
        this.problem = problem;
        this.numThreads = numThreads;
        this.executor = Executors.newFixedThreadPool(numThreads);
        this.terminationDetector = new LLPTerminationDetector(maxIterations);
    }
    
    /**
     * Executes the parallel LLP algorithm.
     * This method orchestrates the main execution loop:
     * 1. Apply Advance operations in parallel
     * 2. Synchronize threads
     * 3. Apply Ensure operations in parallel
     * 4. Check for convergence/termination
     * 
     * @param initialState The initial problem state
     * @return The final solution state
     * @throws InterruptedException if execution is interrupted
     * @throws ExecutionException if an error occurs during execution
     */
    public T execute(T initialState) throws InterruptedException, ExecutionException {
        LLPState<T> state = new LLPState<>(initialState);
        LLPBarrier barrier = new LLPBarrier(numThreads);
        AtomicBoolean continueExecution = new AtomicBoolean(true);
        
        // Main execution loop
        while (!terminationDetector.shouldTerminate() && continueExecution.get()) {
            terminationDetector.incrementIteration();
            
            // Phase 1: Advance in parallel
            executePhase(state, barrier, Phase.ADVANCE);
            
            // Phase 2: Ensure in parallel
            executePhase(state, barrier, Phase.ENSURE);
            
            // Check termination conditions
            T currentState = state.snapshot();
            if (problem.isSolution(currentState)) {
                terminationDetector.markConverged();
            }
            
            // Check if state is forbidden (indicates need to continue)
            if (!problem.Forbidden(currentState) && problem.isSolution(currentState)) {
                continueExecution.set(false);
            }
        }
        
        return state.snapshot();
    }
    
    /**
     * Executes a single phase (Advance or Ensure) across all threads.
     * 
     * @param state The shared state
     * @param barrier Synchronization barrier
     * @param phase The phase to execute
     * @throws InterruptedException if interrupted
     * @throws ExecutionException if execution fails
     */
    private void executePhase(LLPState<T> state, LLPBarrier barrier, Phase phase) 
            throws InterruptedException, ExecutionException {
        
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Get current state
                    T currentState = state.get();
                    
                    // Apply appropriate operation based on phase
                    T newState = null;
                    if (phase == Phase.ADVANCE) {
                        newState = problem.Advance(currentState);
                    } else if (phase == Phase.ENSURE) {
                        newState = problem.Ensure(currentState);
                    }
                    
                    // Update shared state if changed
                    if (newState != null && newState != currentState) {
                        state.set(newState);
                    }
                    
                    // Synchronize at barrier
                    barrier.await();
                    
                } catch (Exception e) {
                    terminationDetector.forceStop();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete this phase
        latch.await();
    }
    
    /**
     * Gets the termination detector for monitoring execution state.
     * 
     * @return The termination detector
     */
    public LLPTerminationDetector getTerminationDetector() {
        return terminationDetector;
    }
    
    /**
     * Gets the number of threads used by this engine.
     * 
     * @return The number of threads
     */
    public int getNumThreads() {
        return numThreads;
    }
    
    /**
     * Shuts down the engine and releases resources.
     * Should be called when the engine is no longer needed.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Enumeration of execution phases.
     */
    private enum Phase {
        ADVANCE,
        ENSURE
    }
}
