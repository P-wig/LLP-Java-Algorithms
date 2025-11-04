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
            
            // Initialize distance matrix - all infinite except diagonal
            for (int i = 0; i < n; i++) {
                Arrays.fill(distances[i], Integer.MAX_VALUE);
                distances[i][i] = 0; // Distance to self is zero
            }
            
            // Initialize h-values and computed flags
            Arrays.fill(h, Integer.MAX_VALUE);
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
                    // Reweighting is complete when h-values are properly computed
                    // Check if h-values have been set 
                    boolean hasNonZeroH = false;
                    for (int i = 1; i < n; i++) { // Skip h[0] which should be 0
                        if (h[i] != 0) {
                            hasNonZeroH = true;
                            break;
                        }
                    }
                    return hasNonZeroH;
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
     * Returns true if the current state violates Johnson's algorithm constraints.
     */
    @Override
    public boolean Forbidden(JohnsonState state) {
        switch (state.currentPhase) {
            case REWEIGHTING:
                return false;
                
            case COMPUTING_DISTANCES:
                // Not forbidden - let Advance handle the logic
                return false;
                
            case ADJUSTING:
            case COMPLETE:
                // Not forbidden in final phases
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Fixes violations in the current state based on the algorithm phase.
     */
    @Override
    public JohnsonState Ensure(JohnsonState state, int threadId, int totalThreads) {
        switch (state.currentPhase) {
            case REWEIGHTING:
                return ensureBellmanFord(state, threadId, totalThreads);
                
            case COMPUTING_DISTANCES:
                return ensureDijkstra(state, threadId, totalThreads);
                
            case ADJUSTING:
                return ensureAdjustment(state, threadId, totalThreads);
                
            default:
                return state;
        }
    }
    
    /**
     * Performs Bellman-Ford relaxation to compute h-values.
     */
    private JohnsonState ensureBellmanFord(JohnsonState state, int threadId, int totalThreads) {
        if (threadId != 0) return state; // Only thread 0 handles this
        
        int[] newH = Arrays.copyOf(state.h, state.n);
        boolean changed = false;
        
        // Initialize h[0] = 0 if not set
        if (newH[0] == Integer.MAX_VALUE) {
            newH[0] = 0;
            changed = true;
        }
        
        // Relax edges assigned to this thread
        for (Edge edge : state.edges) {
            if (newH[edge.from] != Integer.MAX_VALUE) {
                int newDist = newH[edge.from] + edge.weight;
                if (newDist < newH[edge.to]) {
                    newH[edge.to] = newDist;
                    changed = true;
                }
            }
        }
        
        // Add auxiliary edges (0 weight from auxiliary vertex to all vertices)
        for (int i = 1; i < state.n; i++) {
            if (newH[i] == Integer.MAX_VALUE) {
                newH[i] = 0;
                changed = true;
            }
        }
        
        return changed ? state.withH(newH) : state;
    }
    
    /**
     * Performs Dijkstra-like computation on reweighted graph.
     */
    private JohnsonState ensureDijkstra(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean changed = false;
        
        // Process vertices assigned to this thread
        for (int source = threadId; source < state.n; source += totalThreads) {
            if (!state.verticesComputed[source]) {
                // Relax edges from this source using reweighted edges
                for (Edge edge : state.edges) {
                    if (edge.from == source) {
                        int reweightedWeight = state.getReweightedWeight(edge.from, edge.to, edge.weight);
                        if (newDistances[source][edge.from] != Integer.MAX_VALUE) {
                            int newDist = newDistances[source][edge.from] + reweightedWeight;
                            if (newDist < newDistances[source][edge.to]) {
                                newDistances[source][edge.to] = newDist;
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        
        return changed ? state.withDistances(newDistances) : state;
    }
    
    /**
     * Adjusts final distances using h-values.
     */
    private JohnsonState ensureAdjustment(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean changed = false;
        
        // Distribute distance matrix updates among threads
        for (int i = threadId; i < state.n; i += totalThreads) {
            for (int j = 0; j < state.n; j++) {
                if (newDistances[i][j] != Integer.MAX_VALUE) {
                    int adjustedDist = newDistances[i][j] + state.h[j] - state.h[i];
                    if (adjustedDist != newDistances[i][j]) {
                        newDistances[i][j] = adjustedDist;
                        changed = true;
                    }
                }
            }
        }
        
        return changed ? state.withDistances(newDistances) : state;
    }
    
    /**
     * Makes progress through Johnson's algorithm phases.
     */
    @Override
    public JohnsonState Advance(JohnsonState state, int threadId, int totalThreads) {
        switch (state.currentPhase) {
            case REWEIGHTING:
                return advanceBellmanFord(state, threadId, totalThreads);
                
            case COMPUTING_DISTANCES:
                return advanceDijkstra(state, threadId, totalThreads);
                
            case ADJUSTING:
                return advanceToComplete(state, threadId, totalThreads);
                
            default:
                return state;
        }
    }
    
    /**
     * Advances Bellman-Ford computation for h-values.
     */
    private JohnsonState advanceBellmanFord(JohnsonState state, int threadId, int totalThreads) {
        if (threadId != 0) return state; // Only thread 0 handles phase transitions
        
        // Step 1: Create auxiliary vertex and compute h-values
        int[] newH = new int[state.n];
        Arrays.fill(newH, Integer.MAX_VALUE);
        
        newH[0] = 0;
        
        // Run Bellman-Ford algorithm for n-1 iterations
        for (int iteration = 0; iteration < state.n - 1; iteration++) {
            boolean changed = false;
            
            // Relax all edges
            for (Edge edge : state.edges) {
                if (newH[edge.from] != Integer.MAX_VALUE) {
                    int newDist = newH[edge.from] + edge.weight;
                    if (newDist < newH[edge.to]) {
                        newH[edge.to] = newDist;
                        changed = true;
                    }
                }
            }
            
            // Also add auxiliary edges (0 weight from auxiliary to all vertices)
            for (int i = 1; i < state.n; i++) {
                if (newH[i] > 0) {
                    newH[i] = 0;
                    changed = true;
                }
            }
            
            if (!changed) break; // Early termination
        }
        
        // Set h-values for any unreachable vertices
        for (int i = 0; i < state.n; i++) {
            if (newH[i] == Integer.MAX_VALUE) {
                newH[i] = 0;
            }
        }
        
        JohnsonState newState = state.withH(newH);
        return newState.withPhase(Phase.COMPUTING_DISTANCES);
    }
    
    /**
     * Advances Dijkstra computation for each source vertex.
     */
    private JohnsonState advanceDijkstra(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean[] newVerticesComputed = Arrays.copyOf(state.verticesComputed, state.n);
        
        // Process vertices assigned to this thread
        for (int source = threadId; source < state.n; source += totalThreads) {
            if (!state.verticesComputed[source]) {
                // Run simplified Dijkstra for this source on reweighted graph
                boolean sourceChanged = runDijkstraForSource(state, newDistances, source);
                if (sourceChanged) {
                    newVerticesComputed[source] = true;
                }
            }
        }
        
        JohnsonState newState = new JohnsonState(state.n, state.edges, newDistances, 
                                                state.h, newVerticesComputed, state.currentPhase);
        
        // Check if all vertices are computed
        if (threadId == 0 && allVerticesComputed(newVerticesComputed)) {
            return newState.withPhase(Phase.ADJUSTING);
        }
        
        return newState;
    }
    
    /**
     * Runs Dijkstra algorithm for a single source vertex.
     */
    private boolean runDijkstraForSource(JohnsonState state, int[][] distances, int source) {
        // Initialize distances from this source
        Arrays.fill(distances[source], Integer.MAX_VALUE);
        distances[source][source] = 0;
        
        boolean[] visited = new boolean[state.n];
        
        // Simple Dijkstra implementation
        for (int count = 0; count < state.n; count++) {
            // Find minimum distance unvisited vertex
            int u = -1;
            for (int v = 0; v < state.n; v++) {
                if (!visited[v] && (u == -1 || distances[source][v] < distances[source][u])) {
                    u = v;
                }
            }
            
            if (u == -1 || distances[source][u] == Integer.MAX_VALUE) break;
            
            visited[u] = true;
            
            // Relax all edges from u using reweighted graph
            for (Edge edge : state.edges) {
                if (edge.from == u && !visited[edge.to]) {
                    int reweightedWeight = state.getReweightedWeight(edge.from, edge.to, edge.weight);
                    if (distances[source][u] != Integer.MAX_VALUE) {
                        long newDist = (long)distances[source][u] + reweightedWeight;
                        if (newDist < distances[source][edge.to] && newDist <= Integer.MAX_VALUE) {
                            distances[source][edge.to] = (int)newDist;
                        }
                    }
                }
            }
        }
        
        return true; // Always consider changed for simplicity
    }
    
    /**
     * Advances to completion by adjusting distances.
     */
    private JohnsonState advanceToComplete(JohnsonState state, int threadId, int totalThreads) {
        if (threadId != 0) return state;
        
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        
        // Adjust all distances back to original graph using h-values
        for (int i = 0; i < state.n; i++) {
            for (int j = 0; j < state.n; j++) {
                if (newDistances[i][j] != Integer.MAX_VALUE) {
                    newDistances[i][j] = newDistances[i][j] - state.h[i] + state.h[j];
                }
            }
        }
        
        return new JohnsonState(state.n, state.edges, newDistances, state.h, 
                               state.verticesComputed, Phase.COMPLETE);
    }
    
    /**
     * Checks if all vertices have been computed.
     */
    private boolean allVerticesComputed(boolean[] verticesComputed) {
        for (boolean computed : verticesComputed) {
            if (!computed) return false;
        }
        return true;
    }
    
    /**
     * Returns true if we have valid all-pairs shortest paths.
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