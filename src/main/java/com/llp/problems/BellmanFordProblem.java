package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;

/**
 * Bellman-Ford Single-Source Shortest Path Algorithm using the LLP framework.
 * 
 * Problem Description:
 * The Bellman-Ford algorithm finds shortest paths from a source vertex to all
 * other vertices in a weighted directed graph. It can handle negative edge weights
 * (unlike Dijkstra's algorithm) but the graph must not contain negative cycles.
 * 
 * LLP Implementation Strategy:
 * - State: Distance estimates from source to each vertex
 * - Forbidden: Distance estimates that violate triangle inequality
 * - Advance: Relax edges to improve distance estimates (NO MERGE NEEDED)
 * - Parallelism: Multiple edges can be relaxed simultaneously on shared state
 */
public class BellmanFordProblem {
    
    /**
     * Represents a weighted directed edge in the graph.
     */
    static class Edge {
        final int from;
        final int to;
        final double weight;
        
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
     * State representation for Bellman-Ford algorithm.
     * Uses simple synchronized methods - often faster than complex lock-free code.
     */
    static class BellmanFordState {
        final Edge[] edges;           // Graph edges (readonly)
        volatile double[] distances;  // Current shortest distances
        volatile boolean[] updated;   // Which vertices were updated
        final int source;             // Source vertex
        final int numVertices;        // Number of vertices
        
        public BellmanFordState(int numVertices, Edge[] edges, int source) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
            this.source = source;
            this.distances = new double[numVertices];
            this.updated = new boolean[numVertices];
            
            // Initialize distances: source=0, others=infinity
            Arrays.fill(distances, Double.POSITIVE_INFINITY);
            distances[source] = 0.0;
            Arrays.fill(updated, false);
        }
        
        /**
         * Simple synchronized update - often faster than lock-free for this workload.
         */
        public synchronized boolean updateDistance(int vertex, double newDistance) {
            if (newDistance < distances[vertex] - 1e-10) {
                distances[vertex] = newDistance;
                updated[vertex] = true;
                return true;
            }
            return false;
        }
        
        public double getDistance(int vertex) {
            return distances[vertex];  // Safe to read volatile array
        }
        
        public double[] getDistances() {
            return distances.clone();
        }
        
        @Override
        public String toString() {
            return String.format("BellmanFord{source=%d, distances=%s, updated=%s}", 
                               source, Arrays.toString(distances), Arrays.toString(updated));
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BellmanFordState)) return false;
            BellmanFordState other = (BellmanFordState) obj;
            return source == other.source &&
                   numVertices == other.numVertices &&
                   Arrays.equals(distances, other.distances) &&
                   Arrays.equals(updated, other.updated);
        }
    }

    /**
     * Bellman-Ford problem implementation using simplified LLP framework.
     */
    static class BellmanFordLLPProblem implements LLPProblem<BellmanFordState> {
        
        private final int numVertices;
        private final Edge[] edges;
        private final int source;
        private static final double EPSILON = 1e-10;
        
        public BellmanFordLLPProblem(int numVertices, Edge[] edges, int source) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
            this.source = source;
        }
        
        @Override
        public boolean Forbidden(BellmanFordState state) {
            // Same logic - check if any edge can be relaxed
            for (Edge edge : state.edges) {
                if (Double.isFinite(state.distances[edge.from])) {
                    double newDistance = state.distances[edge.from] + edge.weight;
                    if (state.distances[edge.to] > newDistance + EPSILON) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        @Override
        public BellmanFordState Advance(BellmanFordState state, int threadId, int totalThreads) {
            // SYNCHRONIZED iteration-level updates to maintain algorithm correctness
            
            if (threadId == 0) {
                // Only Thread-0 does the work to maintain deterministic behavior
                boolean anyUpdate = false;
                
                for (Edge edge : state.edges) {
                    if (Double.isFinite(state.distances[edge.from])) {
                        double newDistance = state.distances[edge.from] + edge.weight;
                        if (newDistance < state.distances[edge.to] - EPSILON) {
                            state.updateDistance(edge.to, newDistance);
                            anyUpdate = true;
                        }
                    }
                }
                
                // Store whether any update was made
                state.updated[0] = anyUpdate;
            }
            
            // All threads wait for Thread-0 to complete the iteration
            // This ensures deterministic behavior
            
            return state;
        }
        
        @Override
        public BellmanFordState getInitialState() {
            return new BellmanFordState(numVertices, edges, source);
        }
        
        @Override
        public boolean isSolution(BellmanFordState state) {
            // Solution when no more improvements possible
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Bellman-Ford Shortest Path Example ===\n");
        
        // Create a larger, more complex graph for scalability testing
        int numVertices = 1000;  // Much larger graph
        int source = 0;
        Edge[] edges = generateLargeGraph(numVertices);
        
        int[] threadCounts = {1, 2, 4, 8, 16};
        int maxIterations = 1000;  // More iterations needed for larger graph
        
        // Create the problem
        BellmanFordLLPProblem problem = new BellmanFordLLPProblem(numVertices, edges, source);
        
        System.out.println("Problem: Find shortest paths from source vertex " + source);
        System.out.println("Graph: " + numVertices + " vertices, " + edges.length + " edges");
        System.out.println("Average edges per vertex: " + (double)edges.length / numVertices);
        
        // Test different thread counts
        for (int numThreads : threadCounts) {
            solveProblem(problem, numThreads, maxIterations);
        }
    }

    /**
     * Generate a large, connected graph where all vertices are reachable from source 0.
     */
    private static Edge[] generateLargeGraph(int numVertices) {
        java.util.List<Edge> edgeList = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random(42);
        
        // Create a spanning tree (no cycles possible)
        for (int i = 1; i < numVertices; i++) {
            int parent = random.nextInt(i);
            double weight = 1.0 + random.nextDouble() * 3.0; // Only positive weights
            edgeList.add(new Edge(parent, i, weight));
        }
        
        // Add additional edges with CAREFUL negative weights
        for (int i = 0; i < numVertices; i++) {
            for (int j = 0; j < 2; j++) { // 2 extra edges per vertex
                int to = random.nextInt(numVertices);
                if (i != to) {
                    // Mostly positive weights, few small negatives
                    double weight = random.nextDouble() < 0.8 ? 
                        (1.0 + random.nextDouble() * 3.0) :     // 80% positive
                        (-0.1 - random.nextDouble() * 0.4);     // 20% small negative
                    edgeList.add(new Edge(i, to, weight));
                }
            }
        }
        
        return edgeList.toArray(new Edge[0]);
    }

    private static void solveProblem(BellmanFordLLPProblem problem, int numThreads, int maxIterations) {
        LLPSolver<BellmanFordState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.nanoTime();
            BellmanFordState solution = solver.solve();
            long endTime = System.nanoTime();
            
            // Show results in compact format
            double timeMs = (endTime - startTime) / 1_000_000.0;
            int iterations = solver.getExecutionStats().getIterationCount();
            boolean valid = problem.isSolution(solution);
            
            System.out.printf("Threads: %2d | Time: %8.2fms | Iterations: %3d | Valid: %s", 
                             numThreads, timeMs, iterations, valid);
            
            // Show speedup relative to single thread
            if (numThreads == 1) {
                System.out.println(" | Speedup: 1.00x (baseline)");
                // Store baseline for comparison
                baselineTime = timeMs;
            } else {
                double speedup = baselineTime / timeMs;
                System.out.printf(" | Speedup: %.2fx\n", speedup);
            }
            
            // Show sample distances for first run only (to verify correctness)
            if (numThreads == 1) {
                System.out.println("\nSample shortest distances:");
                double[] distances = solution.getDistances();
                for (int i = 0; i < Math.min(10, distances.length); i++) {
                    System.out.printf("  Vertex %d: %.3f\n", i, distances[i]);
                }
                System.out.println("  ...");
                
                // CORRECTED: Check for negative cycles
                boolean hasNegativeCycle = false;
                for (double dist : distances) {
                    if (Double.isInfinite(dist) || Double.isNaN(dist)) {
                        hasNegativeCycle = true;
                        break;
                    }
                }
                
                if (hasNegativeCycle) {
                    System.err.println("ERROR: Detected invalid distance (infinite or NaN) - possible negative cycle!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }
    
    // Baseline time for speedup comparison (single thread)
    private static double baselineTime = 0.0;
}
