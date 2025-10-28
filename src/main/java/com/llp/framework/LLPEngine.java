package com.llp.framework;

import com.llp.algorithm.LLPProblem;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Streams-based execution engine for parallel LLP algorithms.
 * Uses Java 8+ parallel streams for coordination-free parallelism.
 */
public class LLPEngine<T> {
    
    private final LLPProblem<T> problem;
    private final int parallelism;
    private final LLPTerminationDetector terminationDetector;
    
    public LLPEngine(LLPProblem<T> problem, int parallelism, int maxIterations) {
        this.problem = problem;
        this.parallelism = parallelism;
        this.terminationDetector = new LLPTerminationDetector(maxIterations);
        
        // Set parallelism for streams
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
                          String.valueOf(parallelism));
    }
    
    /**
     * Execute using streams-based parallel approach.
     */
    public T execute(T initialState) {
        AtomicReference<T> currentState = new AtomicReference<>(initialState);
        
        while (!terminationDetector.shouldTerminate()) {
            terminationDetector.incrementIteration();
            
            // Advance Phase - parallel streams
            T afterAdvance = IntStream.range(0, parallelism)
                .parallel()
                .mapToObj(i -> problem.Advance(currentState.get()))
                .reduce(currentState.get(), this::mergeStates);
            currentState.set(afterAdvance);
            
            // Ensure Phase - parallel streams  
            T afterEnsure = IntStream.range(0, parallelism)
                .parallel()
                .mapToObj(i -> problem.Ensure(currentState.get()))
                .reduce(currentState.get(), this::mergeStates);
            currentState.set(afterEnsure);
            
            // Check termination
            T current = currentState.get();
            if (problem.isSolution(current) && !problem.Forbidden(current)) {
                terminationDetector.markConverged();
                break;
            }
        }
        
        return currentState.get();
    }
    
    /**
     * Merge states from parallel operations.
     */
    private T mergeStates(T state1, T state2) {
        // Prefer non-forbidden states
        if (problem.Forbidden(state1) && !problem.Forbidden(state2)) {
            return state2;
        }
        if (!problem.Forbidden(state1) && problem.Forbidden(state2)) {
            return state1;
        }
        return state2; // Default to second state
    }
    
    public LLPTerminationDetector getTerminationDetector() {
        return terminationDetector;
    }
    
    public int getNumThreads() {
        return parallelism;
    }
    
    public void shutdown() {
        // No explicit cleanup needed for streams
    }
}
