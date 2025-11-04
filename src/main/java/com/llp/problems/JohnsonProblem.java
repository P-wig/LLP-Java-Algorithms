package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import java.util.Arrays;

/**
 * Johnson's All-Pairs Shortest Path Algorithm using the LLP framework.
 * 
 * Problem Description:
 * Johnson's algorithm finds shortest paths between all pairs of vertices in a
 * sparse, weighted, directed graph. It combines edge reweighting with Bellman-Ford
 * and Dijkstra's algorithms to efficiently handle negative weights while maintaining
 * good performance on sparse graphs.
 * 
 * LLP Implementation Strategy:
 * - State: Distance matrix, reweighting function, and algorithm phase
 * - Forbidden: Distance estimates that violate triangle inequality
 * - Advance: Multi-phase execution (reweighting, distance computation, adjustment)
 * - Ensure: Fix triangle inequality violations using relaxation
 * - Parallelism: Distribute vertices/edges across threads per phase
 */
public class JohnsonProblem implements LLPProblem<JohnsonProblem.JohnsonState> {
    
    // Algorithm phases
    public enum Phase { 
        REWEIGHTING, 
        COMPUTING_DISTANCES, 
        ADJUSTING, 
        COMPLETE 
    }
    
    // Edge representation
    public static class Edge {
        public final int from, to;
        public final int weight;
        
        public Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
        
        @Override
        public String toString() {
            return String.format("%d->%d(%d)", from, to, weight);
        }
    }
    
    /**
     * State class representing the current Johnson's algorithm computation state.
     */
    public static class JohnsonState {
        public final int n;                          // Number of vertices
        public final Edge[] edges;                   // Graph edges (immutable)
        public final int[][] distances;             // All-pairs distance matrix
        public final int[] h;                        // Reweighting function values
        public final boolean[] verticesComputed;    // Which source vertices are complete
        public final Phase currentPhase;            // Current algorithm phase
        
        private static final int INF = Integer.MAX_VALUE;
        
        /**
         * Creates initial state with given vertices and edges, initializes distance matrix.
         */
        public JohnsonState(int numVertices, Edge[] edges) {
            this.n = numVertices;
            this.edges = Arrays.copyOf(edges, edges.length);
            this.distances = new int[n][n];
            this.h = new int[n];
            this.verticesComputed = new boolean[n];
            this.currentPhase = Phase.REWEIGHTING;
            
            // Initialize distance matrix
            for (int i = 0; i < n; i++) {
                Arrays.fill(distances[i], INF);
                distances[i][i] = 0; // Distance to self is zero
            }
            
            // Add direct edge distances
            for (Edge edge : edges) {
                distances[edge.from][edge.to] = edge.weight;
            }
            
            // Initialize h-values and computed flags
            Arrays.fill(h, 0);
            Arrays.fill(verticesComputed, false);
        }
        
        /**
         * Copy constructor for creating new states with updated values.
         */
        public JohnsonState(int n, Edge[] edges, int[][] distances, int[] h, 
                           boolean[] verticesComputed, Phase currentPhase) {
            this.n = n;
            this.edges = edges; // Share immutable edges
            this.distances = copyMatrix(distances);
            this.h = Arrays.copyOf(h, n);
            this.verticesComputed = Arrays.copyOf(verticesComputed, n);
            this.currentPhase = currentPhase;
        }
        
        /**
         * Creates new state with updated distance matrix.
         */
        public JohnsonState withDistances(int[][] newDistances) {
            return new JohnsonState(n, edges, newDistances, h, verticesComputed, currentPhase);
        }
        
        /**
         * Creates new state with updated h-values.
         */
        public JohnsonState withH(int[] newH) {
            return new JohnsonState(n, edges, distances, newH, verticesComputed, currentPhase);
        }
        
        /**
         * Creates new state with updated phase.
         */
        public JohnsonState withPhase(Phase newPhase) {
            return new JohnsonState(n, edges, distances, h, verticesComputed, newPhase);
        }
        
        /**
         * Creates new state with vertex marked as computed.
         */
        public JohnsonState withVertexComputed(int vertex) {
            boolean[] newComputed = Arrays.copyOf(verticesComputed, n);
            newComputed[vertex] = true;
            return new JohnsonState(n, edges, distances, h, newComputed, currentPhase);
        }
        
        /**
         * Returns true if all vertices have been processed in the current phase.
         */
        public boolean isPhaseComplete() {
            switch (currentPhase) {
                case REWEIGHTING:
                    // Reweighting is complete when h-values are computed
                    return !Arrays.equals(h, new int[n]);
                case COMPUTING_DISTANCES:
                    // Distance computation complete when all vertices processed
                    for (boolean computed : verticesComputed) {
                        if (!computed) return false;
                    }
                    return true;
                case ADJUSTING:
                case COMPLETE:
                    return true;
                default:
                    return false;
            }
        }
        
        /**
         * Returns the reweighted edge weight using h-values.
         */
        public int getReweightedWeight(int from, int to, int originalWeight) {
            return originalWeight + h[from] - h[to];
        }
        
        /**
         * Helper method to copy a 2D array.
         */
        private static int[][] copyMatrix(int[][] matrix) {
            int[][] copy = new int[matrix.length][];
            for (int i = 0; i < matrix.length; i++) {
                copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
            }
            return copy;
        }
        
        @Override
        public String toString() {
            int computed = 0;
            for (boolean c : verticesComputed) if (c) computed++;
            return String.format("JohnsonState{n=%d, phase=%s, computed=%d/%d}", 
                               n, currentPhase, computed, n);
        }
    }

    private final int numVertices;
    private final Edge[] edges;

    /**
     * Creates a new Johnson's algorithm problem instance with given vertices and edges.
     */
    public JohnsonProblem(int numVertices, int[][] edgeData) {
        this.numVertices = numVertices;
        this.edges = new Edge[edgeData.length];
        
        // Convert edge data to Edge objects
        for (int i = 0; i < edgeData.length; i++) {
            edges[i] = new Edge(edgeData[i][0], edgeData[i][1], edgeData[i][2]);
        }
    }
    
    /**
     * Returns the initial state with distance matrix initialized and phase set to reweighting.
     */
    @Override
    public JohnsonState getInitialState() {
        return new JohnsonState(numVertices, edges);
    }

    /**
     * Returns true if any distance estimates violate triangle inequality constraints.
     */
    @Override
    public boolean Forbidden(JohnsonState state) {
        // Check triangle inequality: distance[i][j] <= distance[i][k] + distance[k][j]
        for (int i = 0; i < state.n; i++) {
            for (int j = 0; j < state.n; j++) {
                if (state.distances[i][j] < Double.POSITIVE_INFINITY) {
                    for (int k = 0; k < state.n; k++) {
                        if (state.distances[i][k] < Double.POSITIVE_INFINITY && 
                            state.distances[k][j] < Double.POSITIVE_INFINITY) {
                            // Check if triangle inequality is violated
                            if (state.distances[i][j] > state.distances[i][k] + state.distances[k][j] + 1e-9) {
                                return true; // Found violation - state is forbidden
                            }
                        }
                    }
                }
            }
        }
        return false; 
    }
    
    /**
     * Fixes distance violations using Floyd-Warshall relaxation, distributed across threads.
     */
    @Override
    public JohnsonState Ensure(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean changed = false;
        
        // Distribute vertices among threads for parallel relaxation
        for (int i = threadId; i < state.n; i += totalThreads) {
            for (int j = 0; j < state.n; j++) {
                for (int k = 0; k < state.n; k++) {
                    if (newDistances[i][k] < Integer.MAX_VALUE && 
                        newDistances[k][j] < Integer.MAX_VALUE) {
                        int newDist = newDistances[i][k] + newDistances[k][j];
                        if (newDist < newDistances[i][j]) {
                            newDistances[i][j] = newDist;
                            changed = true;
                        }
                    }
                }
            }
        }
        
        return changed ? state.withDistances(newDistances) : state;
    }
    
    /**
     * Makes progress based on current phase: reweighting, computing distances, or adjusting.
     */
    @Override
    public JohnsonState Advance(JohnsonState state, int threadId, int totalThreads) {
        switch (state.currentPhase) {
            case REWEIGHTING:
                return advanceReweighting(state, threadId, totalThreads);
            case COMPUTING_DISTANCES:
                return advanceDistanceComputation(state, threadId, totalThreads);
            case ADJUSTING:
                return advanceAdjustment(state, threadId, totalThreads);
            default:
                return state; 
        }
    }
    
    /**
     * Computes h-values using Bellman-Ford for edge reweighting.
     */
    private JohnsonState advanceReweighting(JohnsonState state, int threadId, int totalThreads) {
        // Use Bellman-Ford relaxation for h-values
        int[] newH = Arrays.copyOf(state.h, state.n);
        
        // Distribute edges among threads for parallel relaxation
        for (int i = threadId; i < state.edges.length; i += totalThreads) {
            Edge edge = state.edges[i];
            if (newH[edge.from] + edge.weight < newH[edge.to]) {
                newH[edge.to] = newH[edge.from] + edge.weight;
            }
        }
        
        // Check if reweighting is complete 
        boolean complete = !Arrays.equals(newH, state.h) || threadId == 0;
        
        JohnsonState newState = state.withH(newH);
        return complete ? newState.withPhase(Phase.COMPUTING_DISTANCES) : newState;
    }
    
    /**
     * Runs Dijkstra from each source vertex on reweighted graph.
     */
    private JohnsonState advanceDistanceComputation(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean[] newComputed = Arrays.copyOf(state.verticesComputed, state.n);
        
        // Distribute source vertices among threads
        for (int source = threadId; source < state.n; source += totalThreads) {
            if (!state.verticesComputed[source]) {
                // Run simplified Dijkstra from this source
                runDijkstra(state, source, newDistances[source]);
                newComputed[source] = true;
            }
        }
        
        JohnsonState newState = new JohnsonState(state.n, state.edges, newDistances, 
                                                state.h, newComputed, state.currentPhase);
        
        // Check if all vertices are computed
        if (newState.isPhaseComplete()) {
            return newState.withPhase(Phase.ADJUSTING);
        }
        return newState;
    }
    
    /**
     * Adjusts distances back using h-values to get final shortest paths.
     */
    private JohnsonState advanceAdjustment(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        
        // Distribute distance adjustments among threads
        for (int i = threadId; i < state.n; i += totalThreads) {
            for (int j = 0; j < state.n; j++) {
                if (newDistances[i][j] < Integer.MAX_VALUE) {
                    newDistances[i][j] = newDistances[i][j] - state.h[i] + state.h[j];
                }
            }
        }
        
        JohnsonState finalState = state.withDistances(newDistances);
        return finalState.withPhase(Phase.COMPLETE);
    }
    
    /**
     * Simplified Dijkstra implementation for computing shortest paths from a source.
     */
    private void runDijkstra(JohnsonState state, int source, int[] distances) {
        Arrays.fill(distances, Integer.MAX_VALUE);
        distances[source] = 0;
        
        boolean[] visited = new boolean[state.n];
        
        for (int count = 0; count < state.n; count++) {
            int u = -1;
            int minDist = Integer.MAX_VALUE;
            
            // Find minimum distance unvisited vertex
            for (int v = 0; v < state.n; v++) {
                if (!visited[v] && distances[v] < minDist) {
                    minDist = distances[v];
                    u = v;
                }
            }
            
            if (u == -1) {
                break; // No more reachable vertices
            }
            
            visited[u] = true;
            
            // Relax edges from u using reweighted weights
            for (Edge edge : state.edges) {
                if (edge.from == u && !visited[edge.to]) {
                    int reweightedWeight = state.getReweightedWeight(edge.from, edge.to, edge.weight);
                    if (distances[u] + reweightedWeight < distances[edge.to]) {
                        distances[edge.to] = distances[u] + reweightedWeight;
                    }
                }
            }
        }
    }
    
    /**
     * Returns true if all shortest paths are correctly computed and algorithm is complete.
     */
    @Override
    public boolean isSolution(JohnsonState state) {
        return state.currentPhase == Phase.COMPLETE && !Forbidden(state);
    }
    
    /**
     * Merges results from parallel threads based on current algorithm phase.
     */
    @Override
    public JohnsonState merge(JohnsonState state1, JohnsonState state2) {
        // Take the more advanced phase
        Phase mergedPhase = state1.currentPhase.ordinal() >= state2.currentPhase.ordinal() ? 
                           state1.currentPhase : state2.currentPhase;
        
        // Merge distance matrices (take minimum distances)
        int[][] mergedDistances = new int[state1.n][state1.n];
        for (int i = 0; i < state1.n; i++) {
            for (int j = 0; j < state1.n; j++) {
                mergedDistances[i][j] = Math.min(state1.distances[i][j], state2.distances[i][j]);
            }
        }
        
        // Merge h-values 
        int[] mergedH = new int[state1.n];
        for (int i = 0; i < state1.n; i++) {
            mergedH[i] = (state1.h[i] != 0) ? state1.h[i] : state2.h[i];
        }
        
        // Merge computed vertices 
        boolean[] mergedComputed = new boolean[state1.n];
        for (int i = 0; i < state1.n; i++) {
            mergedComputed[i] = state1.verticesComputed[i] || state2.verticesComputed[i];
        }
        
        return new JohnsonState(state1.n, state1.edges, mergedDistances, mergedH, mergedComputed, mergedPhase);
    }

    /**
     * Main method demonstrating Johnson's algorithm with test cases.
     */
    public static void main(String[] args) {
        System.out.println("=== Johnson's All-Pairs Shortest Path Example ===\n");
        
        // Test Case 1: Simple graph with negative weights
        testCase1();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 2: Larger graph
        testCase2();
    }

    /**
     * Test case 1: Small graph with negative edge weights.
     */
    private static void testCase1() {
        System.out.println("Test Case 1: Small Graph with Negative Weights");
        System.out.println("----------------------------------------------");
        
        // Graph: 0->1(3), 0->2(8), 1->3(-4), 2->1(7), 3->0(2)
        int[][] edges = {
            {0, 1, 3},   // 0 -> 1 with weight 3
            {0, 2, 8},   // 0 -> 2 with weight 8
            {1, 3, -4},  // 1 -> 3 with weight -4 (negative!)
            {2, 1, 7},   // 2 -> 1 with weight 7
            {3, 0, 2}    // 3 -> 0 with weight 2
        };
        
        printGraph(4, edges);
        
        int numThreads = 4;
        int maxIterations = 100;
        
        JohnsonProblem problem = new JohnsonProblem(4, edges);
        JohnsonState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected: All-pairs shortest paths computed");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Test case 2: Larger graph to test scalability.
     */
    private static void testCase2() {
        System.out.println("Test Case 2: Larger Graph");
        System.out.println("-------------------------");
        
        // 5-vertex graph with mixed positive/negative weights
        int[][] edges = {
            {0, 1, 4}, {0, 2, 2},
            {1, 2, -3}, {1, 3, 2}, {1, 4, 4},
            {2, 3, 4}, {2, 4, 5},
            {3, 4, -1},
            {4, 0, 3}
        };
        
        printGraph(5, edges);
        
        int numThreads = 6;
        int maxIterations = 100;
        
        JohnsonProblem problem = new JohnsonProblem(5, edges);
        JohnsonState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected: All-pairs shortest paths with negative edge handling");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Helper method to print graph structure.
     */
    private static void printGraph(int vertices, int[][] edges) {
        System.out.println("Graph structure:");
        System.out.println("  Vertices: " + vertices);
        System.out.println("  Edges:");
        for (int[] edge : edges) {
            System.out.printf("    %d -> %d (weight: %d)\n", edge[0], edge[1], edge[2]);
        }
        System.out.println();
    }

    /**
     * Solves Johnson's algorithm using the LLP framework and prints results.
     */
    private static void solveProblem(JohnsonProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<JohnsonState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            JohnsonState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\nSolution found!");
            printSolution(solution);
            System.out.println("Execution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            System.out.println("Final phase: " + solution.currentPhase);
            
            // Get statistics
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            if (stats != null) {
                System.out.println("Total iterations: " + stats.getIterationCount());
                System.out.println("Converged: " + stats.hasConverged());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prints the all-pairs shortest distance matrix.
     */
    private static void printSolution(JohnsonState solution) {
        System.out.println("All-pairs shortest distances:");
        System.out.print("     ");
        for (int j = 0; j < solution.n; j++) {
            System.out.printf("%8d", j);
        }
        System.out.println();
        
        for (int i = 0; i < solution.n; i++) {
            System.out.printf("%2d: ", i);
            for (int j = 0; j < solution.n; j++) {
                if (solution.distances[i][j] == Integer.MAX_VALUE) {
                    System.out.printf("%8s", "∞");
                } else {
                    System.out.printf("%8d", solution.distances[i][j]);
                }
            }
            System.out.println();
        }
    }
}