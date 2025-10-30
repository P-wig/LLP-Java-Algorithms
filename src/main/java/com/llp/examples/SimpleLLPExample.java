package com.llp.examples;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;

/**
 * Parallel Array Maximum Finding Example - Demonstrates TRUE parallel computation.
 * 
 * Problem: Find maximum element in each segment of an array in parallel
 * - Input: [15, 22, 8, 31, 9, 43, 7, 54, 6, 19, 3, 28]  (12 elements)
 * - With 4 threads: segment 0=[15,22,8], segment 1=[31,9,43], segment 2=[7,54,6], segment 3=[19,3,28]
 * - Output: [22, 43, 54, 28]  (max of each segment)
 * - Forbidden: any segment has incorrect maximum
 * - Advance: compute maximum for assigned segment (DIFFERENT threads work on DIFFERENT segments)
 * - Ensure: fix incorrect maximums
 */
public class SimpleLLPExample {
    
    /**
     * State for parallel maximum finding computation.
     */
    static class MaxFindingState {
        final int[] array;        // Original array (readonly)
        final int[] segmentMaxs;  // Maximum of each segment
        final boolean[] computed; // Which segments are processed
        final int segmentSize;    // Elements per segment
        
        public MaxFindingState(int[] array, int numSegments) {
            this.array = array.clone();
            this.segmentMaxs = new int[numSegments];
            this.computed = new boolean[numSegments];
            this.segmentSize = (int) Math.ceil((double) array.length / numSegments);
            
            // Initialize with minimum values
            Arrays.fill(segmentMaxs, Integer.MIN_VALUE);
        }
        
        public MaxFindingState(int[] array, int[] segmentMaxs, boolean[] computed, int segmentSize) {
            this.array = array.clone();
            this.segmentMaxs = segmentMaxs.clone();
            this.computed = computed.clone();
            this.segmentSize = segmentSize;
        }
        
        public MaxFindingState withSegmentMax(int segmentId, int max) {
            int[] newMaxs = segmentMaxs.clone();
            boolean[] newComputed = computed.clone();
            newMaxs[segmentId] = max;
            newComputed[segmentId] = true;
            return new MaxFindingState(array, newMaxs, newComputed, segmentSize);
        }
        
        /**
         * Merge this state with another state, combining computed segments.
         */
        public MaxFindingState mergeWith(MaxFindingState other) {
            int[] newMaxs = segmentMaxs.clone();
            boolean[] newComputed = computed.clone();
            
            // Copy computed segments from other state
            for (int i = 0; i < segmentMaxs.length; i++) {
                if (other.computed[i] && !computed[i]) {
                    newMaxs[i] = other.segmentMaxs[i];
                    newComputed[i] = true;
                } else if (other.computed[i] && computed[i]) {
                    // Both computed - prefer the one that's not minimum value
                    if (segmentMaxs[i] == Integer.MIN_VALUE && other.segmentMaxs[i] != Integer.MIN_VALUE) {
                        newMaxs[i] = other.segmentMaxs[i];
                    }
                }
            }
            
            return new MaxFindingState(array, newMaxs, newComputed, segmentSize);
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
     * Parallel maximum finding problem using LLP framework.
     */
    static class MaxFindingProblem implements LLPProblem<MaxFindingState> {
        
        @Override
        public boolean Forbidden(MaxFindingState state) {
            // Check if any computed segment maximum is incorrect
            for (int segmentId = 0; segmentId < state.segmentMaxs.length; segmentId++) {
                if (state.computed[segmentId]) {
                    int expectedMax = computeExpectedMax(state, segmentId);
                    if (state.segmentMaxs[segmentId] != expectedMax) {
                        return true; // Incorrect maximum
                    }
                }
            }
            return false;
        }
        
        @Override
        public MaxFindingState Ensure(MaxFindingState state) {
            // Fix any incorrect segment maximums
            MaxFindingState result = state;
            for (int segmentId = 0; segmentId < state.segmentMaxs.length; segmentId++) {
                if (state.computed[segmentId]) {
                    int expectedMax = computeExpectedMax(state, segmentId);
                    if (state.segmentMaxs[segmentId] != expectedMax) {
                        System.out.println("    Fixing segment[" + segmentId + "]: " + 
                                         state.segmentMaxs[segmentId] + " → " + expectedMax);
                        result = result.withSegmentMax(segmentId, expectedMax);
                    }
                }
            }
            return result;
        }
        
        @Override
        public MaxFindingState AdvanceWithContext(MaxFindingState state, int threadId, int totalThreads) {
            // Each thread works on specific segments - THIS IS THE KEY FOR TRUE PARALLELISM
            for (int segmentId = threadId; segmentId < state.segmentMaxs.length; segmentId += totalThreads) {
                if (!state.computed[segmentId]) {
                    int max = computeSegmentMax(state, segmentId);
                    
                    int start = state.getSegmentStart(segmentId);
                    int end = state.getSegmentEnd(segmentId);
                    System.out.println("    Thread-" + threadId + " processing segment[" + segmentId + "] " +
                                     "elements[" + start + ".." + (end-1) + "]: " + 
                                     getSegmentString(state, segmentId) + " → max=" + max);
                    
                    return state.withSegmentMax(segmentId, max);
                }
            }
            return state; // No work available for this thread
        }
        
        @Override
        public MaxFindingState Advance(MaxFindingState state) {
            // Fallback: process first uncomputed segment
            for (int segmentId = 0; segmentId < state.segmentMaxs.length; segmentId++) {
                if (!state.computed[segmentId]) {
                    int max = computeSegmentMax(state, segmentId);
                    return state.withSegmentMax(segmentId, max);
                }
            }
            return state;
        }
        
        /**
         * CRITICAL: Override the merge method to properly combine parallel results.
         */
        @Override
        public MaxFindingState merge(MaxFindingState state1, MaxFindingState state2) {
            // If one state has no computed segments, return the other
            boolean state1HasComputed = false;
            boolean state2HasComputed = false;
            
            for (boolean computed : state1.computed) {
                if (computed) { state1HasComputed = true; break; }
            }
            for (boolean computed : state2.computed) {
                if (computed) { state2HasComputed = true; break; }
            }
            
            if (!state1HasComputed) return state2;
            if (!state2HasComputed) return state1;
            
            // Both have computed segments - merge them
            return state1.mergeWith(state2);
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
         * Compute expected maximum for validation.
         */
        private int computeExpectedMax(MaxFindingState state, int segmentId) {
            return computeSegmentMax(state, segmentId);
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
            int[] testArray = {15, 22, 8, 31, 9, 43, 7, 54, 6, 19, 3, 28};
            int numSegments = 4; // Will create 4 segments of 3 elements each
            return new MaxFindingState(testArray, numSegments);
        }
        
        @Override
        public boolean isSolution(MaxFindingState state) {
            // All segments must be computed
            for (boolean c : state.computed) {
                if (!c) return false;
            }
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Array Maximum Finding Example ===\n");
        
        // Problem parameters
        int numThreads = 4;  // Match number of segments for clear demonstration
        int maxIterations = 100;
        
        // Create the problem
        MaxFindingProblem problem = new MaxFindingProblem();
        MaxFindingState initial = problem.getInitialState();
        
        System.out.println("Problem: Find maximum in each array segment using parallel processing");
        System.out.println("Input array: " + Arrays.toString(initial.array));
        System.out.println("Segments (" + initial.segmentMaxs.length + "):");
        for (int i = 0; i < initial.segmentMaxs.length; i++) {
            int start = initial.getSegmentStart(i);
            int end = initial.getSegmentEnd(i);
            int[] segment = Arrays.copyOfRange(initial.array, start, end);
            System.out.println("  Segment " + i + ": " + Arrays.toString(segment));
        }
        System.out.println("Expected maximums: [22, 43, 54, 28]");
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        
        // Solve using the LLP framework
        solveProblem(problem, numThreads, maxIterations);
    }

    private static void solveProblem(MaxFindingProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<MaxFindingState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            MaxFindingState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found!");
            System.out.println("Input array:      " + Arrays.toString(solution.array));
            System.out.println("Segment maximums: " + Arrays.toString(solution.segmentMaxs));
            System.out.println("Computed:         " + Arrays.toString(solution.computed));
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            
            // Verify correctness
            int[] expected = {22, 43, 54, 28};
            boolean correct = Arrays.equals(solution.segmentMaxs, expected);
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
