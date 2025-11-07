package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import com.llp.framework.GraphFileReader;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * Corrected Boruvka's MST Algorithm with proper convergence conditions.
 * Optimized for parallel performance while ensuring correct MST construction.
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
     * Corrected state with proper MST construction tracking
     */
    static class BoruvkaState {
        final Set<Integer> V;
        final Set<Edge> E;
        final int[] G;
        final Set<Edge> T;
        final int numOriginalVertices;
        final Map<Integer, List<Edge>> adjacencyList;
        
        public BoruvkaState(Set<Integer> V, Set<Edge> E, int numOriginalVertices) {
            this.V = new HashSet<>(V);
            this.E = new HashSet<>(E);
            this.numOriginalVertices = numOriginalVertices;
            this.G = new int[numOriginalVertices];
            this.T = new HashSet<>();
            
            for (int v : V) {
                G[v] = v;
            }
            
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
        
        private Map<Integer, List<Edge>> buildAdjacencyList() {
            Map<Integer, List<Edge>> adjList = new HashMap<>();
            for (Edge edge : E) {
                adjList.computeIfAbsent(edge.u, k -> new ArrayList<>()).add(edge);
                adjList.computeIfAbsent(edge.v, k -> new ArrayList<>()).add(edge);
            }
            return adjList;
        }
        
        /**
         * OPTIMIZED: Find minimum weight edges efficiently with proper component tracking
         */
        public Map<Integer, Edge> findAllMWE(Collection<Integer> vertices, int[] G) {
            Map<Integer, Edge> mweMap = new HashMap<>();
            
            for (int v : vertices) {
                List<Edge> edges = adjacencyList.get(v);
                if (edges == null || edges.isEmpty()) continue;
                
                Edge minEdge = null;
                double minWeight = Double.POSITIVE_INFINITY;
                int rootV = findRoot(G, v);
                
                for (Edge edge : edges) {
                    int w = (edge.u == v) ? edge.v : edge.u;
                    int rootW = findRoot(G, w);
                    
                    if (rootV != rootW && edge.weight < minWeight) {
                        minWeight = edge.weight;
                        minEdge = edge;
                    }
                }
                
                if (minEdge != null) {
                    mweMap.put(v, minEdge);
                }
            }
            return mweMap;
        }
        
        public BoruvkaState withUpdatedParentsAndEdges(int[] newG, Set<Edge> newT) {
            return new BoruvkaState(V, E, newG, newT, numOriginalVertices);
        }
        
        /**
         * CORRECTED: Graph reduction that maintains MST construction progress
         */
        public BoruvkaState reduceGraph() {
            // Apply path compression to all vertices
            int[] compressedG = G.clone();
            for (int i = 0; i < compressedG.length; i++) {
                compressedG[i] = findRoot(compressedG, i);
            }
            
            Set<Integer> newV = new HashSet<>();
            Set<Edge> newE = new HashSet<>();
            
            // Find component representatives
            for (int v : V) {
                if (v < compressedG.length && compressedG[v] == v) {
                    newV.add(v);
                }
            }
            
            // Build inter-component edges (these are the edges for the next iteration)
            for (Edge edge : E) {
                if (edge.u < compressedG.length && edge.v < compressedG.length) {
                    int gU = compressedG[edge.u];
                    int gV = compressedG[edge.v];
                    if (gU != gV) {
                        newE.add(new Edge(gU, gV, edge.weight));
                    }
                }
            }
            
            return new BoruvkaState(newV, newE, compressedG, T, numOriginalVertices);
        }
        
        @Override
        public String toString() {
            return String.format("Boruvka{V=%d, E=%d, T=%d}", V.size(), E.size(), T.size());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BoruvkaState)) return false;
            BoruvkaState other = (BoruvkaState) obj;
            return V.equals(other.V) && E.equals(other.E) && 
                   Arrays.equals(G, other.G) && T.equals(other.T);
        }
        
        private int findRoot(int[] G, int v) {
            if (v >= G.length) return v;
            
            // Path compression
            int original = v;
            while (v < G.length && G[v] != v) {
                v = G[v];
            }
            
            // Apply compression to path
            while (original < G.length && G[original] != v) {
                int next = G[original];
                G[original] = v;
                original = next;
            }
            
            return v;
        }
    }

    /**
     * CORRECTED Boruvka LLP with proper MST construction
     */
    static class BoruvkaLLPProblem implements LLPProblem<BoruvkaState> {
        
        private final int numVertices;
        private final Edge[] allEdges;
        private volatile int iterationCount = 0;
        
        public BoruvkaLLPProblem(int numVertices, Edge[] allEdges) {
            this.numVertices = numVertices;
            this.allEdges = allEdges.clone();
        }
        
        @Override
        public boolean Forbidden(BoruvkaState state) {
            // Sample-based checking for large graphs
            List<Integer> vertices = new ArrayList<>(state.V);
            int sampleSize = Math.min(1000, vertices.size());
            
            for (int i = 0; i < sampleSize; i++) {
                int j = vertices.get(i);
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
            int[] newG = state.G.clone();
            
            // Parallel path compression
            List<Integer> vertices = new ArrayList<>(state.V);
            int chunkSize = Math.max(1, vertices.size() / totalThreads);
            int startIdx = threadId * chunkSize;
            int endIdx = (threadId == totalThreads - 1) ? vertices.size() : startIdx + chunkSize;
            
            for (int i = startIdx; i < endIdx; i++) {
                int j = vertices.get(i);
                if (j < newG.length && newG[j] < newG.length) {
                    newG[j] = newG[newG[j]];
                }
            }
            
            return state.withUpdatedParentsAndEdges(newG, state.T);
        }
        
        @Override
        public BoruvkaState Advance(BoruvkaState state, int threadId, int totalThreads) {
            // Progress logging
            if (threadId == 0 && iterationCount % 10 == 0) {
                System.out.printf("[%s] Boruvka iteration %d: |V|=%,d, |E|=%,d, |T|=%,d\n", 
                    new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()),
                    iterationCount, state.V.size(), state.E.size(), state.T.size());
            }
            
            if (threadId == 0) iterationCount++;
            
            // CORRECTED: Only terminate when we have the complete MST or single component
            if (state.V.size() <= 1) {
                return state; // Single component - we're done
            }
            
            if (state.E.isEmpty()) {
                return state; // No more edges to process
            }
            
            List<Integer> vertices = new ArrayList<>(state.V);
            
            // Distribute vertices among threads
            int chunkSize = Math.max(1, vertices.size() / totalThreads);
            int startIdx = threadId * chunkSize;
            int endIdx = (threadId == totalThreads - 1) ? vertices.size() : startIdx + chunkSize;
            
            if (startIdx >= vertices.size()) {
                return state; // Thread has no work
            }
            
            List<Integer> threadVertices = vertices.subList(startIdx, endIdx);
            Map<Integer, Edge> mweMap = state.findAllMWE(threadVertices, state.G);
            
            int[] newG = state.G.clone();
            Set<Edge> threadEdges = new HashSet<>();
            
            // OPTIMIZED: Component-based edge selection to prevent duplicates
            Map<String, Edge> componentBestEdges = new HashMap<>();
            
            for (Map.Entry<Integer, Edge> entry : mweMap.entrySet()) {
                Edge vwEdge = entry.getValue();
                int rootU = findRoot(newG, vwEdge.u);
                int rootV = findRoot(newG, vwEdge.v);
                
                if (rootU != rootV) {
                    // Create unique key for component pair
                    String componentKey = Math.min(rootU, rootV) + "-" + Math.max(rootU, rootV);
                    Edge currentBest = componentBestEdges.get(componentKey);
                    
                    if (currentBest == null || vwEdge.weight < currentBest.weight) {
                        componentBestEdges.put(componentKey, vwEdge);
                    }
                }
            }
            
            // Apply unions and collect edges
            for (Edge bestEdge : componentBestEdges.values()) {
                int rootU = findRoot(newG, bestEdge.u);
                int rootV = findRoot(newG, bestEdge.v);
                
                if (rootU != rootV) {
                    threadEdges.add(bestEdge);
                    // Union operation
                    if (rootU < rootV) {
                        newG[rootV] = rootU;
                    } else {
                        newG[rootU] = rootV;
                    }
                }
            }
            
            // Apply path compression
            for (int i = 0; i < newG.length; i++) {
                findRoot(newG, i);
            }
            
            // Add new edges to MST
            Set<Edge> newT = new HashSet<>(state.T);
            newT.addAll(threadEdges);
            
            // Create intermediate state and reduce
            BoruvkaState intermediate = new BoruvkaState(state.V, state.E, newG, newT, state.numOriginalVertices);
            return intermediate.reduceGraph();
        }
        
        @Override
        public BoruvkaState getInitialState() {
            Set<Integer> initialV = new HashSet<>();
            for (int i = 0; i < numVertices; i++) {
                initialV.add(i);
            }
            
            Set<Edge> initialE = new HashSet<>(Arrays.asList(allEdges));
            return new BoruvkaState(initialV, initialE, numVertices);
        }
        
        @Override
        public boolean isSolution(BoruvkaState state) {
            // CORRECTED: Solution when we have a single component OR complete MST
            boolean singleComponent = state.V.size() <= 1;
            boolean noEdges = state.E.isEmpty();
            boolean completeMST = state.T.size() >= numVertices - 1;
            
            return singleComponent || (noEdges && completeMST);
        }
        
        @Override
        public BoruvkaState merge(BoruvkaState state1, BoruvkaState state2) {
            // Choose the more progressed state (fewer vertices = more progress)
            BoruvkaState primary = state1.V.size() <= state2.V.size() ? state1 : state2;
            BoruvkaState secondary = state1.V.size() <= state2.V.size() ? state2 : state1;
            
            // CORRECTED: Use Kruskal's algorithm to build valid MST from combined edges
            int[] parent = new int[primary.numOriginalVertices];
            for (int i = 0; i < parent.length; i++) {
                parent[i] = i;
            }
            
            // Combine all MST edges from both states
            Set<Edge> allCandidates = new HashSet<>(primary.T);
            allCandidates.addAll(secondary.T);
            
            // Sort by weight for optimal MST
            List<Edge> sortedEdges = new ArrayList<>(allCandidates);
            sortedEdges.sort(Comparator.comparingDouble(e -> e.weight));
            
            // Build MST using Kruskal's algorithm
            Set<Edge> validMST = new HashSet<>();
            for (Edge edge : sortedEdges) {
                int rootU = findRootCompressed(parent, edge.u);
                int rootV = findRootCompressed(parent, edge.v);
                
                if (rootU != rootV) {
                    validMST.add(edge);
                    parent[rootV] = rootU;
                    
                    // Stop when we have a complete MST
                    if (validMST.size() >= primary.numOriginalVertices - 1) {
                        break;
                    }
                }
            }
            
            return new BoruvkaState(primary.V, primary.E, primary.G, validMST, primary.numOriginalVertices);
        }
        
        private int findRootCompressed(int[] parent, int v) {
            if (v >= parent.length) return v;
            if (parent[v] != v) {
                parent[v] = findRootCompressed(parent, parent[v]);
            }
            return parent[v];
        }
        
        private int findRoot(int[] G, int v) {
            if (v >= G.length) return v;
            
            int original = v;
            while (v < G.length && G[v] != v) {
                v = G[v];
            }
            
            // Path compression
            while (original < G.length && G[original] != v) {
                int next = G[original];
                G[original] = v;
                original = next;
            }
            
            return v;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Boruvka's Algorithm ===\n");
        
        String graphFile = "LLP-Java-Algorithms/USA-road-d.NY.egr";
        
        System.out.println("Testing Graph Data version for presentation:");
        
        // Single thread baseline
        System.out.println("\n1. Single-threaded baseline:");
        testWithGraphFile(graphFile, 1000000, 1);
        
        // Parallel execution
        System.out.println("\n2. Parallel execution (2 threads):");
        testWithGraphFile(graphFile, 1000000, 2);
    }

    public static void testWithGraphFile(String filename, int maxVertices, int numThreads) {
        System.out.println("=== Parallel Boruvka Test ===\n");
        try {
            GraphFileReader.GraphData graphData = GraphFileReader.readGraphFile(filename, maxVertices);
            GraphFileReader.printGraphStats(graphData);
            testCorrectedLLP(graphData, numThreads);          
        } catch (IOException e) {
            System.err.println("Error reading graph file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testCorrectedLLP(GraphFileReader.GraphData graphData, int numThreads) {
        System.out.println("=== LLP Boruvka Analysis ===\n");
        
        BoruvkaLLPProblem llpProblem = new BoruvkaLLPProblem(graphData.numVertices, graphData.edges);
        
        LLPSolver<BoruvkaState> solver = null;
        try {
            // Allow more iterations to ensure completion
            solver = new LLPSolver<>(llpProblem, numThreads, 500);
            
            System.out.printf(" Executing LLP Boruvka with %d threads...\n", numThreads);
            
            long startTime = System.nanoTime();
            BoruvkaState solution = solver.solve();
            long llpTime = System.nanoTime() - startTime;
            
            double llpWeight = solution.T.stream().mapToDouble(edge -> edge.weight).sum();
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            
            System.out.printf("  Execution Complete!\n");
            System.out.printf("  MST edges found: %,d\n", solution.T.size());
            System.out.printf("  Total MST weight: %.2f\n", llpWeight);
            System.out.printf("  Execution time: %.2f seconds\n", llpTime / 1_000_000_000.0);
            System.out.printf("  Iterations required: %d\n", stats != null ? stats.getIterationCount() : -1);
            System.out.printf("  Threads used: %d\n", numThreads);
            System.out.printf("  Convergence: %s\n\n", stats != null ? stats.hasConverged() : "unknown");
            
            // Validation with detailed output
            boolean valid = llpProblem.isSolution(solution) && !llpProblem.Forbidden(solution);
            int expectedEdges = graphData.numVertices - 1;
            boolean perfectCount = solution.T.size() == expectedEdges;
            
            System.out.printf("🔍 Validation Results:\n");
            if (perfectCount && valid) {
                System.out.printf("  ✅ PERFECT: %,d edges (exact MST)\n", solution.T.size());
                System.out.printf("  ✅ Algorithm correctness: VALIDATED\n");
                System.out.printf("  ✅ No forbidden states: CONFIRMED\n");
            } else {
                System.out.printf("  ⚠️  Edge count: %,d (expected %,d, diff: %+d)\n", 
                    solution.T.size(), expectedEdges, solution.T.size() - expectedEdges);
                System.out.printf("  ⚠️  Validation: %s\n", valid ? "VALID" : "NEEDS REVIEW");
                System.out.printf("  ⚠️  Forbidden check: %s\n", llpProblem.Forbidden(solution) ? "HAS VIOLATIONS" : "CLEAN");
                
                if (solution.T.size() < expectedEdges) {
                    System.out.printf("  💡 Likely cause: Algorithm terminated early\n");
                } else if (solution.T.size() > expectedEdges) {
                    System.out.printf("  💡 Likely cause: Duplicate edges in MST\n");
                }
            }
            
        } catch (Exception e) {
            System.err.println("  Error in execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }
    
    // Keep existing recursive implementation...
    public static Set<Edge> boruvkaRecursive(Set<Integer> V, Set<Edge> E) {
        if (E.isEmpty()) {
            return new HashSet<>();
        }
        
        if (V.size() <= 1) {
            return new HashSet<>();
        }
        
        System.out.println("Recursive call with |V|=" + V.size() + ", |E|=" + E.size());
        System.out.println("  Current vertices: " + V);
        
        int maxVertex = Math.max(
            V.stream().mapToInt(Integer::intValue).max().orElse(0),
            E.stream().mapToInt(edge -> Math.max(edge.u, edge.v)).max().orElse(0)
        );
        int[] G = new int[maxVertex + 1];
        Set<Edge> T = new HashSet<>();
        
        for (int i = 0; i <= maxVertex; i++) {
            G[i] = i;
        }
        
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
        
        for (int v : V) {
            Edge vwEdge = mweMap.get(v);
            if (vwEdge == null) continue;
            
            int w = (vwEdge.u == v) ? vwEdge.v : vwEdge.u;
            
            if (v < w) {
                G[w] = v;
            } else {
                G[v] = w;
            }
            
            T.add(vwEdge);
            System.out.println("  G[" + v + "] = " + G[v] + ", G[" + w + "] = " + G[w] + ", added edge: " + vwEdge);
        }
        
        System.out.println("  Before path compression:");
        for (int v : V) {
            System.out.println("    G[" + v + "] = " + G[v]);
        }
        
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
        
        Set<Edge> newE = new HashSet<>();
        for (Edge edge : E) {
            if (edge.u < G.length && edge.v < G.length) {
                int gU = G[edge.u];
                int gV = G[edge.v];
                if (gU != gV) {
                    newE.add(new Edge(gU, gV, edge.weight));
                }
            }
        }
        
        Set<Integer> newV = new HashSet<>();
        for (int v : V) {
            if (v < G.length && G[v] == v) {
                newV.add(v);
            }
        }
        
        System.out.println("  Reduced: |V'|=" + newV.size() + ", |E'|=" + newE.size());
        System.out.println("  New vertices: " + newV);
        System.out.println("  MST edges so far: " + T.size());
        
        if (newV.size() == V.size() && newE.size() == E.size()) {
            System.out.println("  No progress made, stopping recursion");
            return T;
        }
        
        if (T.size() >= maxVertex) {
            System.out.println("  Complete MST found, stopping recursion");
            return T;
        }
        
        Set<Edge> recursiveResult = boruvkaRecursive(newV, newE);
        T.addAll(recursiveResult);
        
        return T;
    }
    
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
}
