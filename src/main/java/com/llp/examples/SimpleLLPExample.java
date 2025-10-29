package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;

/**
 * Parallel Prefix Sum Example - Demonstrates real parallel computation.
 * 
 * Problem: Compute prefix sums of an array in parallel
 * - Input: [1, 2, 3, 4, 5]
 * - Output: [1, 3, 6, 10, 15]  (prefix sums)
 * - Forbidden: prefix[i] != sum(array[0..i])
 * - Advance: compute prefix[i] = prefix[i-1] + array[i]
 * - Ensure: fix incorrect prefix sums
 */
public class SimpleLLPExample {
    
    /**
     * State for parallel prefix sum computation.
     */
    static class PrefixSumState {
        final int[] array;      // Original array (readonly)
        final int[] prefixSum;  // Prefix sums being computed
        final boolean[] computed; // Which prefix sums are computed
        
        public PrefixSumState(int[] array) {
            this.array = array.clone();
            this.prefixSum = new int[array.length];
            this.computed = new boolean[array.length];
        }
        
        public PrefixSumState(int[] array, int[] prefixSum, boolean[] computed) {
            this.array = array.clone();
            this.prefixSum = prefixSum.clone();
            this.computed = computed.clone();
        }
        
        public PrefixSumState withPrefixSum(int index, int sum) {
            int[] newPrefixSum = prefixSum.clone();
            boolean[] newComputed = computed.clone();
            newPrefixSum[index] = sum;
            newComputed[index] = true;
            return new PrefixSumState(array, newPrefixSum, newComputed);
        }
        
        @Override
        public String toString() {
            return String.format("PrefixSum{array=%s, prefix=%s, computed=%s}", 
                               Arrays.toString(array), 
                               Arrays.toString(prefixSum), 
                               Arrays.toString(computed));
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PrefixSumState)) return false;
            PrefixSumState other = (PrefixSumState) obj;
            return Arrays.equals(array, other.array) && 
                   Arrays.equals(prefixSum, other.prefixSum) && 
                   Arrays.equals(computed, other.computed);
        }
    }

    /**
     * Parallel prefix sum problem using LLP framework.
     */
    static class PrefixSumProblem implements LLPProblem<PrefixSumState> {
        
        @Override
        public boolean Forbidden(PrefixSumState state) {
            // Check if any computed prefix sum is incorrect
            for (int i = 0; i < state.array.length; i++) {
                if (state.computed[i]) {
                    int expectedSum = computeExpectedPrefix(state.array, i);
                    if (state.prefixSum[i] != expectedSum) {
                        return true; // Incorrect prefix sum
                    }
                }
            }
            return false;
        }
        
        @Override
        public PrefixSumState Ensure(PrefixSumState state) {
            // Fix any incorrect prefix sums
            PrefixSumState result = state;
            for (int i = 0; i < state.array.length; i++) {
                if (state.computed[i]) {
                    int expectedSum = computeExpectedPrefix(state.array, i);
                    if (state.prefixSum[i] != expectedSum) {
                        System.out.println("    Fixing prefix[" + i + "]: " + 
                                         state.prefixSum[i] + " → " + expectedSum);
                        result = result.withPrefixSum(i, expectedSum);
                    }
                }
            }
            return result;
        }
        
        @Override
        public PrefixSumState Advance(PrefixSumState state) {
            // Find an element whose prefix sum can be computed
            long threadId = Thread.currentThread().getId();
            int startIndex = (int)(threadId % state.array.length);
            
            for (int offset = 0; offset < state.array.length; offset++) {
                int i = (startIndex + offset) % state.array.length;
                
                if (!state.computed[i] && canCompute(state, i)) {
                    int prefixSum;
                    if (i == 0) {
                        // First element: prefix[0] = array[0]
                        prefixSum = state.array[0];
                    } else {
                        // Other elements: prefix[i] = prefix[i-1] + array[i]
                        prefixSum = state.prefixSum[i-1] + state.array[i];
                    }
                    
                    System.out.println("    Thread-" + threadId + " computing prefix[" + i + "]: " + 
                                     (i == 0 ? state.array[i] : state.prefixSum[i-1] + " + " + state.array[i]) + 
                                     " = " + prefixSum);
                    return state.withPrefixSum(i, prefixSum);
                }
            }
            return state; // No progress possible
        }
        
        /**
         * Check if prefix[i] can be computed (dependencies satisfied).
         */
        private boolean canCompute(PrefixSumState state, int i) {
            if (i == 0) {
                return true; // First element can always be computed
            }
            return state.computed[i-1]; // Need previous prefix sum
        }
        
        /**
         * Compute expected prefix sum for validation.
         */
        private int computeExpectedPrefix(int[] array, int index) {
            int sum = 0;
            for (int i = 0; i <= index; i++) {
                sum += array[i];
            }
            return sum;
        }
        
        @Override
        public PrefixSumState getInitialState() {
            return new PrefixSumState(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        }
        
        @Override
        public boolean isSolution(PrefixSumState state) {
            // All prefix sums must be computed
            for (boolean c : state.computed) {
                if (!c) return false;
            }
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Prefix Sum Example ===\n");
        
        // Problem parameters
        int numThreads = 2;
        int maxIterations = 100;
        
        // Create the problem
        PrefixSumProblem problem = new PrefixSumProblem();
        
        System.out.println("Problem: Compute prefix sums in parallel");
        System.out.println("Input array: " + Arrays.toString(problem.getInitialState().array));
        System.out.println("Expected output: [1, 3, 6, 10, 15, 21, 28, 36, 45, 55]");
        System.out.println("Initial state: " + problem.getInitialState());
        System.out.println("Threads: " + numThreads);
        
        // Solve using the LLP framework
        solveProblem(problem, numThreads, maxIterations);
    }

    private static void solveProblem(PrefixSumProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<PrefixSumState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            PrefixSumState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found!");
            System.out.println("Input array:  " + Arrays.toString(solution.array));
            System.out.println("Prefix sums:  " + Arrays.toString(solution.prefixSum));
            System.out.println("Computed:     " + Arrays.toString(solution.computed));
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            
            // Verify correctness
            int[] expected = {1, 3, 6, 10, 15, 21, 28, 36, 45, 55};
            boolean correct = Arrays.equals(solution.prefixSum, expected);
            System.out.println("Correct result? " + correct);
            
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
