package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.*;

/**
 * Connected Components Problem using the simplified LLP framework.
 * 
 * Problem Description:
 * Find all connected components in an undirected graph. A connected component
 * is a maximal set of vertices such that there is a path between every pair
 * of vertices within the set.
 * 
 * LLP Implementation Strategy:
 * - State: Component labels for each vertex using Union-Find
 * - Forbidden: Any edge connects vertices with different labels
 * - Advance: Propagate minimum labels along edges (NO MERGE NEEDED)
 * - Parallelism: Different threads can process different edges simultaneously
 */
public class ConnectedComponentsProblem {
    
    /**
     * Represents an undirected edge in the graph.
     */
    static class Edge {
        final int u, v;
        
        public Edge(int u, int v) {
            this.u = u;
            this.v = v;
        }
        
        @Override
        public String toString() {
            return String.format("(%d-%d)", u, v);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) return false;
            Edge other = (Edge) obj;
            return (u == other.u && v == other.v) || (u == other.v && v == other.u);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(Math.min(u, v), Math.max(u, v));
        }
    }

    /**
     * Simplified state for Connected Components algorithm.
     * Uses Union-Find with thread-safe operations.
     */
    static class ComponentState {
        final Edge[] edges;           // Graph edges (readonly)
        volatile int[] parent;        // Union-Find parent array - thread-safe
        volatile int[] rank;          // Union-Find rank array for optimization
        final int numVertices;        // Number of vertices
        final Object lock = new Object(); // Synchronization lock
        
        public ComponentState(int numVertices, Edge[] edges) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
            this.parent = new int[numVertices];
            this.rank = new int[numVertices];
            
            // Initialize Union-Find: each vertex is its own parent
            for (int i = 0; i < numVertices; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }
        
        /**
         * Thread-safe find with path compression.
         */
        public int findRoot(int vertex) {
            if (vertex >= parent.length) return vertex;
            
            // Path compression
            if (parent[vertex] != vertex) {
                parent[vertex] = findRoot(parent[vertex]);
            }
            return parent[vertex];
        }
        
        /**
         * Thread-safe union of two components with rank optimization.
         */
        public synchronized boolean union(int u, int v) {
            int rootU = findRoot(u);
            int rootV = findRoot(v);
            
            if (rootU == rootV) {
                return false; // Already in same component
            }
            
            // Union by rank
            if (rank[rootU] < rank[rootV]) {
                parent[rootU] = rootV;
            } else if (rank[rootU] > rank[rootV]) {
                parent[rootV] = rootU;
            } else {
                parent[rootV] = rootU;
                rank[rootU]++;
            }
            return true;
        }
        
        /**
         * Check if edge connects different components.
         */
        public boolean connectsDifferentComponents(Edge edge) {
            return findRoot(edge.u) != findRoot(edge.v);
        }
        
        /**
         * Get the number of distinct components.
         */
        public int getComponentCount() {
            Set<Integer> roots = new HashSet<>();
            for (int i = 0; i < numVertices; i++) {
                roots.add(findRoot(i));
            }
            return roots.size();
        }
        
        /**
         * Get component labels (root representatives).
         */
        public int[] getComponentLabels() {
            int[] labels = new int[numVertices];
            for (int i = 0; i < numVertices; i++) {
                labels[i] = findRoot(i);
            }
            return labels;
        }
        
        @Override
        public String toString() {
            return String.format("Components{vertices=%d, edges=%d, components=%d}", 
                               numVertices, edges.length, getComponentCount());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ComponentState)) return false;
            ComponentState other = (ComponentState) obj;
            return numVertices == other.numVertices &&
                   Arrays.equals(getComponentLabels(), other.getComponentLabels()) &&
                   Arrays.equals(edges, other.edges);
        }
    }

    /**
     * Connected Components problem using simplified LLP framework.
     */
    static class ConnectedComponentsLLPProblem implements LLPProblem<ComponentState> {
        
        private final int numVertices;
        private final Edge[] edges;
        
        public ConnectedComponentsLLPProblem(int numVertices, Edge[] edges) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
        }
        
        @Override
        public boolean Forbidden(ComponentState state) {
            // State is forbidden if any edge connects vertices with different components
            for (Edge edge : state.edges) {
                if (state.connectsDifferentComponents(edge)) {
                    return true; // Found disconnected components
                }
            }
            return false; // All edges connect vertices in same components
        }
        
        @Override
        public ComponentState Advance(ComponentState state, int threadId, int totalThreads) {
            // Each thread processes different edges - modifies the SAME state object
            // NO new state created, NO merging needed!
            
            // Distribute edges among threads using round-robin
            for (int edgeIndex = threadId; edgeIndex < state.edges.length; edgeIndex += totalThreads) {
                Edge edge = state.edges[edgeIndex];
                
                // If edge connects different components, union them
                if (state.connectsDifferentComponents(edge)) {
                    state.union(edge.u, edge.v);
                }
            }
            
            // Return the SAME state object (now modified)
            return state;
        }
        
        @Override
        public ComponentState getInitialState() {
            return new ComponentState(numVertices, edges);
        }
        
        @Override
        public boolean isSolution(ComponentState state) {
            // Solution when no edges connect different components
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Connected Components Example ===\n");
        
        // Example graph: 0-1-2  3-4  5-6-7-8  9
        Edge[] edges = {
            // Component 1: vertices 0, 1, 2
            new Edge(0, 1),
            new Edge(1, 2),
            
            // Component 2: vertices 3, 4
            new Edge(3, 4),
            
            // Component 3: vertices 5, 6, 7, 8
            new Edge(5, 6),
            new Edge(6, 7),
            new Edge(7, 8),
            new Edge(5, 8), // Creates a cycle
            
            // Isolated vertex 9 will be its own component
        };

        int numVertices = 10;
        int[] threadCounts = {1, 2, 4, 8};
        int maxIterations = 50;
        
        System.out.println("Problem: Find connected components in graph");
        System.out.println("Graph: " + numVertices + " vertices, " + edges.length + " edges");
        System.out.println("Expected components: {0,1,2}, {3,4}, {5,6,7,8}, {9}");
        
        System.out.println("Graph edges:");
        for (Edge edge : edges) {
            System.out.println("  " + edge);
        }
        System.out.println();
        
        // Test different thread counts
        for (int numThreads : threadCounts) {
            solveProblem(numVertices, edges, numThreads, maxIterations);
        }
    }

    private static void solveProblem(int numVertices, Edge[] edges, int numThreads, int maxIterations) {
        LLPSolver<ComponentState> solver = null;
        
        try {
            ConnectedComponentsLLPProblem problem = new ConnectedComponentsLLPProblem(numVertices, edges);
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.nanoTime();
            ComponentState solution = solver.solve();
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
                baselineTime = timeMs;
            } else {
                double speedup = baselineTime / timeMs;
                System.out.printf(" | Speedup: %.2fx\n", speedup);
            }
            
            // Show component details for first run only
            if (numThreads == 1) {
                int[] labels = solution.getComponentLabels();
                
                // Group vertices by component
                Map<Integer, List<Integer>> components = new HashMap<>();
                for (int i = 0; i < labels.length; i++) {
                    components.computeIfAbsent(labels[i], k -> new ArrayList<>()).add(i);
                }
                
                System.out.println("\nComponents found:");
                for (Map.Entry<Integer, List<Integer>> entry : components.entrySet()) {
                    Collections.sort(entry.getValue());
                    System.out.println("  Component " + entry.getKey() + ": " + entry.getValue());
                }
                System.out.printf("Total components: %d\n", solution.getComponentCount());
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Error with " + numThreads + " threads: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }

    // Store baseline time for speedup calculation
    private static double baselineTime = 0.0;
}
