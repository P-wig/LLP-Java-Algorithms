package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.*;

/**
 * Connected Components Problem using the LLP framework (Fast Parallel Algorithm).
 * 
 * <h3>Problem Description:</h3>
 * Find all connected components in an undirected graph. A connected component
 * is a maximal set of vertices such that there is a path between every pair
 * of vertices within the set.
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
     * State representation for Connected Components algorithm.
     */
    static class ComponentState {
        final Edge[] edges;           // Graph edges (readonly)
        final int[] labels;           // Component labels for each vertex
        final int numVertices;        // Number of vertices
        
        public ComponentState(int numVertices, Edge[] edges) {
            this.numVertices = numVertices;
            this.edges = edges.clone();
            this.labels = new int[numVertices];
            
            // Initialize: each vertex is its own component
            for (int i = 0; i < numVertices; i++) {
                labels[i] = i;
            }
        }
        
        public ComponentState(Edge[] edges, int[] labels, int numVertices) {
            this.edges = edges.clone();
            this.labels = labels.clone();
            this.numVertices = numVertices;
        }
        
        public ComponentState withUpdatedLabels(int[] newLabels) {
            return new ComponentState(edges, newLabels, numVertices);
        }
        
        /**
         * Merge this state with another state, taking the minimum component labels.
         */
        public ComponentState mergeWith(ComponentState other) {
            int[] newLabels = labels.clone();
            
            for (int i = 0; i < numVertices; i++) {
                newLabels[i] = Math.min(newLabels[i], other.labels[i]);
            }
            
            return new ComponentState(edges, newLabels, numVertices);
        }
        
        /**
         * Get the number of distinct components.
         */
        public int getComponentCount() {
            Set<Integer> distinctLabels = new HashSet<>();
            for (int label : labels) {
                distinctLabels.add(label);
            }
            return distinctLabels.size();
        }
        
        @Override
        public String toString() {
            return String.format("Components{vertices=%d, edges=%d, components=%d, labels=%s}", 
                               numVertices, edges.length, getComponentCount(), Arrays.toString(labels));
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ComponentState)) return false;
            ComponentState other = (ComponentState) obj;
            return numVertices == other.numVertices &&
                   Arrays.equals(labels, other.labels) &&
                   Arrays.equals(edges, other.edges);
        }
    }

    /**
     * Connected Components problem implementation using unified LLP framework.
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
            // State is forbidden if any edge connects vertices with different labels
            for (Edge edge : state.edges) {
                if (state.labels[edge.u] != state.labels[edge.v]) {
                    return true; // Inconsistent component labels
                }
            }
            return false;
        }
        
        @Override
        public ComponentState Ensure(ComponentState state, int threadId, int totalThreads) {
            // Fix inconsistent component labels by merging components
            int[] newLabels = state.labels.clone();
            boolean changed = false;
            
            // Distribute edges among threads using round-robin
            for (int edgeIndex = threadId; edgeIndex < state.edges.length; edgeIndex += totalThreads) {
                Edge edge = state.edges[edgeIndex];
                
                if (newLabels[edge.u] != newLabels[edge.v]) {
                    // Merge components by assigning minimum label
                    int minLabel = Math.min(newLabels[edge.u], newLabels[edge.v]);
                    int maxLabel = Math.max(newLabels[edge.u], newLabels[edge.v]);
                    
                    System.out.println("    Thread-" + threadId + " merging components: " + 
                                     maxLabel + " -> " + minLabel + " (edge " + edge + ")");
                    
                    // Update all vertices with maxLabel to minLabel
                    for (int i = 0; i < newLabels.length; i++) {
                        if (newLabels[i] == maxLabel) {
                            newLabels[i] = minLabel;
                            changed = true;
                        }
                    }
                }
            }
            
            return changed ? state.withUpdatedLabels(newLabels) : state;
        }
        
        @Override
        public ComponentState Advance(ComponentState state, int threadId, int totalThreads) {
            // Propagate component labels along edges
            int[] newLabels = state.labels.clone();
            boolean changed = false;
            
            // Distribute edges among threads using round-robin
            for (int edgeIndex = threadId; edgeIndex < state.edges.length; edgeIndex += totalThreads) {
                Edge edge = state.edges[edgeIndex];
                
                int minLabel = Math.min(state.labels[edge.u], state.labels[edge.v]);
                
                // Propagate minimum label to both vertices
                if (newLabels[edge.u] > minLabel) {
                    System.out.println("    Thread-" + threadId + " updating vertex " + edge.u + 
                                     ": " + newLabels[edge.u] + " -> " + minLabel);
                    newLabels[edge.u] = minLabel;
                    changed = true;
                }
                
                if (newLabels[edge.v] > minLabel) {
                    System.out.println("    Thread-" + threadId + " updating vertex " + edge.v + 
                                     ": " + newLabels[edge.v] + " -> " + minLabel);
                    newLabels[edge.v] = minLabel;
                    changed = true;
                }
            }
            
            return changed ? state.withUpdatedLabels(newLabels) : state;
        }
        
        @Override
        public ComponentState merge(ComponentState state1, ComponentState state2) {
            // Merge by taking minimum component labels
            System.out.println("    Merging states: components1=" + state1.getComponentCount() + 
                             ", components2=" + state2.getComponentCount());
            
            ComponentState merged = state1.mergeWith(state2);
            
            System.out.println("    Merged result: components=" + merged.getComponentCount());
            return merged;
        }
        
        @Override
        public ComponentState getInitialState() {
            return new ComponentState(numVertices, edges);
        }
        
        @Override
        public boolean isSolution(ComponentState state) {
            // Solution when no constraint violations exist
            return !Forbidden(state);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Connected Components Example ===\n");
        
        // Example graph: 0-1-2  3-4  5-6-7-8
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
        int numThreads = 2;
        int maxIterations = 50;
        
        // Create the problem
        ConnectedComponentsLLPProblem problem = new ConnectedComponentsLLPProblem(numVertices, edges);
        ComponentState initial = problem.getInitialState();
        
        System.out.println("Problem: Find connected components in graph");
        System.out.println("Graph edges:");
        for (Edge edge : edges) {
            System.out.println("  " + edge);
        }
        System.out.println("Expected components: {0,1,2}, {3,4}, {5,6,7,8}, {9}");
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        
        // Solve using the LLP framework
        solveProblem(problem, numThreads, maxIterations);
    }

    private static void solveProblem(ConnectedComponentsLLPProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- LLP Framework Solution ---");
        
        LLPSolver<ComponentState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            ComponentState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found!");
            System.out.println("Component labels: " + Arrays.toString(solution.labels));
            System.out.println("Number of components: " + solution.getComponentCount());
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            
            // Display components
            Map<Integer, List<Integer>> components = new HashMap<>();
            for (int i = 0; i < solution.labels.length; i++) {
                components.computeIfAbsent(solution.labels[i], k -> new ArrayList<>()).add(i);
            }
            
            System.out.println("Components found:");
            for (Map.Entry<Integer, List<Integer>> entry : components.entrySet()) {
                System.out.println("  Component " + entry.getKey() + ": " + entry.getValue());
            }
            
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
    }
}
