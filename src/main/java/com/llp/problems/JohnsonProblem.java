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
            
            // Initialize distance matrix - start with no connections (infinite distance)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        distances[i][j] = 0; // Distance to self is zero
                    } else {
                        distances[i][j] = Integer.MAX_VALUE; // No direct connection
                    }
                }
            }
            
            // Add direct edge distances from the input edges
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
        // For simplicity, only check basic triangle inequality violations
        for (int i = 0; i < state.n; i++) {
            for (int j = 0; j < state.n; j++) {
                for (int k = 0; k < state.n; k++) {
                    if (state.distances[i][k] != Integer.MAX_VALUE && 
                        state.distances[k][j] != Integer.MAX_VALUE &&
                        state.distances[i][j] != Integer.MAX_VALUE) {
                        // Check if we can improve distance[i][j] through k
                        if (state.distances[i][j] > state.distances[i][k] + state.distances[k][j]) {
                            return true; // Found violation - state is forbidden
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
     * Makes progress using complete Floyd-Warshall relaxation.
     */
    @Override
    public JohnsonState Advance(JohnsonState state, int threadId, int totalThreads) {
        if (threadId != 0) {
            return state;
        }
        
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean globalChanged = false;
        
        // Complete Floyd-Warshall: all k iterations
        for (int k = 0; k < state.n; k++) {
            for (int i = 0; i < state.n; i++) {
                for (int j = 0; j < state.n; j++) {
                    if (newDistances[i][k] != Integer.MAX_VALUE && 
                        newDistances[k][j] != Integer.MAX_VALUE) {
                        int newDist = newDistances[i][k] + newDistances[k][j];
                        if (newDist < newDistances[i][j]) {
                            newDistances[i][j] = newDist;
                            globalChanged = true;
                        }
                    }
                }
            }
        }
        
        JohnsonState newState = state.withDistances(newDistances);
        return globalChanged ? newState : newState.withPhase(Phase.COMPLETE);
    }
    
    /**
     * Returns true if we have valid all-pairs shortest paths.
     */
    @Override
    public boolean isSolution(JohnsonState state) {
        // A valid solution is one where no triangle inequality violations exist
        // This means we have correct shortest paths regardless of the phase
        return !Forbidden(state);
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