package com.llp.problems;

import java.util.Arrays;
import java.util.function.BinaryOperator;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

/**
 * Parallel Prefix (Scan) Algorithm using the LLP framework.
 * 
 * Problem Description:
 * The parallel prefix (also called scan) algorithm computes all prefix results
 * of an array given an associative operator. For an array [a1, a2, ..., an] and
 * an associative operator ⊕, it computes [a1, a1⊕a2, a1⊕a2⊕a3, ..., a1⊕a2⊕...⊕an].
 * Common examples include prefix sums, prefix products, and other reductions.
 * 
 * LLP Implementation Strategy:
 * - State: Array of prefix values with completion flags
 * - Forbidden: Prefix values that don't match sequential computation
 * - Advance: Compute new prefix values using previous results
 * - Ensure: Fix incorrect prefix computations
 * - Parallelism: Multiple positions can be computed simultaneously
 */
public class ParallelPrefixProblem implements LLPProblem<ParallelPrefixProblem.PrefixState> {
    
    /**
     * State class representing the current prefix computation state.
     */
    public static class PrefixState {
        public final int n;                              // Number of elements in the array
        public final int[] inputArray;                   // Original input values (immutable)
        public final int[] prefixArray;                  // Current prefix computation results
        public final boolean[] computed;                 // Flags indicating which positions are computed
        public final BinaryOperator<Integer> operator;  // Associative operation (e.g., Integer::sum)

        /**
         * Creates initial state with given input array and operator, only first position computed.
         */
        public PrefixState(int[] inputArray, BinaryOperator<Integer> operator) {
            this.n = inputArray.length;
            this.inputArray = Arrays.copyOf(inputArray, n);
            this.prefixArray = new int[n];
            this.computed = new boolean[n];
            this.operator = operator;
            
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
         * Copy constructor for creating new states with updated prefix values.
         */
        public PrefixState(int[] inputArray, int[] prefixArray, boolean[] computed, BinaryOperator<Integer> operator) {
            this.n = inputArray.length;
            this.inputArray = inputArray; // Share immutable input array
            this.prefixArray = Arrays.copyOf(prefixArray, n);
            this.computed = Arrays.copyOf(computed, n);
            this.operator = operator;
        }

        /**
         * Creates new state with the specified position marked as computed with given value.
         */
        public PrefixState withComputed(int position, int value) {
            int[] newPrefixArray = Arrays.copyOf(prefixArray, n);
            boolean[] newComputed = Arrays.copyOf(computed, n);
            
            // Update the specified position
            newPrefixArray[position] = value;
            newComputed[position] = true;
            
            return new PrefixState(inputArray, newPrefixArray, newComputed, operator);
        }
        
        /**
         * Returns true if all positions have been computed (complete prefix array).
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
         * Computes the correct prefix value for a given position using the sequential definition.
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
    }

    private final int[] inputArray;           // Original input values
    private final BinaryOperator<Integer> operator;   // Associative operation to apply

    /**
     * Creates a new parallel prefix problem instance with given input array and operator.
     */
    public ParallelPrefixProblem(int[] inputArray, BinaryOperator<Integer> operator) {
        this.inputArray = Arrays.copyOf(inputArray, inputArray.length);
        this.operator = operator;
    }

    /**
     * Returns the initial state with only the first position computed.
     */
    @Override
    public PrefixState getInitialState() {
        return new PrefixState(inputArray, operator);
    }
    
    /**
     * Returns true if any computed prefix value is incorrect.
     */
    @Override
    public boolean Forbidden(PrefixState state) {
        // Check all computed positions for correctness
        for (int i = 0; i < state.n; i++){
            // Only verify positions that are marked as computed
            if (state.computed[i]) {
                int correctValue = state.computeCorrectPrefix(i);

                if (state.prefixArray[i] != correctValue) {
                    return true; // Found an incorrect prefix value - state is forbidden
                }
            }
        }

        return false; // All computed values are correct - state is allowed
    }
    
    /**
     * Fixes incorrect prefix values using parallel processing across threads.
     */
    @Override
    public PrefixState Ensure(PrefixState state, int threadId, int totalThreads) {
        PrefixState currentState = state;

        for (int i = threadId; i < state.n; i += totalThreads) {
            // Only fix positions that are computed but incorrect
            if (currentState.computed[i]) {
                int correctValue = currentState.computeCorrectPrefix(i);

                if (currentState.prefixArray[i] != correctValue) {
                    // Fix this incorrect value
                    currentState = currentState.withComputed(i, correctValue);
                }
            }
        }

        return currentState;
    }
    
    /**
     * Computes new prefix values using stride-based parallel processing.
     */
    @Override
    public PrefixState Advance(PrefixState state, int threadId, int totalThreads) {
        PrefixState currentState = state;
        
        for (int i = threadId; i < state.n; i += totalThreads) {
            // Skip positions that are already computed
            if (!currentState.computed[i]) {
                // Try to compute this position if we have prerequisite information
                if (i == 0) {
                    // Position 0 should already be computed, but handle edge case
                    currentState = currentState.withComputed(0, state.inputArray[0]);
                } else if (currentState.computed[i - 1]) {
                    // Compute prefix[i] = prefix[i-1] ⊕ input[i]
                    int newValue = currentState.operator.apply(currentState.prefixArray[i - 1], state.inputArray[i]);
                    currentState = currentState.withComputed(i, newValue);
                }
            }
        }
        
        return currentState;
    }
    
    /**
     * Returns true if all prefix values are computed correctly.
     */
    @Override
    public boolean isSolution(PrefixState state) {
        return !Forbidden(state) && state.isComplete();
    }
    
    /**
     * Merges prefix computation results from different threads, combining their progress.
     */
    @Override
    public PrefixState merge(PrefixState state1, PrefixState state2) {
        int[] newPrefixArray = Arrays.copyOf(state1.prefixArray, state1.n);
        boolean[] newComputed = Arrays.copyOf(state1.computed, state1.n);
        
        // Merge computed values from state2
        for (int i = 0; i < state1.n; i++) {
            if (state2.computed[i]) {
                newPrefixArray[i] = state2.prefixArray[i];
                newComputed[i] = true;
            }
        }
        
        return new PrefixState(state1.inputArray, newPrefixArray, newComputed, state1.operator);
    }

    /**
     * Main method demonstrating the parallel prefix problem with test cases.
     */
    public static void main(String[] args) {
        System.out.println("=== Parallel Prefix Problem Example ===\n");
        
        // Test Case 1: Simple prefix sum
        testCase1();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 2: Prefix product
        testCase2();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 3: Larger array with prefix sum
        testCase3();
    }

    /**
     * Test case 1: Classic prefix sum with small array.
     */
    private static void testCase1() {
        System.out.println("Test Case 1: Prefix Sum [1, 2, 3, 4, 5]");
        System.out.println("---------------------------------------");
        
        int[] input = {1, 2, 3, 4, 5};
        BinaryOperator<Integer> sumOp = Integer::sum;
        
        printInputArray(input, "Addition");
        
        int numThreads = 4;
        int maxIterations = 100;
        
        ParallelPrefixProblem problem = new ParallelPrefixProblem(input, sumOp);
        PrefixState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected result: [1, 3, 6, 10, 15]");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Test case 2: Prefix product with different operator.
     */
    private static void testCase2() {
        System.out.println("Test Case 2: Prefix Product [2, 3, 4]");
        System.out.println("-------------------------------------");
        
        int[] input = {2, 3, 4};
        BinaryOperator<Integer> productOp = (a, b) -> a * b;
        
        printInputArray(input, "Multiplication");
        
        int numThreads = 3;
        int maxIterations = 100;
        
        ParallelPrefixProblem problem = new ParallelPrefixProblem(input, productOp);
        PrefixState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected result: [2, 6, 24]");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Test case 3: Larger array to test scalability.
     */
    private static void testCase3() {
        System.out.println("Test Case 3: Larger Prefix Sum [1, 1, 1, 1, 1, 1, 1, 1]");
        System.out.println("-------------------------------------------------------");
        
        int[] input = {1, 1, 1, 1, 1, 1, 1, 1};
        BinaryOperator<Integer> sumOp = Integer::sum;
        
        printInputArray(input, "Addition");
        
        int numThreads = 6;
        int maxIterations = 100;
        
        ParallelPrefixProblem problem = new ParallelPrefixProblem(input, sumOp);
        PrefixState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected result: [1, 2, 3, 4, 5, 6, 7, 8]");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Helper method to print input array and operator information.
     */
    private static void printInputArray(int[] input, String operatorName) {
        System.out.println("Input array: " + Arrays.toString(input));
        System.out.println("Operator: " + operatorName);
        System.out.println();
    }

    /**
     * Solves the parallel prefix problem using the LLP framework and prints results.
     */
    private static void solveProblem(ParallelPrefixProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<PrefixState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            PrefixState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\nSolution found!");
            printSolution(solution);
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            System.out.println("Is complete? " + solution.isComplete());
            
            // Verify correctness
            verifyCorrectness(solution);
            
            // Get statistics
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            if (stats != null) {
                System.out.println("Total iterations: " + stats.getIterationCount());
                System.out.println("Converged: " + stats.hasConverged());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prints the final prefix computation result.
     */
    private static void printSolution(PrefixState solution) {
        System.out.println("Final prefix array: " + Arrays.toString(solution.prefixArray));
        System.out.println("Computed positions: " + Arrays.toString(solution.computed));
    }

    /**
     * Verifies that the computed prefix values are correct.
     */
    private static void verifyCorrectness(PrefixState state) {
        System.out.println("Correctness verification:");
        boolean allCorrect = true;
        
        for (int i = 0; i < state.n; i++) {
            if (state.computed[i]) {
                int expected = state.computeCorrectPrefix(i);
                int actual = state.prefixArray[i];
                if (expected != actual) {
                    System.out.printf("Position %d: expected %d, got %d\n", i, expected, actual);
                    allCorrect = false;
                }
            }
        }
        
        if (allCorrect) {
            System.out.println("All computed prefix values are correct!");
        }
    }
}
