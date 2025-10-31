package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.*;

/**
 * Boruvka's Minimum Spanning Tree Algorithm using the LLP framework.
 * 
 * Problem Description:
 * Boruvka's algorithm finds a minimum spanning tree by repeatedly finding
 * the minimum-weight edge leaving each component and adding all such edges.
 * 
 * LLP Implementation Strategy:
 * - State: Component labels and MST edges
 * - Forbidden: Forest contains cycles
 * - Advance: Add minimum outgoing edges from each component
 * - Ensure: Remove cycles by removing heaviest edge
 * - Parallelism: Multiple components processed simultaneously
 */
public class BoruvkaProblem {
    
    /**
     * Represents an undirected weighted edge.
     */
    static class Edge {
        final int u, v;
        final double weight;
        
        public Edge(int u, int v, double weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
        
        @Override
        public String toString() {
            return String.format("(%d-%d:%.1f)", u, v, weight);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) return false;
            Edge other = (Edge) obj;
            return weight == other.weight && 
                   ((u == other.u && v == other.v) || (u == other.v && v == other.u));
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(Math.min(u, v), Math.max(u, v), weight);
        }
    }

    /**
     * Simplified state for Boruvka's algorithm.
     */
    static class BoruvkaState {
        final Edge[] allEdges;        // All graph edges
        final Set<Edge> mstEdges;     // Current MST edges
        final int[] component;        // Component label for each vertex
        final int numVertices;
        
        public BoruvkaState(int numVertices, Edge[] allEdges) {
            this.numVertices = numVertices;
            this.allEdges = allEdges.clone();
            this.mstEdges = new HashSet<>();
            this.component = new int[numVertices];
            
            // Each vertex starts in its own component
            for (int i = 0; i < numVertices; i++) {
                component[i] = i;
            }
        }
        
        public BoruvkaState(Edge[] allEdges, Set<Edge> mstEdges, int[] component, int numVertices) {
            this.allEdges = allEdges.clone();
            this.mstEdges = new HashSet<>(mstEdges);
            this.component = component.clone();
            this.numVertices = numVertices;
        }
        
        public BoruvkaState withAddedEdge(Edge edge) {
            Set<Edge> newMstEdges = new HashSet<>(mstEdges);
            newMstEdges.add(edge);
            
            // Merge components
            int[] newComponent = component.clone();
            int compU = component[edge.u];
            int compV = component[edge.v];
            int minComp = Math.min(compU, compV);
            int maxComp = Math.max(compU, compV);
            
            // Relabel larger component to smaller
            for (int i = 0; i < numVertices; i++) {
                if (newComponent[i] == maxComp) {
                    newComponent[i] = minComp;
                }
            }
            
            return new BoruvkaState(allEdges, newMstEdges, newComponent, numVertices);
        }
        
        public BoruvkaState withoutEdge(Edge edge) {
            Set<Edge> newMstEdges = new HashSet<>(mstEdges);
            newMstEdges.remove(edge);
            
            // Recalculate components after edge removal
            int[] newComponent = recalculateComponents(newMstEdges);
            return new BoruvkaState(allEdges, newMstEdges, newComponent, numVertices);
        }
        
        public BoruvkaState mergeWith(BoruvkaState other) {
            Set<Edge> newMstEdges = new HashSet<>(mstEdges);
            newMstEdges.addAll(other.mstEdges);
            
            int[] newComponent = recalculateComponents(newMstEdges);
            return new BoruvkaState(allEdges, newMstEdges, newComponent, numVertices);
        }
        
        public int[] recalculateComponents(Set<Edge> edges) {
            int[] comp = new int[numVertices];
            for (int i = 0; i < numVertices; i++) {
                comp[i] = i;
            }
            
            // Simple Union-Find
            for (Edge edge : edges) {
                int rootU = findRoot(comp, edge.u);
                int rootV = findRoot(comp, edge.v);
                if (rootU != rootV) {
                    comp[Math.max(rootU, rootV)] = Math.min(rootU, rootV);
                }
            }
            
            // Flatten paths
            for (int i = 0; i < numVertices; i++) {
                comp[i] = findRoot(comp, i);
            }
            
            return comp;
        }
        
        public int findRoot(int[] comp, int v) {
            while (comp[v] != v) {
                v = comp[v];
            }
            return v;
        }
        
        public int getNumComponents() {
            Set<Integer> unique = new HashSet<>();
            for (int c : component) {
                unique.add(c);
            }
            return unique.size();
        }
        
        @Override
        public String toString() {
            return String.format("Boruvka{vertices=%d, components=%d, mstEdges=%d}", 
                               numVertices, getNumComponents(), mstEdges.size());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BoruvkaState)) return false;
            BoruvkaState other = (BoruvkaState) obj;
            return Arrays.equals(component, other.component) && mstEdges.equals(other.mstEdges);
        }
    }

    /**
     * Boruvka LLP implementation using unified interface.
     */
    static class BoruvkaLLPProblem implements LLPProblem<BoruvkaState> {
        
        private final int numVertices;
        private final Edge[] allEdges;
        
        public BoruvkaLLPProblem(int numVertices, Edge[] allEdges) {
            this.numVertices = numVertices;
            this.allEdges = allEdges.clone();
        }
        
        @Override
        public boolean Forbidden(BoruvkaState state) {
            // Simple and efficient: a forest with n vertices can have at most n-1 edges
            if (state.mstEdges.size() >= state.numVertices) {
                return true; // Too many edges - must have a cycle
            }
            
            // Use Union-Find to detect cycles without modifying the state
            int[] parent = new int[state.numVertices];
            for (int i = 0; i < state.numVertices; i++) {
                parent[i] = i;
            }
            
            for (Edge edge : state.mstEdges) {
                int rootU = findRootWithPathCompression(parent, edge.u);
                int rootV = findRootWithPathCompression(parent, edge.v);
                
                if (rootU == rootV) {
                    return true; // Cycle detected - both endpoints in same component
                }
                
                // Union the components
                parent[rootV] = rootU;
            }
            
            return false;
        }
        
        // Helper method to avoid conflicts with existing findRoot
        private int findRootWithPathCompression(int[] parent, int v) {
            if (parent[v] != v) {
                parent[v] = findRootWithPathCompression(parent, parent[v]); // Path compression
            }
            return parent[v];
        }
        
        @Override
        public BoruvkaState Ensure(BoruvkaState state, int threadId, int totalThreads) {
            // Each thread removes cycle edges for specific components using unified interface
            List<Edge> invalidEdges = new ArrayList<>();
            
            // Find invalid edges in this thread's partition
            Set<Integer> components = new HashSet<>();
            for (int i = 0; i < state.numVertices; i++) {
                components.add(state.component[i]);
            }
            
            List<Integer> compList = new ArrayList<>(components);
            
            // Distribute components among threads
            for (int i = threadId; i < compList.size(); i += totalThreads) {
                int comp = compList.get(i);
                
                // Check edges involving this component
                for (Edge edge : state.mstEdges) {
                    if ((state.component[edge.u] == comp || state.component[edge.v] == comp) &&
                        state.component[edge.u] == state.component[edge.v]) {
                        invalidEdges.add(edge);
                    }
                }
            }
            
            if (!invalidEdges.isEmpty()) {
                Edge heaviest = invalidEdges.stream()
                    .max(Comparator.comparingDouble(e -> e.weight))
                    .orElse(null);
                System.out.println("    Thread-" + threadId + " removing cycle edge " + heaviest);
                return state.withoutEdge(heaviest);
            }
            
            return state;
        }
        
        @Override
        public BoruvkaState Advance(BoruvkaState state, int threadId, int totalThreads) {
            // Each thread finds minimum edges for specific components using unified interface
            Map<Integer, Edge> componentMinEdges = new HashMap<>();
            
            Set<Integer> components = new HashSet<>();
            for (int i = 0; i < state.numVertices; i++) {
                components.add(state.component[i]);
            }
            
            List<Integer> compList = new ArrayList<>(components);
            
            // Distribute components among threads using modulo arithmetic
            // Thread 0 gets: 0, 4, 8...
            // Thread 1 gets: 1, 5, 9...
            // Thread 2 gets: 2, 6, 10...
            // Thread 3 gets: 3, 7, 11...
            for (int i = threadId; i < compList.size(); i += totalThreads) {
                int comp = compList.get(i);
                Edge minEdge = findMinOutgoingEdge(state, comp);
                if (minEdge != null) {
                    componentMinEdges.put(comp, minEdge);
                    System.out.println("    Thread-" + threadId + " found min edge for component " + comp + ": " + minEdge);
                }
            }
            
            // Add minimum edges found by this thread
            BoruvkaState result = state;
            for (Edge edge : componentMinEdges.values()) {
                if (!result.mstEdges.contains(edge)) {
                    // Only add if it still connects different components
                    if (result.component[edge.u] != result.component[edge.v]) {
                        result = result.withAddedEdge(edge);
                        System.out.println("    Thread-" + threadId + " added MST edge: " + edge);
                    }
                }
            }
            
            return result;
        }
        
        @Override
        public BoruvkaState getInitialState() {
            return new BoruvkaState(numVertices, allEdges);
        }
        
        @Override
        public boolean isSolution(BoruvkaState state) {
            if (Forbidden(state)) return false;
            
            // Check if we have optimal number of edges and no more outgoing edges
            int expectedEdges = state.numVertices - state.getNumComponents();
            if (state.mstEdges.size() != expectedEdges) return false;
            
            // Check if any component has outgoing edges
            Set<Integer> components = new HashSet<>();
            for (int i = 0; i < state.numVertices; i++) {
                components.add(state.component[i]);
            }
            
            for (int comp : components) {
                if (findMinOutgoingEdge(state, comp) != null) {
                    return false; // Still have edges to add
                }
            }
            
            return true;
        }
        
        @Override
        public BoruvkaState merge(BoruvkaState state1, BoruvkaState state2) {
            // Take the union of MST edges
            Set<Edge> allEdges = new HashSet<>(state1.mstEdges);
            allEdges.addAll(state2.mstEdges);
            
            // If no new edges, return state1
            if (allEdges.equals(state1.mstEdges)) {
                return state1;
            }
            
            // Create new state with merged edges and recalculated components
            BoruvkaState merged = new BoruvkaState(state1.allEdges, allEdges, new int[state1.numVertices], state1.numVertices);
            int[] newComponents = merged.recalculateComponents(allEdges);
            
            return new BoruvkaState(merged.allEdges, merged.mstEdges, newComponents, merged.numVertices);
        }
        
        private Edge findMinOutgoingEdge(BoruvkaState state, int comp) {
            Edge minEdge = null;
            double minWeight = Double.POSITIVE_INFINITY;
            
            for (Edge edge : state.allEdges) {
                boolean uInComp = state.component[edge.u] == comp;
                boolean vInComp = state.component[edge.v] == comp;
                
                if (uInComp ^ vInComp) { // One in component, one out
                    if (edge.weight < minWeight) {
                        minWeight = edge.weight;
                        minEdge = edge;
                    }
                }
            }
            
            return minEdge;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Boruvka's Minimum Spanning Tree Example ===\n");
        
        // Larger test graph with 10 vertices and 21 edges
        Edge[] edges = {
            // Core connections (forms a spanning tree)
            new Edge(0, 1, 2.0),
            new Edge(1, 2, 3.0),
            new Edge(2, 3, 1.0),
            new Edge(3, 4, 4.0),
            new Edge(4, 5, 2.0),
            new Edge(5, 6, 3.0),
            new Edge(6, 7, 1.0),
            new Edge(7, 8, 5.0),
            new Edge(8, 9, 2.0),
            
            // Additional edges (some may be cheaper alternatives)
            new Edge(0, 3, 6.0),
            new Edge(1, 4, 7.0),
            new Edge(2, 5, 4.0),
            new Edge(3, 6, 5.0),
            new Edge(4, 7, 3.0),
            new Edge(5, 8, 6.0),
            new Edge(6, 9, 4.0),
            
            // Cross connections
            new Edge(0, 5, 8.0),
            new Edge(1, 6, 9.0),
            new Edge(2, 7, 7.0),
            new Edge(3, 8, 8.0),
            new Edge(4, 9, 6.0)
        };

        int numVertices = 10;
        int numThreads = 4; // More threads for larger graph
        int maxIterations = 100;
        
        BoruvkaLLPProblem problem = new BoruvkaLLPProblem(numVertices, edges);
        BoruvkaState initial = problem.getInitialState();
        
        System.out.println("Problem: Find minimum spanning tree");
        System.out.println("Graph: " + numVertices + " vertices, " + edges.length + " edges");
        System.out.println("Graph edges:");
        for (Edge edge : edges) {
            System.out.println("  " + edge);
        }
        System.out.println("\nExpected MST should have " + (numVertices - 1) + " edges");
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        
        // Solve using the LLP framework
        solveProblem(problem, numThreads, maxIterations);
        
        // Also test with different thread counts
        System.out.println("\n=== Performance Comparison ===");
        testPerformance(problem, numVertices, edges);
    }

    private static void solveProblem(BoruvkaLLPProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<BoruvkaState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            BoruvkaState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found!");
            System.out.println("MST edges:");
            double totalWeight = 0.0;
            for (Edge edge : solution.mstEdges) {
                System.out.println("  " + edge);
                totalWeight += edge.weight;
            }
            System.out.println("Total MST weight: " + totalWeight);
            System.out.println("Number of components: " + solution.getNumComponents());
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            
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

    /**
     * Additional testing cases for performance comparison. Use in presentation
     * @param problem is the BoruvkaLLPProblem instance
     * @param numVertices is the number of vertices in the graph
     * @param edges is the array of edges in the graph
     */
    private static void testPerformance(BoruvkaLLPProblem problem, int numVertices, Edge[] edges) {
        int[] threadCounts = {1, 2, 4, 8};
        int maxIterations = 100;
        
        for (int threads : threadCounts) {
            System.out.println("\n--- Testing with " + threads + " thread(s) ---");
            
            LLPSolver<BoruvkaState> solver = null;
            try {
                solver = new LLPSolver<>(problem, threads, maxIterations);
                
                long startTime = System.nanoTime();
                BoruvkaState solution = solver.solve();
                long endTime = System.nanoTime();
                
                double totalWeight = solution.mstEdges.stream()
                    .mapToDouble(edge -> edge.weight)
                    .sum();
                
                LLPSolver.ExecutionStats stats = solver.getExecutionStats();
                
                System.out.printf("Threads: %d | Time: %.2fms | Weight: %.1f | Edges: %d | Iterations: %d | Valid: %s%n",
                    threads,
                    (endTime - startTime) / 1_000_000.0,
                    totalWeight,
                    solution.mstEdges.size(),
                    stats != null ? stats.getIterationCount() : -1,
                    problem.isSolution(solution)
                );
                
            } catch (Exception e) {
                System.err.println("Error with " + threads + " threads: " + e.getMessage());
            } finally {
                if (solver != null) {
                    solver.shutdown();
                }
            }
        }
    }
}
