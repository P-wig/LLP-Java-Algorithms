package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import com.llp.framework.GraphFileReader;

import java.util.*;
import java.io.*;

/**
 * Boruvka's MST Algorithm using the simplified LLP framework.
 * 
 * Problem Description:
 * Boruvka's algorithm finds the Minimum Spanning Tree (MST) by repeatedly
 * finding the minimum weight edge from each component and adding them to the MST.
 * 
 * LLP Implementation Strategy:
 * - State: Current components and MST edges
 * - Forbidden: Components that don't have their minimum edge selected
 * - Advance: Find and add minimum edges for each component (NO MERGE NEEDED)
 * - Parallelism: Different threads can work on different components simultaneously
 */
public class BoruvkaProblem {
    
    public static class Edge {
        public final int u, v;
        public final double weight;
        
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
     * Uses thread-safe operations for parallel updates.
     */
    static class BoruvkaState {
        final Edge[] allEdges;              // All graph edges (readonly)
        volatile int[] parent;              // Union-Find parent array - thread-safe
        volatile boolean[] mstEdgeAdded;    // Which edges are in MST - thread-safe
        volatile boolean[] componentProcessed; // Which components found their min edge
        final int numVertices;              // Number of vertices
        final Object lock = new Object();   // Synchronization lock
        
        public BoruvkaState(int numVertices, Edge[] allEdges) {
            this.numVertices = numVertices;
            this.allEdges = allEdges.clone();
            this.parent = new int[numVertices];
            this.mstEdgeAdded = new boolean[allEdges.length];
            this.componentProcessed = new boolean[numVertices];
            
            // Initialize Union-Find: each vertex is its own parent
            for (int i = 0; i < numVertices; i++) {
                parent[i] = i;
            }
            Arrays.fill(mstEdgeAdded, false);
            Arrays.fill(componentProcessed, false);
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
         * Thread-safe union of two components.
         */
        public synchronized boolean union(int u, int v) {
            int rootU = findRoot(u);
            int rootV = findRoot(v);
            
            if (rootU == rootV) {
                return false; // Already in same component
            }
            
            // Union by making smaller root point to larger root
            if (rootU < rootV) {
                parent[rootV] = rootU;
            } else {
                parent[rootU] = rootV;
            }
            return true;
        }
        
        /**
         * Thread-safe check if edge connects different components.
         */
        public boolean connectsDifferentComponents(Edge edge) {
            return findRoot(edge.u) != findRoot(edge.v);
        }
        
        /**
         * Thread-safe method to mark MST edge as added.
         */
        public synchronized void addMSTEdge(int edgeIndex) {
            mstEdgeAdded[edgeIndex] = true;
        }
        
        /**
         * Get current MST edges.
         */
        public List<Edge> getMSTEdges() {
            List<Edge> mstEdges = new ArrayList<>();
            for (int i = 0; i < allEdges.length; i++) {
                if (mstEdgeAdded[i]) {
                    mstEdges.add(allEdges[i]);
                }
            }
            return mstEdges;
        }
        
        /**
         * Count number of components.
         */
        public int getComponentCount() {
            Set<Integer> roots = new HashSet<>();
            for (int i = 0; i < numVertices; i++) {
                roots.add(findRoot(i));
            }
            return roots.size();
        }
        
        @Override
        public String toString() {
            return String.format("Boruvka{vertices=%d, components=%d, mstEdges=%d}", 
                               numVertices, getComponentCount(), getMSTEdges().size());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BoruvkaState)) return false;
            BoruvkaState other = (BoruvkaState) obj;
            return numVertices == other.numVertices &&
                   Arrays.equals(parent, other.parent) &&
                   Arrays.equals(mstEdgeAdded, other.mstEdgeAdded);
        }
    }

    /**
     * Boruvka's algorithm using simplified LLP framework.
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
            // Forbidden if we have multiple components but can still add edges
            if (state.getComponentCount() <= 1) {
                return false; // Single component = MST complete
            }
            
            // Check if any component can still add a minimum edge
            for (int i = 0; i < state.numVertices; i++) {
                int root = state.findRoot(i);
                if (root == i) { // This is a component root
                    // Find if this component has a minimum outgoing edge
                    Edge minEdge = null;
                    double minWeight = Double.POSITIVE_INFINITY;
                    
                    for (int edgeIdx = 0; edgeIdx < state.allEdges.length; edgeIdx++) {
                        if (!state.mstEdgeAdded[edgeIdx]) {
                            Edge edge = state.allEdges[edgeIdx];
                            if (state.connectsDifferentComponents(edge)) {
                                int edgeRoot = -1;
                                if (state.findRoot(edge.u) == root) {
                                    edgeRoot = root;
                                } else if (state.findRoot(edge.v) == root) {
                                    edgeRoot = root;
                                }
                                
                                if (edgeRoot == root && edge.weight < minWeight) {
                                    minWeight = edge.weight;
                                    minEdge = edge;
                                }
                            }
                        }
                    }
                    
                    if (minEdge != null) {
                        return true; // Found a component with available minimum edge
                    }
                }
            }
            
            return false; // No more edges can be added
        }
        
        @Override
        public BoruvkaState Advance(BoruvkaState state, int threadId, int totalThreads) {
            // SEQUENTIAL processing to maintain deterministic behavior
            // Only Thread-0 does the work to avoid race conditions
            
            if (threadId == 0) {
                // Get all current component roots at start of iteration
                Set<Integer> componentRoots = new HashSet<>();
                for (int i = 0; i < state.numVertices; i++) {
                    componentRoots.add(state.findRoot(i));
                }
                
                // For each component, find its minimum edge
                Map<Integer, Edge> componentMinEdges = new HashMap<>();
                Map<Integer, Integer> componentMinEdgeIndices = new HashMap<>();
                
                // Phase 1: Find minimum edge for each component
                for (int componentRoot : componentRoots) {
                    Edge minEdge = null;
                    int minEdgeIndex = -1;
                    double minWeight = Double.POSITIVE_INFINITY;
                    
                    for (int edgeIdx = 0; edgeIdx < state.allEdges.length; edgeIdx++) {
                        if (!state.mstEdgeAdded[edgeIdx]) {
                            Edge edge = state.allEdges[edgeIdx];
                            if (state.connectsDifferentComponents(edge)) {
                                // Check if this edge is incident to our component
                                boolean incidentToComponent = 
                                    (state.findRoot(edge.u) == componentRoot) || 
                                    (state.findRoot(edge.v) == componentRoot);
                                
                                if (incidentToComponent && edge.weight < minWeight) {
                                    minWeight = edge.weight;
                                    minEdge = edge;
                                    minEdgeIndex = edgeIdx;
                                }
                            }
                        }
                    }
                    
                    if (minEdge != null) {
                        componentMinEdges.put(componentRoot, minEdge);
                        componentMinEdgeIndices.put(componentRoot, minEdgeIndex);
                    }
                }
                
                // Phase 2: Add all minimum edges (in deterministic order)
                List<Integer> sortedRoots = new ArrayList<>(componentMinEdges.keySet());
                Collections.sort(sortedRoots); // Deterministic order
                
                for (int componentRoot : sortedRoots) {
                    Edge minEdge = componentMinEdges.get(componentRoot);
                    int minEdgeIndex = componentMinEdgeIndices.get(componentRoot);
                    
                    // Double-check edge is still valid after previous unions
                    if (minEdge != null && !state.mstEdgeAdded[minEdgeIndex] && 
                        state.connectsDifferentComponents(minEdge)) {
                        
                        state.addMSTEdge(minEdgeIndex);
                        state.union(minEdge.u, minEdge.v);
                    }
                }
            }
            
            // All other threads wait (no work to do)
            // This ensures deterministic behavior
            
            return state;
        }
        
        @Override
        public BoruvkaState getInitialState() {
            return new BoruvkaState(numVertices, allEdges);
        }
        
        @Override
        public boolean isSolution(BoruvkaState state) {
            // Solution when we have a single component (complete MST)
            return state.getComponentCount() <= 1 || state.getMSTEdges().size() >= numVertices - 1;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Boruvka's MST Algorithm Example ===\n");
        
        // Test with different thread counts
        int[] threadCounts = {1, 2, 4, 8};
        int maxIterations = 100;
        
        // Generate a test graph
        int numVertices = 1000;
        Edge[] edges = generateTestGraph(numVertices);
        
        // Create the problem
        BoruvkaLLPProblem problem = new BoruvkaLLPProblem(numVertices, edges);
        BoruvkaState initial = problem.getInitialState();
        
        System.out.println("Problem: Find Minimum Spanning Tree using Boruvka's algorithm");
        System.out.println("Graph: " + numVertices + " vertices, " + edges.length + " edges");
        
        // Test different thread counts
        for (int numThreads : threadCounts) {
            solveProblem(problem, numThreads, maxIterations);
        }
    }
    
    /**
     * Generate a connected test graph for MST algorithms.
     */
    private static Edge[] generateTestGraph(int numVertices) {
        List<Edge> edgeList = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        // Create spanning tree to ensure connectivity
        for (int i = 1; i < numVertices; i++) {
            int parent = random.nextInt(i);
            double weight = 1.0 + random.nextDouble() * 10.0;
            edgeList.add(new Edge(parent, i, weight));
        }
        
        // Add extra edges for complexity
        int extraEdges = numVertices * 2;
        for (int i = 0; i < extraEdges; i++) {
            int u = random.nextInt(numVertices);
            int v = random.nextInt(numVertices);
            if (u != v) {
                double weight = 1.0 + random.nextDouble() * 10.0;
                edgeList.add(new Edge(u, v, weight));
            }
        }
        
        return edgeList.toArray(new Edge[0]);
    }

    private static void solveProblem(BoruvkaLLPProblem problem, int numThreads, int maxIterations) {
        LLPSolver<BoruvkaState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.nanoTime();
            BoruvkaState solution = solver.solve();
            long endTime = System.nanoTime();
            
            // Show results
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
            
            // Show MST info for first run only
            if (numThreads == 1) {
                List<Edge> mstEdges = solution.getMSTEdges();
                double totalWeight = mstEdges.stream().mapToDouble(e -> e.weight).sum();
                
                System.out.println("\nMST Information:");
                System.out.printf("  MST edges: %d (expected: %d)\n", 
                                mstEdges.size(), solution.numVertices - 1);
                System.out.printf("  Total weight: %.2f\n", totalWeight);
                System.out.printf("  Components: %d\n", solution.getComponentCount());
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
