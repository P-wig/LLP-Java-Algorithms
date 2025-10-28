package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

/**
 * Simple LLP Example - Clean version for real problem solving.
 */
public class SimpleLLPExample {
    
    static class CounterState {
        final int value;
        final int target;
        
        public CounterState(int value, int target) {
            this.value = value;
            this.target = target;
        }
        
        public CounterState withValue(int newValue) {
            return new CounterState(newValue, this.target);
        }
        
        @Override
        public String toString() {
            return String.format("Counter{value=%d, target=%d}", value, target);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CounterState)) return false;
            CounterState other = (CounterState) obj;
            return value == other.value && target == other.target;
        }
    }
    

    static class CounterProblem implements LLPProblem<CounterState> {
        
        private final int target;
        
        public CounterProblem(int target) {
            this.target = target;
        }
        
        @Override
        public boolean Forbidden(CounterState state) {
            return state.value > state.target;
        }
        
        @Override
        public CounterState Ensure(CounterState state) {
            if (Forbidden(state)) {
                return state.withValue(state.target);
            }
            return state;
        }
        
        @Override
        public CounterState Advance(CounterState state) {
            if (state.value < state.target) {
                return state.withValue(state.value + 1);
            }
            return state;
        }
        
        @Override
        public CounterState getInitialState() {
            return new CounterState(0, target);
        }
        
        @Override
        public boolean isSolution(CounterState state) {
            return state.value == state.target && !Forbidden(state);
        }
    }
    
    
    public static void main(String[] args) {
        System.out.println("=== Simple LLP Counter Example ===\n");
        
        // Create and solve the problem
        CounterProblem problem = new CounterProblem(5);
        
        System.out.println("Problem: Count from 0 to 5");
        System.out.println("Initial state: " + problem.getInitialState());
        
        // Solve using LLP framework - SIMPLIFIED!
        LLPSolver<CounterState> solver = null;
        
        try {
            // Simple constructor - 2 threads, 100 max iterations
            solver = new LLPSolver<>(problem, 2, 100);
            
            System.out.println("\nSolving with LLP framework...");
            System.out.println("Using " + solver.getNumThreads() + " threads");
            System.out.println("Max iterations: " + solver.getMaxIterations());
            
            long startTime = System.currentTimeMillis();
            CounterState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("✓ Solution found: " + solution);
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            if (stats != null) {
                System.out.println("Iterations: " + stats.getIterationCount());
                System.out.println("Converged: " + stats.hasConverged());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
        
        System.out.println("\n=== Example Complete ===");
    }
}
