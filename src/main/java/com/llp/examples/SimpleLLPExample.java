package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;
import java.util.Random;

/**
 * Simple LLP Example - Demonstrates the framework with a basic counter problem.
 * 
 * Problem: Count from 0 to a target value, but sometimes we "overshoot" and need correction.
 * - Forbidden: when value > target (overshoot)
 * - Ensure: fix overshoot by setting value = target
 * - Advance: increment value toward target
 */
public class SimpleLLPExample {
    
    /**
     * Immutable state class for the array sum problem.
     */
    static class ArraySumState {
        final int[] array;        // Original array (readonly)
        final int[] partialSums;  // Partial sums computed so far
        final boolean[] computed; // Which elements have been computed
        
        public ArraySumState(int[] array) {
            this.array = array.clone();
            this.partialSums = new int[array.length];
            this.computed = new boolean[array.length];
        }
        
        public ArraySumState(int[] array, int[] partialSums, boolean[] computed) {
            this.array = array.clone();
            this.partialSums = partialSums.clone();
            this.computed = computed.clone();
        }
        
        public ArraySumState withComputed(int index, int sum) {
            int[] newSums = partialSums.clone();
            boolean[] newComputed = computed.clone();
            newSums[index] = sum;
            newComputed[index] = true;
            return new ArraySumState(array, newSums, newComputed);
        }
        
        @Override
        public String toString() {
            return String.format("ArraySum{sums=%s, computed=%s}", 
                               Arrays.toString(partialSums), Arrays.toString(computed));
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ArraySumState)) return false;
            ArraySumState other = (ArraySumState) obj;
            return Arrays.equals(partialSums, other.partialSums) && 
                   Arrays.equals(computed, other.computed);
        }
    }

    /**
     * Problem implementation for computing the sum of an array using the LLP framework.
     */
    static class ArraySumProblem implements LLPProblem<ArraySumState> {
        
        @Override
        public boolean Forbidden(ArraySumState state) {
            // State is forbidden if any computed sum is wrong
            for (int i = 0; i < state.array.length; i++) {
                if (state.computed[i] && state.partialSums[i] != state.array[i]) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public ArraySumState Ensure(ArraySumState state) {
            ArraySumState result = state;
            for (int i = 0; i < state.array.length; i++) {
                if (state.computed[i] && state.partialSums[i] != state.array[i]) {
                    System.out.println("    Fixing sum at " + i + ": " + state.partialSums[i] + " → " + state.array[i]);
                    result = result.withComputed(i, state.array[i]);
                }
            }
            return result;
        }
        
        @Override
        public ArraySumState Advance(ArraySumState state) {
            // Find an uncomputed element and compute it
            Random rand = new Random();
            for (int attempt = 0; attempt < state.array.length; attempt++) {
                int i = rand.nextInt(state.array.length);
                if (!state.computed[i]) {
                    System.out.println("    Thread computing element " + i + ": " + state.array[i]);
                    return state.withComputed(i, state.array[i]);
                }
            }
            return state; // All computed
        }
        
        @Override
        public ArraySumState getInitialState() {
            return new ArraySumState(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        }
        
        @Override
        public boolean isSolution(ArraySumState state) {
            for (boolean c : state.computed) {
                if (!c) return false;
            }
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Array Sum Example ===\n");
        
        // Problem parameters
        int numThreads = 4;  // More threads to see parallelism
        int maxIterations = 100;
        
        // Create the problem
        ArraySumProblem problem = new ArraySumProblem();
        
        System.out.println("Problem: Compute array sum in parallel");
        System.out.println("Initial state: " + problem.getInitialState());
        System.out.println("Threads: " + numThreads);
        
        // Solve using the LLP framework
        solveProblem(problem, numThreads, maxIterations);
    }

    private static void solveProblem(ArraySumProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<ArraySumState> solver = null;
        
        try {
            // Create solver with simple constructor
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            ArraySumState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found: " + solution);
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            
            // Get statistics
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            if (stats != null) {
                System.out.println("Total iterations: " + stats.getIterationCount());
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
