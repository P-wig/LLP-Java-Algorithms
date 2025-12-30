package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import java.util.Arrays;

/**
 * Simplified Floyd-Warshall All-Pairs Shortest Path Algorithm using the LLP framework.
 * 
 * Problem Description:
 * Floyd-Warshall algorithm finds shortest paths between all pairs of vertices
 * in a weighted directed graph. It can handle negative edge weights but not
 * negative cycles.
 * 
 * LLP Implementation Strategy:
 * - State: Distance matrix representing shortest path estimates
 * - Forbidden: Distance matrix can still be improved via triangle inequality
 * - Advance: Update distances using intermediate vertices (NO MERGE NEEDED)
 * - Parallelism: Different threads can process different vertex pairs simultaneously
 */
public class JohnsonProblem {
    
    /**
     * Represents a weighted directed edge.
     */
    public static class Edge {
        public final int from, to;
        public final double weight;
        
        public Edge(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
        
        @Override
        public String toString() {
            return String.format("%d->%d(%.1f)", from, to, weight);
        }
    }

    /**
     * Simplified state for Floyd-Warshall algorithm.
     * Uses thread-safe operations for parallel updates.
     */
    static class FloydWarshallState {
        final int numVertices;                    // Number of vertices
        final Edge[] edges;                       // Graph edges (readonly)
        volatile double[][] distances;           // All-pairs distance matrix
        volatile int currentK;                    // Current intermediate vertex
        volatile int iterations;                  // Iteration counter
        volatile int threadsCompleted;            // Completed threads counter
        final Object lock = new Object();        // Synchronization lock
        
        public FloydWarshallState(int numVertices, Edge[] edges) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
            this.distances = new double[numVertices][numVertices];
            this.currentK = 0;
            this.iterations = 0;
            this.threadsCompleted = 0;
            
            // Initialize distance matrix
            for (int i = 0; i < numVertices; i++) {
                Arrays.fill(distances[i], Double.POSITIVE_INFINITY);
                distances[i][i] = 0.0; // Distance to self is zero
            }
            
            // Add direct edges
            for (Edge edge : edges) {
                distances[edge.from][edge.to] = edge.weight;
            }
        }
        
        /**
         * Thread-safe method to update distance.
         */
        public synchronized boolean updateDistance(int from, int to, double newDistance) {
            if (newDistance < distances[from][to] - 1e-10) {
                distances[from][to] = newDistance;
                return true;
            }
            return false;
        }
        
        /**
         * Thread-safe method to update current intermediate vertex.
         */
        public synchronized void incrementK() {
            currentK++;
        }
        
        /**
         * Thread-safe method to increment iteration counter.
         */
        public synchronized void incrementIterations() {
            iterations++;
        }
        
        /**
         * Get the current iteration count.
         */
        public int getIterations() {
            return iterations;
        }
        
        @Override
        public String toString() {
            return String.format("FloydWarshall{vertices=%d, k=%d}", numVertices, currentK);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FloydWarshallState)) return false;
            FloydWarshallState other = (FloydWarshallState) obj;
            return numVertices == other.numVertices && currentK == other.currentK;
        }
        
        @Override
        public int hashCode() {
            return numVertices * 31 + currentK;
        }
    }

    /**
     * Floyd-Warshall algorithm using simplified LLP framework.
     */
    static class FloydWarshallLLPProblem implements LLPProblem<FloydWarshallState> {
        
        private final int numVertices;
        private final Edge[] edges;
        
        public FloydWarshallLLPProblem(int numVertices, Edge[] edges) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
        }
        
        @Override
        public boolean Forbidden(FloydWarshallState state) {
            // Check if we can still make improvements to the distance matrix
            
            // If we've processed all k values, no more improvements possible
            if (state.currentK >= state.numVertices) {
                return false; // No forbidden state - algorithm is done
            }
            
            // Check if any distance can be improved using current k value
            int k = state.currentK;
            for (int i = 0; i < state.numVertices; i++) {
                for (int j = 0; j < state.numVertices; j++) {
                    if (Double.isFinite(state.distances[i][k]) && Double.isFinite(state.distances[k][j])) {
                        double throughK = state.distances[i][k] + state.distances[k][j];
                        if (throughK < state.distances[i][j] - 1e-10) {
                            return true; // Found an improvement - state is forbidden (needs fixing)
                        }
                    }
                }
            }
            
            return false; // No improvements found for current k - not forbidden
        }

        @Override
        public FloydWarshallState Advance(FloydWarshallState state, int threadId, int totalThreads) {
            // Increment iterations (Thread-0 only)
            if (threadId == 0) {
                state.incrementIterations();
            }
            
            int k = state.currentK;
            
            // Check if we're done
            if (k >= state.numVertices) {
                return state;
            }
            
            // ALL threads process their assigned work for current k
            for (int i = threadId; i < state.numVertices; i += totalThreads) {
                for (int j = 0; j < state.numVertices; j++) {
                    if (Double.isFinite(state.distances[i][k]) && Double.isFinite(state.distances[k][j])) {
                        double newDistance = state.distances[i][k] + state.distances[k][j];
                        state.updateDistance(i, j, newDistance);
                    }
                }
            }
            
            // Barrier: all threads must reach here before k advances
            synchronized (state.lock) {
                // Increment thread completion counter
                state.threadsCompleted++;
                
                // If all threads completed current k, advance to next k
                if (state.threadsCompleted == totalThreads) {
                    state.incrementK();
                    state.threadsCompleted = 0; // Reset for next iteration
                }
            }
            
            return state;
        }
        
        @Override
        public FloydWarshallState getInitialState() {
            return new FloydWarshallState(numVertices, edges);
        }
        
        @Override
        public boolean isSolution(FloydWarshallState state) {
            // Solution when algorithm is complete (all k processed) AND no forbidden improvements
            return state.currentK >= state.numVertices && !Forbidden(state);
        }
    }
    
    /**
     * Main method to demonstrate Floyd-Warshall algorithm using LLP framework.
     */
    public static void main(String[] args) {
        System.out.println("Running Floyd-Warshall All-Pairs Shortest Path Algorithm using LLP Framework\n");
        
        // Create a test graph
        int numVertices = 5;
        Edge[] edges = {
            new Edge(0, 1, 3.0),
            new Edge(0, 2, 8.0),
            new Edge(0, 4, -4.0),
            new Edge(1, 3, 1.0),
            new Edge(1, 4, 7.0),
            new Edge(2, 1, 4.0),
            new Edge(3, 0, 2.0),
            new Edge(3, 2, -5.0),
            new Edge(4, 3, 6.0)
        };
        
        System.out.println("Graph with " + numVertices + " vertices and " + edges.length + " edges:");
        for (Edge edge : edges) {
            System.out.println("  " + edge);
        }
        System.out.println();
        
        // Test different thread counts
        int[] threadCounts = {1, 2, 4, 8};
        long[] times = new long[threadCounts.length];
        int[] iterations = new int[threadCounts.length];
        
        FloydWarshallState solution = null;
        
        for (int i = 0; i < threadCounts.length; i++) {
            int threads = threadCounts[i];
            System.out.println("Running with " + threads + " thread(s):");
            
            FloydWarshallLLPProblem problem = new FloydWarshallLLPProblem(numVertices, edges);
            LLPSolver<FloydWarshallState> solver = new LLPSolver<>(problem, threads, 100);
            
            long startTime = System.nanoTime();
            FloydWarshallState result = solver.solve();
            long endTime = System.nanoTime();
            
            times[i] = endTime - startTime;
            iterations[i] = result.getIterations();
            
            System.out.println("  Time: " + (times[i] / 1_000_000) + " ms");
            System.out.println("  Iterations: " + iterations[i]);
            
            if (solution == null) {
                solution = result;
                System.out.println("\n  Final distance matrix:");
                printDistanceMatrix(solution.distances);
            }
            
            System.out.println();
            solver.shutdown();
        }
        
        // Analyze performance scaling
        System.out.println("Performance Analysis:");
        System.out.println("Threads\tTime(ms)\tIterations\tSpeedup");
        for (int i = 0; i < threadCounts.length; i++) {
            double speedup = (i == 0) ? 1.0 : (double) times[0] / times[i];
            System.out.printf("%d\t%d\t\t%d\t\t%.2fx\n", 
                threadCounts[i], times[i] / 1_000_000, iterations[i], speedup);
        }
    }
    
    private static void printDistanceMatrix(double[][] distances) {
        int n = distances.length;
        
        System.out.print("     ");
        for (int j = 0; j < n; j++) {
            System.out.printf("%8s ", "v" + j);
        }
        System.out.println();
        
        for (int i = 0; i < n; i++) {
            System.out.printf("v%-2d: ", i);
            for (int j = 0; j < n; j++) {
                if (distances[i][j] == Double.POSITIVE_INFINITY) {
                    System.out.printf("%8s ", "∞");
                } else {
                    System.out.printf("%8.1f ", distances[i][j]);
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}