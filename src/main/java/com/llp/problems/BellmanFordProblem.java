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
 * - Advance: Relax edges to improve distance estimates
 * - Ensure: Fix triangle inequality violations
 * - Parallelism: Multiple edges can be relaxed simultaneously
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
     */
    static class BellmanFordState {
        final Edge[] edges;           // Graph edges (readonly)
        final double[] distances;     // Current shortest distances
        final boolean[] updated;      // Which vertices were updated this iteration
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
        
        public BellmanFordState(Edge[] edges, double[] distances, boolean[] updated, int source, int numVertices) {
            this.edges = edges.clone();
            this.distances = distances.clone();
            this.updated = updated.clone();
            this.source = source;
            this.numVertices = numVertices;
        }
        
        public BellmanFordState withDistance(int vertex, double distance) {
            double[] newDistances = distances.clone();
            boolean[] newUpdated = updated.clone();
            newDistances[vertex] = distance;
            newUpdated[vertex] = true;
            return new BellmanFordState(edges, newDistances, newUpdated, source, numVertices);
        }
        
        public BellmanFordState withMultipleDistances(int[] vertices, double[] newDistances) {
            double[] distances = this.distances.clone();
            boolean[] updated = this.updated.clone();
            
            for (int i = 0; i < vertices.length; i++) {
                distances[vertices[i]] = newDistances[i];
                updated[vertices[i]] = true;
            }
            
            return new BellmanFordState(edges, distances, updated, source, numVertices);
        }
        
        /**
         * Merge this state with another state, taking the minimum distances.
         */
        public BellmanFordState mergeWith(BellmanFordState other) {
            double[] newDistances = distances.clone();
            boolean[] newUpdated = updated.clone();
            
            for (int i = 0; i < numVertices; i++) {
                if (other.distances[i] < newDistances[i]) {
                    newDistances[i] = other.distances[i];
                    newUpdated[i] = true;
                }
                if (other.updated[i]) {
                    newUpdated[i] = true;
                }
            }
            
            return new BellmanFordState(edges, newDistances, newUpdated, source, numVertices);
        }
        
        public double getDistance(int vertex) {
            return distances[vertex];
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
     * Bellman-Ford problem implementation using unified LLP framework.
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
            // Check if distance estimates violate triangle inequality
            for (Edge edge : state.edges) {
                if (Double.isFinite(state.distances[edge.from])) {
                    double newDistance = state.distances[edge.from] + edge.weight;
                    if (state.distances[edge.to] > newDistance + EPSILON) {
                        return true; // Triangle inequality violation
                    }
                }
            }
            return false;
        }
        
        @Override
        public BellmanFordState Ensure(BellmanFordState state, int threadId, int totalThreads) {
            // Each thread fixes violations for specific edges - distributed work
            java.util.List<Integer> updatedVertices = new java.util.ArrayList<>();
            java.util.List<Double> updatedDistances = new java.util.ArrayList<>();
            
            // Distribute edges among threads using modulo arithmetic
            for (int edgeIndex = threadId; edgeIndex < state.edges.length; edgeIndex += totalThreads) {
                Edge edge = state.edges[edgeIndex];
                
                if (Double.isFinite(state.distances[edge.from])) {
                    double newDistance = state.distances[edge.from] + edge.weight;
                    if (state.distances[edge.to] > newDistance + EPSILON) {
                        System.out.println("    Thread-" + threadId + " fixing vertex " + edge.to + ": " + 
                                         state.distances[edge.to] + " -> " + newDistance + 
                                         " via edge " + edge);
                        updatedVertices.add(edge.to);
                        updatedDistances.add(newDistance);
                    }
                }
            }
            
            if (!updatedVertices.isEmpty()) {
                int[] vertices = updatedVertices.stream().mapToInt(i -> i).toArray();
                double[] distances = updatedDistances.stream().mapToDouble(d -> d).toArray();
                return state.withMultipleDistances(vertices, distances);
            }
            
            return state; // No fixes made by this thread
        }
        
        @Override
        public BellmanFordState Advance(BellmanFordState state, int threadId, int totalThreads) {
            // Each thread works on specific edges - TRUE PARALLELISM
            java.util.List<Integer> updatedVertices = new java.util.ArrayList<>();
            java.util.List<Double> updatedDistances = new java.util.ArrayList<>();
            
            // Distribute edges among threads using modulo arithmetic
            // Thread 0 gets: 0, 3, 6, 9...
            // Thread 1 gets: 1, 4, 7, 10...
            // Thread 2 gets: 2, 5, 8, 11...
            for (int edgeIndex = threadId; edgeIndex < state.edges.length; edgeIndex += totalThreads) {
                Edge edge = state.edges[edgeIndex];
                
                if (Double.isFinite(state.distances[edge.from])) {
                    double newDistance = state.distances[edge.from] + edge.weight;
                    if (newDistance < state.distances[edge.to] - EPSILON) {
                        System.out.println("    Thread-" + threadId + " relaxing edge " + edge + 
                                         ": " + state.distances[edge.to] + " -> " + newDistance);
                        updatedVertices.add(edge.to);
                        updatedDistances.add(newDistance);
                    }
                }
            }
            
            if (!updatedVertices.isEmpty()) {
                int[] vertices = updatedVertices.stream().mapToInt(i -> i).toArray();
                double[] distances = updatedDistances.stream().mapToDouble(d -> d).toArray(); // FIX: was updatedVertices
                return state.withMultipleDistances(vertices, distances);
            }
            
            return state; // No improvements made by this thread
        }
        
        @Override
        public BellmanFordState merge(BellmanFordState state1, BellmanFordState state2) {
            // Check if either state has no updates
            boolean state1HasUpdates = false;
            boolean state2HasUpdates = false;
            
            for (boolean updated : state1.updated) {
                if (updated) { state1HasUpdates = true; break; }
            }
            for (boolean updated : state2.updated) {
                if (updated) { state2HasUpdates = true; break; }
            }
            
            if (!state1HasUpdates) return state2;
            if (!state2HasUpdates) return state1;
            
            // Both have updates - merge by taking minimum distances
            return state1.mergeWith(state2);
        }
        
        @Override
        public BellmanFordState getInitialState() {
            return new BellmanFordState(numVertices, edges, source);
        }
        
        @Override
        public boolean isSolution(BellmanFordState state) {
            // Solution when no constraint violations exist
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Bellman-Ford Shortest Path Example ===\n");
        
        // Example graph with 8 vertices and 15 edges
        Edge[] edges = {
            // From vertex 0 (source)
            new Edge(0, 1, 4.0),
            new Edge(0, 2, 2.0),
            new Edge(0, 3, 7.0),
            
            // From vertex 1
            new Edge(1, 2, -3.0),   // Negative edge
            new Edge(1, 4, 2.0),
            new Edge(1, 5, 4.0),
            
            // From vertex 2
            new Edge(2, 3, 1.0),
            new Edge(2, 4, 5.0),
            new Edge(2, 6, 3.0),
            
            // From vertex 3
            new Edge(3, 6, -2.0),   // Negative edge
            new Edge(3, 7, 1.0),
            
            // From vertex 4
            new Edge(4, 5, -1.0),   // Negative edge
            new Edge(4, 7, 3.0),
            
            // From vertex 5
            new Edge(5, 7, 2.0),
            
            // From vertex 6
            new Edge(6, 7, 4.0)
        };

        int numVertices = 8;
        int source = 0;
        int numThreads = 2;
        int maxIterations = 100;
        
        // Create the problem
        BellmanFordLLPProblem problem = new BellmanFordLLPProblem(numVertices, edges, source);
        BellmanFordState initial = problem.getInitialState();
        
        System.out.println("Problem: Find shortest paths from source vertex " + source);
        System.out.println("Graph edges:");
        for (Edge edge : edges) {
            System.out.println("  " + edge);
        }
        System.out.println("Expected distances from vertex 0: [0.0, 4.0, 1.0, 2.0, 6.0, 5.0, 0.0, 3.0]");
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        
        // Solve using the LLP framework
        solveProblem(problem, numThreads, maxIterations);
    }

    private static void solveProblem(BellmanFordLLPProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<BellmanFordState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            BellmanFordState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found!");
            System.out.println("Shortest distances: " + Arrays.toString(solution.getDistances()));
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            
            // Verify correctness
            double[] expected = {0.0, 4.0, 1.0, 2.0, 6.0, 5.0, 0.0, 3.0};
            boolean correct = Arrays.equals(solution.getDistances(), expected);
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
