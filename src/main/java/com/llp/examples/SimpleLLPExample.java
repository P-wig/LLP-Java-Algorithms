package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

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
     * Example usage of the LLP library.
     */
    public static void main(String[] args) {
        System.out.println("=== Simple LLP Example ===\n");
        
        // Create a problem instance
        CounterProblem problem = new CounterProblem(10);
        
        System.out.println("Problem: Count from 0 to 10");
        System.out.println("Initial state: " + problem.getInitialState());
        
        // Demonstrate the three core methods
        CounterState state = problem.getInitialState();
        System.out.println("\nDemonstrating LLP methods:");
        
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
        
        System.out.println("\n=== Example Complete ===");
    }
}
