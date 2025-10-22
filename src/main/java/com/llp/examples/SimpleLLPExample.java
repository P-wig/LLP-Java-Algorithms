package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.concurrent.ExecutionException;

/**
 * A simple example demonstrating how to use the LLP library.
 * This example implements a basic problem to show the pattern.
 */
public class SimpleLLPExample {
    
    /**
     * Example state class representing a simple counter problem.
     * Goal: Count from 0 to a target value.
     */
    static class CounterState {
        int value;
        int target;
        
        public CounterState(int value, int target) {
            this.value = value;
            this.target = target;
        }
        
        @Override
        public String toString() {
            return "Counter{value=" + value + ", target=" + target + "}";
        }
    }
    
    /**
     * Example problem: Count to a target number.
     * This demonstrates the structure of implementing LLPProblem.
     */
    static class CounterProblem implements LLPProblem<CounterState> {
        
        private final int target;
        
        public CounterProblem(int target) {
            this.target = target;
        }
        
        @Override
        public boolean Forbidden(CounterState state) {
            // A state is forbidden if the counter exceeds the target
            return state.value > state.target;
        }
        
        @Override
        public CounterState Ensure(CounterState state) {
            // If forbidden (value > target), reset to target
            if (Forbidden(state)) {
                return new CounterState(state.target, state.target);
            }
            return state;
        }
        
        @Override
        public CounterState Advance(CounterState state) {
            // Advance by incrementing the counter
            if (state.value < state.target) {
                return new CounterState(state.value + 1, state.target);
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
    
    /**
     * Example usage of the LLP library framework.
     */
    public static void main(String[] args) {
        System.out.println("=== Simple LLP Example ===\n");
        
        // Create a problem instance
        CounterProblem problem = new CounterProblem(10);
        
        System.out.println("Problem: Count from 0 to 10");
        System.out.println("Initial state: " + problem.getInitialState());
        
        // Part 1: Demonstrate the three core methods manually
        System.out.println("\n--- Part 1: Manual Demonstration ---");
        CounterState state = problem.getInitialState();
        System.out.println("Demonstrating LLP methods:");
        
        for (int i = 0; i < 12; i++) {
            System.out.println("\nIteration " + i + ":");
            System.out.println("  Current state: " + state);
            System.out.println("  Is Forbidden? " + problem.Forbidden(state));
            System.out.println("  Is Solution? " + problem.isSolution(state));
            
            // Apply Advance
            state = problem.Advance(state);
            System.out.println("  After Advance: " + state);
            
            // Apply Ensure
            state = problem.Ensure(state);
            System.out.println("  After Ensure: " + state);
            
            if (problem.isSolution(state)) {
                System.out.println("\n✓ Solution found!");
                break;
            }
        }
        
        // Part 2: Demonstrate using the LLPSolver framework
        System.out.println("\n--- Part 2: Using LLPSolver Framework ---");
        demonstrateFramework();
        
        System.out.println("\n=== Example Complete ===");
    }
    
    /**
     * Demonstrates using the LLP framework to solve the problem.
     */
    private static void demonstrateFramework() {
        CounterProblem problem = new CounterProblem(10);
        LLPSolver<CounterState> solver = null;
        
        try {
            System.out.println("Creating solver with default configuration...");
            solver = new LLPSolver<>(problem);
            
            System.out.println("Solving with parallel LLP algorithm...");
            CounterState solution = solver.solve();
            
            System.out.println("\n✓ Solution found using framework!");
            System.out.println("  Final state: " + solution);
            
            // Display execution statistics
            if (solver.getTerminationDetector() != null) {
                System.out.println("  Iterations: " + 
                    solver.getTerminationDetector().getIterationCount());
                System.out.println("  Converged: " + 
                    solver.getTerminationDetector().hasConverged());
            }
            
        } catch (Exception e) {
            System.err.println("Error during execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
                System.out.println("\nSolver resources cleaned up.");
            }
        }
    }
}
