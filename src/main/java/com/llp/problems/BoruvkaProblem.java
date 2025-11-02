package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.*;

/**
 * Boruvka's Minimum Spanning Tree Algorithm using the LLP framework.
 * Following the written recursive example structure.
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
     * State following the written example structure with parent array G and edge set T
     */
    static class BoruvkaState {
        final Set<Integer> V;         // Current vertex set  
        final Set<Edge> E;           // Current edge set
        final int[] G;               // Parent array from written example
        final Set<Edge> T;           // MST edges (T from written example)
        final int numOriginalVertices; // For array sizing
        
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
        }
        
        public BoruvkaState(Set<Integer> V, Set<Edge> E, int[] G, Set<Edge> T, int numOriginalVertices) {
            this.V = new HashSet<>(V);
            this.E = new HashSet<>(E);
            this.G = G.clone();
            this.T = new HashSet<>(T);
            this.numOriginalVertices = numOriginalVertices;
        }
        
        /**
         * Find minimum weight edge from vertex v (mwe(v) from written example)
         */
        public Edge mwe(int v) {
            Edge minEdge = null;
            double minWeight = Double.POSITIVE_INFINITY;
            
            for (Edge edge : E) {
                if ((edge.u == v || edge.v == v) && edge.weight < minWeight) {
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
    }

    /**
     * Boruvka LLP implementation following the written recursive example
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
            // Base case from written example: if |E| = 0 return {}
            if (state.E.isEmpty()) {
                return state;
            }
            
            // Additional base case: if |V| <= 1, we're done
            if (state.V.size() <= 1) {
                return state;
            }
            
            // Debug output from thread 0
            if (threadId == 0) {
                System.out.println("    LLP Advance: |V|=" + state.V.size() + ", |E|=" + state.E.size() + ", |T|=" + state.T.size());
                System.out.println("    Current vertices: " + state.V);
            }
            
            // Step 1: Find minimum weight edges for current vertex set
            Map<Integer, Edge> mweMap = new HashMap<>();
            for (int v : state.V) {
                Edge minEdge = state.mwe(v);
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
                
                // Simplified parent selection (like recursive version)
                if (v < w) {
                    newG[w] = v;
                } else {
                    newG[v] = w;
                }
                
                newT.add(vwEdge);
            }
            
            // Step 3: Apply path compression to ALL vertices in the original graph
            for (int i = 0; i < newG.length; i++) {
                newG[i] = findRoot(newG, i);
            }
            
            // Step 4: Create reduced graph
            BoruvkaState intermediate = new BoruvkaState(state.V, state.E, newG, newT, state.numOriginalVertices);
            BoruvkaState reduced = intermediate.reduceGraph();
            
            // Debug output
            if (threadId == 0) {
                System.out.println("    After reduction: |V'|=" + reduced.V.size() + ", |E'|=" + reduced.E.size());
                for (Edge edge : newT) {
                    if (!state.T.contains(edge)) {
                        System.out.println("    Added edge: " + edge);
                    }
                }
            }
            
            // Debug output for returned state
            if (threadId == 0) {
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
            // Debug output to trace merge operations
            System.out.println("    Merging states: |V1|=" + state1.V.size() + ", |V2|=" + state2.V.size() + 
                               ", |T1|=" + state1.T.size() + ", |T2|=" + state2.T.size());
            
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
            
            System.out.println("    Merged result: |V|=" + result.V.size() + ", |E|=" + result.E.size() + ", |T|=" + result.T.size());
            
            return result;
        }
        
        /**
         * Helper method to find root with path compression
         */
        private int findRoot(int[] G, int v) {
            if (v >= G.length) return v;
            
            // Use iterative approach to avoid stack overflow
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
                // Apply path compression to all visited vertices
                for (int vertex : visited) {
                    if (vertex < G.length) {
                        G[vertex] = root;
                    }
                }
                return root;
            }
            
            // Normal case - current is the root, apply path compression
            for (int vertex : visited) {
                if (vertex < G.length) {
                    G[vertex] = current;
                }
            }
            
            return current;
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
        
        // Test graph
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
        
        // Also test with different thread counts for performance analysis
        System.out.println("\n=== Performance Comparison ===");
        testPerformance(llpProblem, numVertices, edges);
    }

    /**
     * Additional testing cases for performance comparison with different thread counts.
     * Use this for presentations and performance analysis assignments.
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
                
                double totalWeight = solution.T.stream()
                    .mapToDouble(edge -> edge.weight)
                    .sum();
                
                LLPSolver.ExecutionStats stats = solver.getExecutionStats();
                
                System.out.printf("Threads: %d | Time: %.2fms | Weight: %.1f | Edges: %d | Iterations: %d | Valid: %s%n",
                    threads,
                    (endTime - startTime) / 1_000_000.0,
                    totalWeight,
                    solution.T.size(),
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
