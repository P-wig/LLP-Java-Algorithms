package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;
import java.util.function.BinaryOperator;

/**
 * Parallel Prefix (Scan) Algorithm using the simplified LLP framework.
 * 
 * Problem Description:
 * The parallel prefix (also called scan) algorithm computes all prefix results
 * of an array given an associative operator. For an array [a1, a2, ..., an] and
 * an associative operator ⊕, it computes [a1, a1⊕a2, a1⊕a2⊕a3, ..., a1⊕a2⊕...⊕an].
 * Common examples include prefix sums, prefix products, and other reductions.
 * 
 * LLP Implementation Strategy:
 * - State: Array of prefix values with thread-safe operations
 * - Forbidden: When no more prefix positions can be computed
 * - Advance: Compute new prefix values using available previous results
 * - Parallelism: Multiple threads can compute different positions simultaneously
 */
public class ParallelPrefixProblem {
    
    /**
     * Simplified state for Parallel Prefix algorithm.
     * Uses thread-safe operations for parallel updates.
     */
    static class PrefixState {
        final int n;                                    // Number of elements in the array
        final int[] inputArray;                         // Original input values (readonly)
        volatile int[] prefixArray;                     // Current prefix computation results
        volatile boolean[] computed;                    // Flags indicating which positions are computed
        final BinaryOperator<Integer> operator;        // Associative operation (e.g., Integer::sum)
        volatile int iterationCount;                    // Track iterations
        final Object lock = new Object();              // Synchronization lock
        
        public PrefixState(int[] inputArray, BinaryOperator<Integer> operator) {
            this.n = inputArray.length;
            this.inputArray = inputArray.clone();
            this.prefixArray = new int[n];
            this.computed = new boolean[n];
            this.operator = operator;
            this.iterationCount = 0;
            
            // Initialize: only prefix[0] = input[0] is known initially
            this.prefixArray[0] = inputArray[0];
            this.computed[0] = true;
            
            // Mark all other positions as uncomputed
            for (int i = 1; i < n; i++) {
                this.computed[i] = false;
                this.prefixArray[i] = 0; // Default value for uncomputed positions
            }
        }
        
        /**
         * Thread-safe method to mark a position as computed with given value.
         */
        public synchronized boolean setComputed(int position, int value) {
            if (!computed[position]) {
                prefixArray[position] = value;
                computed[position] = true;
                return true; // Successfully computed new position
            }
            return false; // Position was already computed
        }
        
        /**
         * Thread-safe method to increment iteration count.
         */
        public synchronized void incrementIterations() {
            iterationCount++;
        }
        
        /**
         * Get current iteration count.
         */
        public int getIterationCount() {
            return iterationCount;
        }
        
        /**
         * Returns true if all positions have been computed.
         */
        public boolean isComplete() {
            for (int i = 0; i < n; i++) {
                if (!computed[i]) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Computes the correct prefix value for a given position using sequential definition.
         */
        public int computeCorrectPrefix(int position) {
            if (position == 0) {
                return inputArray[0];
            }
            
            // Sequential computation: apply operator from start to position
            int result = inputArray[0];
            for (int i = 1; i <= position; i++) {
                result = operator.apply(result, inputArray[i]);
            }
            return result;
        }
        
        /**
         * Check if computed values are correct.
         */
        public boolean hasIncorrectValues() {
            for (int i = 0; i < n; i++) {
                if (computed[i]) {
                    int correctValue = computeCorrectPrefix(i);
                    if (prefixArray[i] != correctValue) {
                        return true; // Found incorrect value
                    }
                }
            }
            return false; // All values are correct
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PrefixState{n=").append(n).append(", prefix=[");
            
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                if (computed[i]) {
                    sb.append(prefixArray[i]);
                } else {
                    sb.append("?");
                }
            }
            
            sb.append("], computed=").append(countComputed()).append("/").append(n).append("}");
            return sb.toString();
        }
        
        /**
         * Helper method to count how many positions are computed.
         */
        private int countComputed() {
            int count = 0;
            for (boolean c : computed) {
                if (c) count++;
            }
            return count;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PrefixState)) return false;
            PrefixState other = (PrefixState) obj;
            return n == other.n &&
                   Arrays.equals(inputArray, other.inputArray) &&
                   Arrays.equals(prefixArray, other.prefixArray) &&
                   Arrays.equals(computed, other.computed);
        }
    }

    /**
     * Parallel Prefix problem using simplified LLP framework.
     */
    static class ParallelPrefixLLPProblem implements LLPProblem<PrefixState> {
        
        private final int[] inputArray;
        private final BinaryOperator<Integer> operator;
        
        public ParallelPrefixLLPProblem(int[] inputArray, BinaryOperator<Integer> operator) {
            this.inputArray = inputArray.clone();
            this.operator = operator;
        }
        
        @Override
        public boolean Forbidden(PrefixState state) {
            // Check if any position CAN be computed (work available)
            for (int i = 1; i < state.n; i++) {
                if (!state.computed[i] && state.computed[i - 1]) {
                    return true; // Found work to do - state is "forbidden" (needs iteration)
                }
            }
            return false; // No work to do - state is fine as-is
        }
        
        @Override
        public PrefixState Advance(PrefixState state, int threadId, int totalThreads) {
            // Increment iterations (Thread-0 only)
            if (threadId == 0) {
                state.incrementIterations();
            }
            
            // Find the next uncomputed position that CAN be computed
            int nextPosition = -1;
            for (int i = 1; i < state.n; i++) {
                if (!state.computed[i] && state.computed[i - 1]) {
                    nextPosition = i;
                    break; // Process only the FIRST available position
                }
            }
            
            if (nextPosition >= 0) {
                // Only Thread-0 computes the position (like Floyd-Warshall)
                if (threadId == 0) {
                    int newValue = state.operator.apply(state.prefixArray[nextPosition - 1], state.inputArray[nextPosition]);
                    state.setComputed(nextPosition, newValue);
                }
            }
            
            return state;
        }
        
        @Override
        public PrefixState getInitialState() {
            return new PrefixState(inputArray, operator);
        }
        
        @Override
        public boolean isSolution(PrefixState state) {
            return state.isComplete();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Prefix (Scan) Example ===\n");
        
        // Small arrays (overhead-dominated)
        runTestCase("Small Sum", new int[]{1, 2, 3, 4, 5}, Integer::sum, new int[]{1, 3, 6, 10, 15});
        System.out.println();
        
        // Large arrays (computation-dominated)
        int[] largeInput = new int[50];
        Arrays.fill(largeInput, 1);
        int[] largeExpected = new int[50];
        for (int i = 0; i < 50; i++) {
            largeExpected[i] = i + 1;
        }
        runTestCase("Large Sum (50 elements)", largeInput, Integer::sum, largeExpected);
        System.out.println();
    }

    private static void runTestCase(String testName, int[] input, BinaryOperator<Integer> operator, int[] expected) {
        System.out.println("=== " + testName + " ===");
        System.out.println("Input: " + Arrays.toString(input));
        System.out.println("Expected: " + Arrays.toString(expected));
        
        int[] threadCounts = {1, 2, 4, 8};
        int maxIterations = 50;
        
        double localBaselineTime = 0.0; // Local baseline for this test case
        
        // Test different thread counts
        for (int numThreads : threadCounts) {
            localBaselineTime = solveProblem(input, operator, expected, numThreads, maxIterations, localBaselineTime);
        }
    }

    private static double solveProblem(int[] input, BinaryOperator<Integer> operator, int[] expected, 
                                 int numThreads, int maxIterations, double baselineTime) {
        LLPSolver<PrefixState> solver = null;
        
        try {
            ParallelPrefixLLPProblem problem = new ParallelPrefixLLPProblem(input, operator);
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.nanoTime();
            PrefixState solution = solver.solve();
            long endTime = System.nanoTime();
            
            // Show results in compact format
            double timeMs = (endTime - startTime) / 1_000_000.0;
            int iterations = solution.getIterationCount();
            boolean valid = problem.isSolution(solution);
            boolean correct = Arrays.equals(solution.prefixArray, expected);
            
            System.out.printf("Threads: %2d | Time: %8.2fms | Iterations: %3d | Valid: %s | Correct: %s", 
                             numThreads, timeMs, iterations, valid, correct);
            
            // Show speedup relative to single thread FOR THIS TEST CASE
            if (numThreads == 1) {
                System.out.println(" | Speedup: 1.00x (baseline)");
                baselineTime = timeMs; // Set baseline for this test case
            } else {
                double speedup = baselineTime / timeMs;
                System.out.printf(" | Speedup: %.2fx\n", speedup);
            }
            
            // Show detailed results for first run only
            if (numThreads == 1) {
                System.out.println("Result:   " + Arrays.toString(solution.prefixArray));
                System.out.println("Complete: " + solution.isComplete());
                System.out.println("Computed: " + Arrays.toString(solution.computed));
                System.out.println();
            }
            
            return baselineTime; // Return baseline for next iteration
            
        } catch (Exception e) {
            System.err.println("Error with " + numThreads + " threads: " + e.getMessage());
            e.printStackTrace();
            return baselineTime;
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }
}
