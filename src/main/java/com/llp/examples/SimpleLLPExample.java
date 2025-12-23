package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;
import java.util.Random;

/**
 * Parallel Array Maximum Finding Example - Demonstrates TRUE parallel computation.
 * 
 * Problem: Find maximum element in each segment of an array in parallel
 * - Input: [15, 22, 8, 31, 9, 43, 7, 54, 6, 19, 3, 28]  (12 elements)
 * - With 4 threads: segment 0=[15,22,8], segment 1=[31,9,43], segment 2=[7,54,6], segment 3=[19,3,28]
 * - Output: [22, 43, 54, 28]  (max of each segment)
 * - Forbidden: any segment has uncomputed or incorrect maximum
 * - Advance: compute maximum for assigned segment (DIFFERENT threads work on DIFFERENT segments)
 */
public class SimpleLLPExample {
    
    /**
     * State for parallel maximum finding computation.
     * Uses thread-safe operations for parallel updates.
     */
    static class MaxFindingState {
        final int[] array;        // Original array (readonly)
        volatile int[] segmentMaxs;  // Add volatile for thread safety
        volatile boolean[] computed; // Add volatile for thread safety
        final int segmentSize;    // Elements per segment
        
        public MaxFindingState(int[] array, int numSegments) {
            this.array = array.clone();
            this.segmentMaxs = new int[numSegments];
            this.computed = new boolean[numSegments];
            this.segmentSize = (int) Math.ceil((double) array.length / numSegments);
            
            // Initialize with minimum values
            Arrays.fill(segmentMaxs, Integer.MIN_VALUE);
            Arrays.fill(computed, false);
        }
        
        private MaxFindingState(int[] array, int[] segmentMaxs, boolean[] computed, int segmentSize) {
            this.array = array.clone();
            this.segmentMaxs = segmentMaxs.clone();
            this.computed = computed.clone();
            this.segmentSize = segmentSize;
        }
        
        /**
         * Thread-safe update of segment maximum.
         * No need to return new state - modify in place!
         */
        public synchronized void setSegmentMax(int segmentId, int max) {
            segmentMaxs[segmentId] = max;
            computed[segmentId] = true;
        }
        
        /**
         * Get the start index for a segment.
         */
        public int getSegmentStart(int segmentId) {
            return segmentId * segmentSize;
        }
        
        /**
         * Get the end index (exclusive) for a segment.
         */
        public int getSegmentEnd(int segmentId) {
            return Math.min((segmentId + 1) * segmentSize, array.length);
        }
        
        @Override
        public String toString() {
            return String.format("MaxFinding{array=%s, segmentMaxs=%s, computed=%s, segmentSize=%d}", 
                               Arrays.toString(array), 
                               Arrays.toString(segmentMaxs), 
                               Arrays.toString(computed),
                               segmentSize);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MaxFindingState)) return false;
            MaxFindingState other = (MaxFindingState) obj;
            return segmentSize == other.segmentSize &&
                   Arrays.equals(array, other.array) && 
                   Arrays.equals(segmentMaxs, other.segmentMaxs) && 
                   Arrays.equals(computed, other.computed);
        }
    }

    /**
     * Parallel maximum finding problem using simplified LLP framework.
     */
    static class MaxFindingProblem implements LLPProblem<MaxFindingState> {
        
        @Override
        public boolean Forbidden(MaxFindingState state) {
            // State is forbidden if:
            // 1. Any segment is not computed yet, OR
            // 2. Any computed segment maximum is incorrect
            
            for (int segmentId = 0; segmentId < state.segmentMaxs.length; segmentId++) {
                if (!state.computed[segmentId]) {
                    return true; // Uncomputed segment
                }
                
                // Check if computed maximum is correct
                int expectedMax = computeSegmentMax(state, segmentId);
                if (state.segmentMaxs[segmentId] != expectedMax) {
                    return true; // Incorrect maximum
                }
            }
            return false;
        }
        
        @Override
        public MaxFindingState Advance(MaxFindingState state, int threadId, int totalThreads) {
            // Each thread works on specific segments - modifies the SAME state object
            
            for (int segmentId = threadId; segmentId < state.segmentMaxs.length; segmentId += totalThreads) {
                // Fix this segment if it's uncomputed or incorrect
                if (!state.computed[segmentId]) {
                    // Compute maximum for uncomputed segment
                    int max = computeSegmentMax(state, segmentId);
                    
                    // REMOVE ALL PRINTING - it's killing performance!
                    // Just do the work silently
                    state.setSegmentMax(segmentId, max);
                    
                } else {
                    // Check if computed maximum is correct and fix if needed
                    int expectedMax = computeSegmentMax(state, segmentId);
                    if (state.segmentMaxs[segmentId] != expectedMax) {
                        state.setSegmentMax(segmentId, expectedMax);
                    }
                }
            }
            
            return state;
        }
        
        /**
         * Compute maximum for a specific segment.
         */
        private int computeSegmentMax(MaxFindingState state, int segmentId) {
            int start = state.getSegmentStart(segmentId);
            int end = state.getSegmentEnd(segmentId);
            int max = Integer.MIN_VALUE;
            
            for (int i = start; i < end; i++) {
                if (state.array[i] > max) {
                    max = state.array[i];
                }
            }
            return max;
        }
        
        /**
         * Get string representation of a segment's elements.
         */
        private String getSegmentString(MaxFindingState state, int segmentId) {
            int start = state.getSegmentStart(segmentId);
            int end = state.getSegmentEnd(segmentId);
            StringBuilder sb = new StringBuilder("[");
            for (int i = start; i < end; i++) {
                if (i > start) sb.append(",");
                sb.append(state.array[i]);
            }
            sb.append("]");
            return sb.toString();
        }
        
        @Override
        public MaxFindingState getInitialState() {
            // Much larger test case
            int[] testArray = new int[1000000];  // 1 million elements
            Random random = new Random(42);  // Fixed seed for reproducibility
            for (int i = 0; i < testArray.length; i++) {
                testArray[i] = random.nextInt(1000000);
            }
            
            int numSegments = 100; // 100 segments of 10,000 elements each
            return new MaxFindingState(testArray, numSegments);
        }
        
        @Override
        public boolean isSolution(MaxFindingState state) {
            // Solution when state is not forbidden
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Array Maximum Finding Example ===\n");
        
        // Problem parameters
        int[] threadCounts = {1, 2, 4, 8, 16};
        int maxIterations = 100;
        
        // Create the problem
        MaxFindingProblem problem = new MaxFindingProblem();
        MaxFindingState initial = problem.getInitialState();
        
        System.out.println("Problem: Find maximum in each array segment using parallel processing");
        System.out.println("Array size: " + initial.array.length + " elements");
        System.out.println("Segments: " + initial.segmentMaxs.length);
        System.out.println("Elements per segment: ~" + initial.segmentSize);
        
        // Test different thread counts
        for (int numThreads : threadCounts) {
            solveProblem(problem, numThreads, maxIterations);
        }
    }

    private static void solveProblem(MaxFindingProblem problem, int numThreads, int maxIterations) {
        LLPSolver<MaxFindingState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.nanoTime(); // Use nanoTime for better precision
            MaxFindingState solution = solver.solve();
            long endTime = System.nanoTime();
            
            // Only show essential performance info
            System.out.printf("Threads: %2d | Time: %6.2fms | Iterations: %d | Valid: %s\n", 
                             numThreads, 
                             (endTime - startTime) / 1_000_000.0, // Convert to ms with decimals
                             solver.getExecutionStats().getIterationCount(),
                             problem.isSolution(solution));
            
        } catch (Exception e) {
            System.err.println("Error with " + numThreads + " threads: " + e.getMessage());
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }
}
