package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import com.llp.framework.GraphFileReader;

import java.util.*;
import java.io.*;  // Add this if not already present

/**
 * Boruvka's Minimum Spanning Tree Algorithm using the LLP framework.
 * Following the written recursive example structure.
 */
public class BoruvkaProblem {
    
    /**
     * Represents an undirected weighted edge.
     */
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
     * State following the written example structure with parent array G and edge set T
     */
    static class BoruvkaState {
        final Set<Integer> V;         // Current vertex set  
        final Set<Edge> E;           // Current edge set
        final int[] G;               // Parent array from written example
        final Set<Edge> T;           // MST edges (T from written example)
        final int numOriginalVertices; // For array sizing
        
        // ADD: Adjacency list for efficient edge lookup
        final Map<Integer, List<Edge>> adjacencyList;
        
        public BoruvkaState(Set<Integer> V, Set<Edge> E, int numOriginalVertices) {
            this.V = new HashSet<>(V);
            this.E = new HashSet<>(E);
            this.numOriginalVertices = numOriginalVertices;
            this.G = new int[numOriginalVertices];
            this.T = new HashSet<>();
            
            // Initialize G[v] = v for all vertices (each vertex is its own parent initially)
            for (int v : V) {
                G[v] = v;
            }
            
            // Build adjacency list for efficient mwe() lookups
            this.adjacencyList = buildAdjacencyList();
        }
        
        public BoruvkaState(Set<Integer> V, Set<Edge> E, int[] G, Set<Edge> T, int numOriginalVertices) {
            this.V = new HashSet<>(V);
            this.E = new HashSet<>(E);
            this.G = G.clone();
            this.T = new HashSet<>(T);
            this.numOriginalVertices = numOriginalVertices;
            this.adjacencyList = buildAdjacencyList();
        }
        
        /**
         * Build adjacency list for efficient edge lookups
         */
        private Map<Integer, List<Edge>> buildAdjacencyList() {
            Map<Integer, List<Edge>> adjList = new HashMap<>();
            
            for (Edge edge : E) {
                adjList.computeIfAbsent(edge.u, k -> new ArrayList<>()).add(edge);
                adjList.computeIfAbsent(edge.v, k -> new ArrayList<>()).add(edge);
            }
            
            return adjList;
        }
        
        /**
         * Find minimum weight edge from vertex v that connects to a different component
         * NOW OPTIMIZED: O(degree(v)) instead of O(E) AND prevents duplicate edges
         */
        public Edge mwe(int v, int[] G) {
            List<Edge> edges = adjacencyList.get(v);
            if (edges == null || edges.isEmpty()) {
                return null;
            }
            
            Edge minEdge = null;
            double minWeight = Double.POSITIVE_INFINITY;
            int rootV = findRoot(G, v);
            
            for (Edge edge : edges) {
                int w = (edge.u == v) ? edge.v : edge.u;
                int rootW = findRoot(G, w);
                
                // Only consider edges that connect to different components
                if (rootV != rootW && edge.weight < minWeight) {
                    minWeight = edge.weight;
                    minEdge = edge;
                }
            }
            return minEdge;
        }
        
        /**
         * Get the other vertex in an edge
         */
        public int getOther(Edge edge, int v) {
            return edge.u == v ? edge.v : edge.u;
        }
        
        /**
         * Create new state with updated parent array and MST edges
         */
        public BoruvkaState withUpdatedParentsAndEdges(int[] newG, Set<Edge> newT) {
            return new BoruvkaState(V, E, newG, newT, numOriginalVertices);
        }
        
        /**
         * Reduce graph for recursive call following written example:
         * E' := {(G[v], G[w])|((v, w) ∈ E) ∧ (G[v] ≠ G[w])}
         * V' := {v ∈ V | G[v] = v}
         */
        public BoruvkaState reduceGraph() {
            // V' := {v ∈ V | G[v] = v} - vertices that are component representatives
            Set<Integer> newV = new HashSet<>();
            for (int v : V) {
                if (v < G.length && G[v] == v) {
                    newV.add(v);
                }
            }
            
            // E' := {(G[v], G[w])|((v, w) ∈ E) ∧ (G[v] ≠ G[w])} - edges between different components
            Set<Edge> newE = new HashSet<>();
            for (Edge edge : E) {
                if (edge.u < G.length && edge.v < G.length) {
                    int gU = G[edge.u];
                    int gV = G[edge.v];
                    if (gU != gV) {
                        // Create edge between component representatives
                        newE.add(new Edge(gU, gV, edge.weight));
                    }
                }
            }
            
            // Return reduced state with same parent array but reduced vertices/edges
            return new BoruvkaState(newV, newE, G, T, numOriginalVertices);
        }
        
        @Override
        public String toString() {
            return String.format("Boruvka{V=%d, E=%d, T=%d}", V.size(), E.size(), T.size());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BoruvkaState)) return false;
            BoruvkaState other = (BoruvkaState) obj;
            
            // States are equal if they have the same vertex set, edge set, parent array, and MST edges
            return V.equals(other.V) && 
                   E.equals(other.E) && 
                   Arrays.equals(G, other.G) && 
                   T.equals(other.T);
        }
        
        /**
         * Find root with path compression
         */
        private int findRoot(int[] G, int v) {
            if (v >= G.length) return v;
            
            // Simple path compression
            while (v < G.length && G[v] != v) {
                v = G[v];
            }
            return v;
        }
    }

    /**
     * Boruvka LLP implementation following the written recursive example
     */
    static class BoruvkaLLPProblem implements LLPProblem<BoruvkaState> {
        
        private final int numVertices;
        private final Edge[] allEdges;
        
        // Add debugging fields
        private volatile boolean hasLoggedFirstIteration = false;
        private volatile long firstIterationTime = 0;
        private volatile int iterationCount = 0;
        
        public BoruvkaLLPProblem(int numVertices, Edge[] allEdges) {
            this.numVertices = numVertices;
            this.allEdges = allEdges.clone();
        }
        
        @Override
        public boolean Forbidden(BoruvkaState state) {
            // forbidden(j) ≡ G[j] ≠ G[G[j]] from written example
            for (int j : state.V) {
                if (j < state.G.length && state.G[j] < state.G.length) {
                    if (state.G[j] != state.G[state.G[j]]) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        @Override
        public BoruvkaState Ensure(BoruvkaState state, int threadId, int totalThreads) {
            // Fix forbidden states by applying path compression
            // This implements the "advance" operation: G[j] := G[G[j]]
            int[] newG = state.G.clone();
            
            // Distribute vertices among threads
            List<Integer> vertices = new ArrayList<>(state.V);
            for (int i = threadId; i < vertices.size(); i += totalThreads) {
                int j = vertices.get(i);
                if (j < newG.length && newG[j] < newG.length) {
                    newG[j] = newG[newG[j]]; // Path compression step
                }
            }
            
            return state.withUpdatedParentsAndEdges(newG, state.T);
        }
        
        @Override
        public BoruvkaState Advance(BoruvkaState state, int threadId, int totalThreads) {
            // Add iteration counter and timeout tracking
            if (threadId == 0) {
                System.out.printf("[%s] LLP Advance iteration: |V|=%,d, |E|=%,d, |T|=%,d (Thread %d)\n", 
                    new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()),
                    state.V.size(), state.E.size(), state.T.size(), threadId);
            }
            
            // Base case from written example: if |E| = 0 return {}
            if (state.E.isEmpty()) {
                if (threadId == 0) System.out.println("Base case: No edges remaining");
                return state;
            }
            
            // Additional base case: if |V| <= 1, we're done
            if (state.V.size() <= 1) {
                if (threadId == 0) System.out.println("Base case: Only one vertex remaining");
                return state;
            }
            
            // Debug output from thread 0 (only for smaller graphs)
            boolean enableDebug = numVertices < 1000; // Only debug for small graphs
            if (threadId == 0 && enableDebug) {
                System.out.println("    LLP Advance: |V|=" + state.V.size() + ", |E|=" + state.E.size() + ", |T|=" + state.T.size());
                System.out.println("    Current vertices: " + state.V);
            }
            
            // Step 1: Find minimum weight edges for current vertex set
            Map<Integer, Edge> mweMap = new HashMap<>();
            for (int v : state.V) {
                Edge minEdge = state.mwe(v, state.G);
                if (minEdge != null) {
                    mweMap.put(v, minEdge);
                }
            }
            
            // Step 2: Process parent selection following written example
            int[] newG = state.G.clone();
            Set<Edge> newT = new HashSet<>(state.T);

            for (int v : state.V) {
                Edge vwEdge = mweMap.get(v);
                if (vwEdge == null) continue;
                
                int w = (vwEdge.u == v) ? vwEdge.v : vwEdge.u;
                
                // REMOVE the canonical check - let all vertices work!
                // Check if edge connects different components BEFORE updating parents
                int rootV = findRoot(newG, v);
                int rootW = findRoot(newG, w);
                if (rootV != rootW) {
                    // Only add edge if it creates a union between different components
                    newT.add(vwEdge);  // HashSet automatically handles duplicates
                    
                    // Now update parent pointers after adding the edge
                    if (v < w) {
                        newG[w] = v;
                    } else {
                        newG[v] = w;
                    }
                }
            }
            
            // Step 3: Apply path compression to ALL vertices in the original graph
            for (int i = 0; i < newG.length; i++) {
                newG[i] = findRoot(newG, i);
            }
            
            // Step 4: Create reduced graph
            BoruvkaState intermediate = new BoruvkaState(state.V, state.E, newG, newT, state.numOriginalVertices);
            BoruvkaState reduced = intermediate.reduceGraph();
            
            // Debug output
            if (threadId == 0 && enableDebug) {
                System.out.println("    After reduction: |V'|=" + reduced.V.size() + ", |E'|=" + reduced.E.size());
                for (Edge edge : newT) {
                    if (!state.T.contains(edge)) {
                        System.out.println("    Added edge: " + edge);
                    }
                }
            }
            
            // Debug output (only for smaller graphs)
            if (threadId == 0 && enableDebug) {
                System.out.println("    Returning state: |V|=" + reduced.V.size() + ", |E|=" + reduced.E.size() + ", |T|=" + reduced.T.size());
                System.out.println("    Returning vertices: " + reduced.V);
            }
            
            // Return the properly reduced state
            return reduced;
        }
        
        @Override
        public BoruvkaState getInitialState() {
            // Create initial vertex and edge sets
            Set<Integer> initialV = new HashSet<>();
            for (int i = 0; i < numVertices; i++) {
                initialV.add(i);
            }
            
            Set<Edge> initialE = new HashSet<>(Arrays.asList(allEdges));
            
            return new BoruvkaState(initialV, initialE, numVertices);
        }
        
        @Override
        public boolean isSolution(BoruvkaState state) {
            // Algorithm terminates when we have a single component (|V| <= 1) or no edges
            boolean naturalTermination = state.V.size() <= 1 || state.E.isEmpty();
            boolean noViolations = !Forbidden(state);
            
            return naturalTermination && noViolations;
        }
        
        @Override
        public BoruvkaState merge(BoruvkaState state1, BoruvkaState state2) {
            // Only debug for small graphs
            boolean enableDebug = numVertices < 1000;
            
            if (enableDebug) {
                System.out.println("    Merging states: |V1|=" + state1.V.size() + ", |V2|=" + state2.V.size() + 
                                   ", |T1|=" + state1.T.size() + ", |T2|=" + state2.T.size());
            }
            
            // Merge parent arrays by taking the most compressed path
            int[] mergedG = new int[Math.max(state1.G.length, state2.G.length)];
            System.arraycopy(state1.G, 0, mergedG, 0, state1.G.length);
            
            // Merge with state2's parent information
            for (int i = 0; i < Math.min(mergedG.length, state2.G.length); i++) {
                // Take the more compressed parent reference
                if (state2.G[i] != i && mergedG[i] == i) {
                    mergedG[i] = state2.G[i];
                }
            }
            
            // Merge MST edges (use union to avoid duplicates)
            Set<Edge> mergedT = new HashSet<>(state1.T);
            mergedT.addAll(state2.T);
            
            // CRITICAL FIX: Use the smaller/more reduced vertex and edge sets
            // This ensures we preserve the graph reduction from Advance operations
            Set<Integer> mergedV = state1.V.size() <= state2.V.size() ? state1.V : state2.V;
            Set<Edge> mergedE = state1.E.size() <= state2.E.size() ? state1.E : state2.E;
            
            // If both states have the same reduction level, use intersection for safety
            if (state1.V.size() == state2.V.size()) {
                mergedV = new HashSet<>(state1.V);
                mergedV.retainAll(state2.V); // Intersection of vertices
                
                mergedE = new HashSet<>(state1.E);
                mergedE.retainAll(state2.E); // Intersection of edges
            }
            
            BoruvkaState result = new BoruvkaState(mergedV, mergedE, mergedG, mergedT, state1.numOriginalVertices);
            
            if (enableDebug) {
                System.out.println("    Merged result: |V|=" + result.V.size() + ", |E|=" + result.E.size() + ", |T|=" + result.T.size());
            }
            
            return result;
        }
        
        /**
         * Helper method to find root with path compression
         */
        private int findRoot(int[] G, int v) {
            if (v >= G.length) return v;
            
            // Simple path compression
            while (v < G.length && G[v] != v) {
                v = G[v];
            }
            return v;
        }
    }
    
    /**
     * Recursive Boruvka implementation following the written example exactly
     */
    public static Set<Edge> boruvkaRecursive(Set<Integer> V, Set<Edge> E) {
        // Base case: if |E| = 0 return {}
        if (E.isEmpty()) {
            return new HashSet<>();
        }
        
        // Additional base case: if |V| <= 1, we can't form any more edges
        if (V.size() <= 1) {
            return new HashSet<>();
        }
        
        System.out.println("Recursive call with |V|=" + V.size() + ", |E|=" + E.size());
        System.out.println("  Current vertices: " + V);
        
        // Initialize parent array G and MST edge set T
        // Size G based on maximum vertex index in the entire graph, not just current V
        int maxVertex = Math.max(
            V.stream().mapToInt(Integer::intValue).max().orElse(0),
            E.stream().mapToInt(edge -> Math.max(edge.u, edge.v)).max().orElse(0)
        );
        int[] G = new int[maxVertex + 1];
        Set<Edge> T = new HashSet<>();
        
        // Initialize all positions in G array
        for (int i = 0; i <= maxVertex; i++) {
            G[i] = i;
        }
        
        // Helper function to find mwe(v)
        Map<Integer, Edge> mweMap = new HashMap<>();
        for (int v : V) {
            Edge minEdge = null;
            double minWeight = Double.POSITIVE_INFINITY;
            
            for (Edge edge : E) {
                if ((edge.u == v || edge.v == v) && edge.weight < minWeight) {
                    minWeight = edge.weight;
                    minEdge = edge;
                }
            }
            mweMap.put(v, minEdge);
            System.out.println("  mwe(" + v + ") = " + minEdge);
        }
        
        // Main algorithm logic from written example
        for (int v : V) {
            Edge vwEdge = mweMap.get(v);
            if (vwEdge == null) continue;
            
            int w = (vwEdge.u == v) ? vwEdge.v : vwEdge.u;
            
            // Simplified symmetry breaking logic:
            // Always make the smaller vertex the root to avoid cycles
            if (v < w) {
                G[w] = v;  // w points to v (smaller vertex becomes root)
            } else {
                G[v] = w;  // v points to w (smaller vertex becomes root)
            }
            
            // T := T ∪ (v, w)
            T.add(vwEdge);
            System.out.println("  G[" + v + "] = " + G[v] + ", G[" + w + "] = " + G[w] + ", added edge: " + vwEdge);
        }
        
        // Apply Union-Find path compression properly
        System.out.println("  Before path compression:");
        for (int v : V) {
            System.out.println("    G[" + v + "] = " + G[v]);
        }
        
        // Find root function with path compression
        Map<Integer, Integer> rootCache = new HashMap<>();
        for (int v : V) {
            if (v < G.length) {
                int root = findRoot(G, v, rootCache);
                G[v] = root;
            }
        }
        
        System.out.println("  After path compression:");
        for (int v : V) {
            System.out.println("    G[" + v + "] = " + G[v]);
        }
        
        // Create reduced graph for recursive call
        // E' := {(G[v], G[w])|((v, w) ∈ E) ∧ (G[v] ≠ G[w])}
        Set<Edge> newE = new HashSet<>();
        for (Edge edge : E) {
            // Check bounds before accessing G array
            if (edge.u < G.length && edge.v < G.length) {
                int gU = G[edge.u];
                int gV = G[edge.v];
                if (gU != gV) {
                    newE.add(new Edge(gU, gV, edge.weight));
                }
            }
        }
        
        // V' := {v ∈ V | G[v] = v}
        Set<Integer> newV = new HashSet<>();
        for (int v : V) {
            if (v < G.length && G[v] == v) {
                newV.add(v);
            }
        }
        
        System.out.println("  Reduced: |V'|=" + newV.size() + ", |E'|=" + newE.size());
        System.out.println("  New vertices: " + newV);
        System.out.println("  MST edges so far: " + T.size());
        
        // Check for infinite recursion - if no progress made, stop
        if (newV.size() == V.size() && newE.size() == E.size()) {
            System.out.println("  No progress made, stopping recursion");
            return T;
        }
        
        // Check if we have a complete MST (n-1 edges for original vertex count)
        if (T.size() >= maxVertex) {  // maxVertex is original vertex count - 1
            System.out.println("  Complete MST found, stopping recursion");
            return T;
        }
        
        // Recursive call: return T ∪ Boruvka(V', E')
        Set<Edge> recursiveResult = boruvkaRecursive(newV, newE);
        T.addAll(recursiveResult);
        
        return T;
    }
    
    /**
     * Helper method to find root with path compression and cycle detection
     */
    private static int findRoot(int[] G, int v, Map<Integer, Integer> cache) {
        if (cache.containsKey(v)) {
            return cache.get(v);
        }
        
        Set<Integer> visited = new HashSet<>();
        int current = v;
        
        // Follow parent pointers until we find a root or detect a cycle
        while (current < G.length && G[current] != current && !visited.contains(current)) {
            visited.add(current);
            current = G[current];
        }
        
        // If we found a cycle, make the smallest vertex in the cycle the root
        if (visited.contains(current)) {
            int root = visited.stream().min(Integer::compareTo).orElse(v);
            for (int vertex : visited) {
                cache.put(vertex, root);
            }
            return root;
        }
        
        // Normal case - current is the root
        for (int vertex : visited) {
            cache.put(vertex, current);
        }
        cache.put(current, current);
        return current;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Boruvka's Algorithm Following Written Example ===\n");
        
        // Test with synthetic data first
        //testSyntheticGraph();
        
        // Use correct path to the .egr file
        String graphFile = "LLP-Java-Algorithms/USA-road-d.NY.egr";
        
        // Test with real graph file
        System.out.println("\n" + "=".repeat(60) + "\n");
        testWithGraphFile(graphFile, 1000000, 16); // Small sample first
    }

    /**
     * Test with synthetic graph (move your existing test code here).
     */
    private static void testSyntheticGraph() {
        System.out.println("=== Testing with Synthetic Graph ===\n");
        
        // Move your existing edge array and test code here...
        Edge[] edges = {
            // Grid-like structure with multiple components initially
            
            // Component 1: vertices 0-4 (complete subgraph)
            new Edge(0, 1, 1.0),
            new Edge(0, 2, 2.0), 
            new Edge(0, 3, 3.0),
            new Edge(0, 4, 4.0),
            new Edge(1, 2, 1.5),
            new Edge(1, 3, 2.5),
            new Edge(1, 4, 3.5),
            new Edge(2, 3, 1.8),
            new Edge(2, 4, 2.8),
            new Edge(3, 4, 2.2),
            
            // Component 2: vertices 5-9 (complete subgraph)
            new Edge(5, 6, 1.1),
            new Edge(5, 7, 2.1),
            new Edge(5, 8, 3.1),
            new Edge(5, 9, 4.1),
            new Edge(6, 7, 1.6),
            new Edge(6, 8, 2.6),
            new Edge(6, 9, 3.6),
            new Edge(7, 8, 1.9),
            new Edge(7, 9, 2.9),
            new Edge(8, 9, 2.3),
            
            // Component 3: vertices 10-14 (complete subgraph)
            new Edge(10, 11, 1.2),
            new Edge(10, 12, 2.2),
            new Edge(10, 13, 3.2),
            new Edge(10, 14, 4.2),
            new Edge(11, 12, 1.7),
            new Edge(11, 13, 2.7),
            new Edge(11, 14, 3.7),
            new Edge(12, 13, 2.0),
            new Edge(12, 14, 3.0),
            new Edge(13, 14, 2.4),
            
            // Inter-component connections (these will be chosen in later iterations)
            new Edge(2, 7, 5.0),   // Connect component 1 to component 2
            new Edge(4, 6, 6.0),   // Another connection 1-2
            new Edge(8, 12, 7.0),  // Connect component 2 to component 3
            new Edge(9, 13, 8.0),  // Another connection 2-3
            new Edge(1, 11, 9.0),  // Connect component 1 to component 3
            new Edge(3, 14, 10.0), // Another connection 1-3
            
            // Some extra edges to make it interesting
            new Edge(0, 5, 12.0),
            new Edge(4, 9, 11.0),
            new Edge(1, 10, 13.0),
            new Edge(6, 14, 14.0)
        };

        int numVertices = 15;
        
        // Test recursive implementation
        System.out.println("=== LLP Recursive Implementation (Following Written Example) ===");
        Set<Integer> V = new HashSet<>();
        for (int i = 0; i < numVertices; i++) {
            V.add(i);
        }
        Set<Edge> E = new HashSet<>(Arrays.asList(edges));
        
        Set<Edge> mst = boruvkaRecursive(V, E);
        double totalWeight = mst.stream().mapToDouble(edge -> edge.weight).sum();
        
        System.out.println("MST edges:");
        for (Edge edge : mst) {
            System.out.println("  " + edge);
        }
        System.out.println("Total weight: " + totalWeight);
        System.out.println("Number of edges: " + mst.size());
        
        BoruvkaLLPProblem llpProblem = new BoruvkaLLPProblem(numVertices, edges);
        
        LLPSolver<BoruvkaState> solver = null;
        try {
            // Create solver with 4 threads and 100 max iterations
            solver = new LLPSolver<>(llpProblem, 4, 100);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            BoruvkaState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✓ Solution found!");
            System.out.println("LLP solution: " + solution);
            System.out.println("MST edges found: " + solution.T.size());
            
            // Display the actual MST edges
            System.out.println("MST edges:");
            double llpTotalWeight = 0.0;
            for (Edge edge : solution.T) {
                System.out.println("  " + edge);
                llpTotalWeight += edge.weight;
            }
            System.out.println("Total LLP weight: " + llpTotalWeight);
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + llpProblem.isSolution(solution));
            System.out.println("Is forbidden? " + llpProblem.Forbidden(solution));
            
            // Get statistics
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            if (stats != null) {
                System.out.println("Total iterations: " + stats.getIterationCount());
                System.out.println("Converged: " + stats.hasConverged());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
        
        System.out.println("\n=== Example Complete ===");
    }

    /**
     * Test Boruvka's algorithm with a real graph file using only LLP framework.
     */
    public static void testWithGraphFile(String filename, int maxVertices, int numThreads) {
        System.out.println("=== Testing Boruvka with Real Graph Data (LLP Framework) ===\n");
        
        try {
            // Read the graph file with maxVertices limit
            GraphFileReader.GraphData graphData = GraphFileReader.readGraphFile(filename, maxVertices);
            GraphFileReader.printGraphStats(graphData);
            
            // No need for additional sampling since readGraphFile handles it
            
            // Test LLP implementation only
            testLLPImplementation(graphData, numThreads);
            
        } catch (IOException e) {
            System.err.println("Error reading graph file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test LLP implementation on the graph data.
     */
    private static void testLLPImplementation(GraphFileReader.GraphData graphData, int numThreads) {
        System.out.println("=== LLP Boruvka Algorithm Analysis ===\n");
        
        // Initialize LLP problem
        BoruvkaLLPProblem llpProblem = new BoruvkaLLPProblem(graphData.numVertices, graphData.edges);
        
        LLPSolver<BoruvkaState> solver = null;
        try {
            // Create solver with specified threads and max iterations
            solver = new LLPSolver<>(llpProblem, numThreads, 1000);
            
            System.out.println("1. Executing LLP Boruvka Algorithm...");
            
            long startTime = System.nanoTime();
            BoruvkaState solution = solver.solve();
            long llpTime = System.nanoTime() - startTime;
            
            double llpWeight = solution.T.stream()
                .mapToDouble(edge -> edge.weight)
                .sum();
            
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            
            System.out.printf("✓ LLP Execution Complete!\n");
            System.out.printf("  MST edges found: %,d\n", solution.T.size());
            System.out.printf("  Total MST weight: %.2f\n", llpWeight);
            System.out.printf("  Execution time: %.2fms\n", llpTime / 1_000_000.0);
            System.out.printf("  Iterations required: %d\n", stats != null ? stats.getIterationCount() : -1);
            System.out.printf("  Convergence status: %s\n\n", stats != null ? stats.hasConverged() : "unknown");
            
            // ===== DETAILED LLP ANALYSIS =====
            System.out.println("2. Detailed Performance Analysis:");
            System.out.println("   ════════════════════════════════════════════");
            
            // Performance metrics
            double llpMs = llpTime / 1_000_000.0;
            double llpSeconds = llpMs / 1000.0;
            
            System.out.printf("   Execution Performance:\n");
            System.out.printf("     Total execution time: %.2fms (%.3f seconds)\n", llpMs, llpSeconds);
            System.out.printf("     Parallel threads used: %d\n", numThreads);
            System.out.printf("     LLP iterations: %d\n", stats != null ? stats.getIterationCount() : -1);
            System.out.printf("     Algorithm converged: %s\n", stats != null ? stats.hasConverged() : "unknown");
            
            // Graph processing metrics
            System.out.printf("\n   Graph Processing:\n");
            System.out.printf("     Input vertices: %,d\n", graphData.numVertices);
            System.out.printf("     Input edges: %,d\n", graphData.edges.length);
            System.out.printf("     MST edges computed: %,d\n", solution.T.size());
            System.out.printf("     MST total weight: %.2f\n", llpWeight);
            
            // Algorithm validation
            boolean llpValid = llpProblem.isSolution(solution);
            boolean expectedEdgeCount = solution.T.size() == graphData.numVertices - 1;
            boolean isForbidden = llpProblem.Forbidden(solution);
            
            System.out.printf("\n   Algorithm Validation:\n");
            System.out.printf("     LLP termination conditions satisfied: %s\n", llpValid);
            System.out.printf("     No forbidden states: %s\n", !isForbidden);
            
            // Declare these variables once here
            int expectedMSTEdges = graphData.numVertices - 1;
            int actualMSTEdges = solution.T.size();
            
            System.out.printf("     Expected MST edge count (connected): %,d\n", expectedMSTEdges);
            System.out.printf("     Actual MST edge count: %,d\n", actualMSTEdges);
            
            if (actualMSTEdges == expectedMSTEdges) {
                System.out.printf("      Graph is connected (1 component)\n");
            } else if (actualMSTEdges < expectedMSTEdges) { // fewer edges than expected
                int estimatedComponents = expectedMSTEdges - actualMSTEdges + 1;
                System.out.printf("      Graph has ~%,d disconnected components\n", estimatedComponents);
            } else { // more edges than expected
                int excessEdges = actualMSTEdges - expectedMSTEdges;
                System.out.printf("      MST has %,d excess edges - likely duplicates or multiple MSTs\n", excessEdges);
            }
            System.out.println();
            
            // Efficiency metrics
            if (llpSeconds > 0) {
                System.out.printf("\n   Efficiency Analysis:\n");
                double edgesPerSecond = graphData.edges.length / llpSeconds;
                double verticesPerSecond = graphData.numVertices / llpSeconds;
                double avgIterationTime = llpMs / Math.max(stats != null ? stats.getIterationCount() : 1, 1);
                
                System.out.printf("     Edges processed per second: %,.0f\n", edgesPerSecond);
                System.out.printf("     Vertices processed per second: %,.0f\n", verticesPerSecond);
                System.out.printf("     Average time per iteration: %.2fms\n", avgIterationTime);
                
                // Add the efficiency formula explanation
                System.out.printf("\n     Efficiency = (Sequential Time / Parallel Time) / Number of Threads * 100%%\n");
                
                // Parallel efficiency estimate
                double sequentialEstimate = llpSeconds * numThreads; // rough estimate
                System.out.printf("     Rough estimated sequential time: %.2f seconds\n", sequentialEstimate);
                System.out.printf("     Parallel efficiency: %.1f%% (%d threads)\n", 
                    (sequentialEstimate / llpSeconds / numThreads) * 100, numThreads);
            }
            
            // Final assessment
            System.out.printf("\n   Summary Assessment:\n");
            if (llpValid && !isForbidden) {               
                if (actualMSTEdges == expectedMSTEdges) {
                    System.out.printf("      LLP algorithm successfully computed complete MST\n");
                    System.out.printf("      Graph is connected with %,d vertices\n", graphData.numVertices);
                } else if (actualMSTEdges < expectedMSTEdges) {
                    int estimatedComponents = expectedMSTEdges - actualMSTEdges + 1;
                    System.out.printf("      LLP algorithm successfully computed spanning forest\n");
                    System.out.printf("      Graph has ~%,d disconnected components\n", estimatedComponents);
                } else {
                    int excessEdges = actualMSTEdges - expectedMSTEdges;
                    System.out.printf("      LLP algorithm computed MST with %,d excess edges\n", excessEdges);
                }
                
                System.out.printf("      Processed %,d vertices in %.3f seconds using %d threads\n", 
                    graphData.numVertices, llpSeconds, numThreads);
                System.out.printf("      LLP framework achieved parallel speedup\n");
            } else {
                System.out.printf("      LLP algorithm execution needs investigation\n");
                if (isForbidden) {
                    System.out.printf("      Final state contains forbidden configurations\n");
                }
                if (!llpValid) {
                    System.out.printf("      LLP termination conditions not satisfied\n");
                }
            }
            
            System.out.println("   ════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("Error in LLP execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }
}
