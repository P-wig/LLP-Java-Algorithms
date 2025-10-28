package com.llp.framework;

import com.llp.algorithm.LLPProblem;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Flexible streams-based execution engine that properly uses Forbidden.
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
     * Execute using streams-based parallel approach with proper Forbidden usage.
     */
    public T execute(T initialState) {
        AtomicReference<T> currentState = new AtomicReference<>(initialState);
        T previousState = initialState;
        
        for (iterationCount = 0; iterationCount < maxIterations; iterationCount++) {
            
            T current = currentState.get();
            
            // PROPER FORBIDDEN USAGE - Check state first
            if (problem.Forbidden(current)) {
                // State is forbidden - fix it with Ensure
                T afterEnsure = IntStream.range(0, parallelism)
                    .parallel()
                    .mapToObj(i -> problem.Ensure(currentState.get()))
                    .reduce(currentState.get(), this::mergeStates);
                currentState.set(afterEnsure);
                
                System.out.println("  Iteration " + iterationCount + ": Fixed forbidden state");
                
            } else {
                // State is valid - make progress with Advance
                T afterAdvance = IntStream.range(0, parallelism)
                    .parallel()
                    .mapToObj(i -> problem.Advance(currentState.get()))
                    .reduce(currentState.get(), this::mergeStates);
                currentState.set(afterAdvance);
                
                // Check if Advance created forbidden state
                T advanced = currentState.get();
                if (problem.Forbidden(advanced)) {
                    // Fix the violation immediately
                    T afterEnsure = IntStream.range(0, parallelism)
                        .parallel()
                        .mapToObj(i -> problem.Ensure(advanced))
                        .reduce(advanced, this::mergeStates);
                    currentState.set(afterEnsure);
                    
                    System.out.println("  Iteration " + iterationCount + ": Advanced then fixed violation");
                } else {
                    System.out.println("  Iteration " + iterationCount + ": Advanced without violation");
                }
            }
            
            // Check termination conditions
            current = currentState.get();
            
            // Solution found?
            if (problem.isSolution(current)) {
                // Final verification - solution must not be forbidden
                if (!problem.Forbidden(current)) {
                    converged = true;
                    break;
                } else {
                    // Solution is forbidden - this shouldn't happen but fix it
                    currentState.set(problem.Ensure(current));
                }
            }
            
            // No progress made?
            if (current.equals(previousState)) {
                converged = true;
                break;
            }
            
            previousState = current;
        }
        
        return currentState.get();
    }
    
    /**
     * Merge states from parallel operations - prefers non-forbidden states.
     */
    private T mergeStates(T state1, T state2) {
        boolean forbidden1 = problem.Forbidden(state1);
        boolean forbidden2 = problem.Forbidden(state2);
        
        // Prefer non-forbidden states
        if (forbidden1 && !forbidden2) {
            return state2;
        }
        if (!forbidden1 && forbidden2) {
            return state1;
        }
        
        // Both same forbidden status - choose arbitrarily
        return state2;
    }
    
    // Simple getters for statistics
    public int getIterationCount() {
        return iterationCount;
    }
    
    public boolean hasConverged() {
        return converged;
    }
    
    public int getNumThreads() {
        return parallelism;
    }
    
    public void shutdown() {
        // No explicit cleanup needed for streams
    }
}
