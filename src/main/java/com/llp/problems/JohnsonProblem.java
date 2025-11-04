package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import java.util.Arrays;

/**
 * SIMPLIFIED Johnson's Algorithm - Fixed for LLP framework
 * Focus on correctness over complex parallelization
 */
public class JohnsonProblem implements LLPProblem<JohnsonProblem.JohnsonState> {
    
    public static class Edge {
        public final int from, to, weight;
        
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
    
    public static class JohnsonState {
        public final int n;
        public final Edge[] edges;
        public final int[][] distances;
        public final int[] h;
        public final int phase; // 0=computing h, 1=computing distances, 2=complete
        
        private static final int INF = 999999;
        
        public JohnsonState(int numVertices, Edge[] edges) {
            this.n = numVertices;
            this.edges = Arrays.copyOf(edges, edges.length);
            this.distances = new int[n][n];
            this.h = new int[n];
            this.phase = 0;
            
            // Initialize distances
            for (int i = 0; i < n; i++) {
                Arrays.fill(distances[i], INF);
                distances[i][i] = 0;
            }
            Arrays.fill(h, 0);
        }
        
        public JohnsonState(int n, Edge[] edges, int[][] distances, int[] h, int phase) {
            this.n = n;
            this.edges = edges;
            this.distances = copyMatrix(distances);
            this.h = Arrays.copyOf(h, n);
            this.phase = phase;
        }
        
        public JohnsonState withH(int[] newH) {
            return new JohnsonState(n, edges, distances, newH, 1);
        }
        
        public JohnsonState withDistances(int[][] newDistances) {
            return new JohnsonState(n, edges, newDistances, h, phase);
        }
        
        public JohnsonState withComplete() {
            return new JohnsonState(n, edges, distances, h, 2);
        }
        
        private static int[][] copyMatrix(int[][] matrix) {
            int[][] copy = new int[matrix.length][];
            for (int i = 0; i < matrix.length; i++) {
                copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
            }
            return copy;
        }
        
        @Override
        public String toString() {
            String[] phases = {"COMPUTING_H", "COMPUTING_DISTANCES", "COMPLETE"};
            return String.format("Johnson{n=%d, phase=%s}", n, phases[phase]);
        }
    }

    private final int numVertices;
    private final Edge[] edges;

    public JohnsonProblem(int numVertices, int[][] edgeData) {
        this.numVertices = numVertices;
        this.edges = new Edge[edgeData.length];
        
        for (int i = 0; i < edgeData.length; i++) {
            edges[i] = new Edge(edgeData[i][0], edgeData[i][1], edgeData[i][2]);
        }
    }
    
    @Override
    public JohnsonState getInitialState() {
        return new JohnsonState(numVertices, edges);
    }

    @Override
    public boolean Forbidden(JohnsonState state) {
        // Only check triangle inequality on final distances
        if (state.phase != 2) return false;
        
        for (int i = 0; i < state.n; i++) {
            for (int j = 0; j < state.n; j++) {
                if (state.distances[i][j] < JohnsonState.INF) {
                    for (int k = 0; k < state.n; k++) {
                        if (state.distances[i][k] < JohnsonState.INF && 
                            state.distances[k][j] < JohnsonState.INF) {
                            if (state.distances[i][j] > state.distances[i][k] + state.distances[k][j]) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public JohnsonState Ensure(JohnsonState state, int threadId, int totalThreads) {
        // Fix triangle inequality violations
        if (state.phase != 2) return state;
        
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean changed = false;
        
        // Simple Floyd-Warshall relaxation
        for (int k = 0; k < state.n; k++) {
            for (int i = threadId; i < state.n; i += totalThreads) {
                for (int j = 0; j < state.n; j++) {
                    if (newDistances[i][k] < JohnsonState.INF && 
                        newDistances[k][j] < JohnsonState.INF) {
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
    
    @Override
    public JohnsonState Advance(JohnsonState state, int threadId, int totalThreads) {
        switch (state.phase) {
            case 0: return advanceHComputation(state, threadId, totalThreads);
            case 1: return advanceDistanceComputation(state, threadId, totalThreads);
            default: return state;
        }
    }
    
    /**
     * SIMPLIFIED: Single-threaded h-value computation
     */
    private JohnsonState advanceHComputation(JohnsonState state, int threadId, int totalThreads) {
        // Only thread 0 computes h-values
        if (threadId != 0) {
            return state;
        }
        
        // Simple Bellman-Ford from virtual vertex (all distances start at 0)
        int[] h = Arrays.copyOf(state.h, state.n);
        
        // Run n-1 iterations of Bellman-Ford
        for (int iter = 0; iter < state.n - 1; iter++) {
            boolean changed = false;
            
            for (Edge edge : state.edges) {
                if (h[edge.from] + edge.weight < h[edge.to]) {
                    h[edge.to] = h[edge.from] + edge.weight;
                    changed = true;
                }
            }
            
            if (!changed) break;
        }
        
        return state.withH(h);
    }
    
    /**
     * SIMPLIFIED: Parallel distance computation
     */
    private JohnsonState advanceDistanceComputation(JohnsonState state, int threadId, int totalThreads) {
        int[][] newDistances = JohnsonState.copyMatrix(state.distances);
        boolean anyChange = false;
        
        // Each thread processes different source vertices
        for (int source = threadId; source < state.n; source += totalThreads) {
            if (computeShortestPaths(state, source, newDistances[source])) {
                anyChange = true;
            }
        }
        
        if (anyChange) {
            // FIXED: Check if all source vertices have been processed
            // (i.e., at least one non-infinity distance in each row, excluding diagonal)
            boolean allSourcesProcessed = true;
            for (int i = 0; i < state.n && allSourcesProcessed; i++) {
                boolean sourceProcessed = false;
                // Check if this source has computed any distances
                for (int j = 0; j < state.n; j++) {
                    if (i != j && newDistances[i][j] < JohnsonState.INF) {
                        sourceProcessed = true;
                        break;
                    }
                }
                // If no reachable vertices from source i, that's still "processed"
                // Check if diagonal is 0 (means this source was at least initialized)
                if (!sourceProcessed && newDistances[i][i] == 0) {
                    sourceProcessed = true; // Source processed but no outgoing paths
                }
                
                if (!sourceProcessed) {
                    allSourcesProcessed = false;
                }
            }
            
            JohnsonState newState = state.withDistances(newDistances);
            return allSourcesProcessed ? newState.withComplete() : newState;
        }
        
        return state;
    }
    
    /**
     * Run Dijkstra for one source vertex
     */
    private boolean computeShortestPaths(JohnsonState state, int source, int[] distances) {
        // Check if already computed
        boolean alreadyComputed = true;
        for (int j = 0; j < state.n; j++) {
            if (distances[j] >= JohnsonState.INF && j != source) {
                alreadyComputed = false;
                break;
            }
        }
        if (alreadyComputed) return false;
        
        // Initialize Dijkstra
        Arrays.fill(distances, JohnsonState.INF);
        distances[source] = 0;
        boolean[] visited = new boolean[state.n];
        
        // Run Dijkstra
        for (int count = 0; count < state.n; count++) {
            int u = -1;
            int minDist = JohnsonState.INF;
            
            // Find minimum unvisited vertex
            for (int v = 0; v < state.n; v++) {
                if (!visited[v] && distances[v] < minDist) {
                    minDist = distances[v];
                    u = v;
                }
            }
            
            if (u == -1) break;
            visited[u] = true;
            
            // Relax edges with reweighted weights
            for (Edge edge : state.edges) {
                if (edge.from == u && !visited[edge.to]) {
                    int reweighted = edge.weight + state.h[edge.from] - state.h[edge.to];
                    if (distances[u] + reweighted < distances[edge.to]) {
                        distances[edge.to] = distances[u] + reweighted;
                    }
                }
            }
        }
        
        // Apply Johnson's adjustment: d[u][v] = d'[u][v] - h[u] + h[v]
        for (int v = 0; v < state.n; v++) {
            if (distances[v] < JohnsonState.INF) {
                distances[v] = distances[v] - state.h[source] + state.h[v];
            }
        }
        
        return true;
    }
    
    @Override
    public boolean isSolution(JohnsonState state) {
        return state.phase == 2 && !Forbidden(state);
    }
    
    @Override
    public JohnsonState merge(JohnsonState state1, JohnsonState state2) {
        // Take the more advanced phase
        if (state1.phase != state2.phase) {
            return state1.phase > state2.phase ? state1 : state2;
        }
        
        // Merge h-values (they should be the same if both computed)
        int[] mergedH = Arrays.equals(state1.h, new int[state1.n]) ? state2.h : state1.h;
        
        // Merge distances (take minimum)
        int[][] mergedDistances = new int[state1.n][state1.n];
        for (int i = 0; i < state1.n; i++) {
            for (int j = 0; j < state1.n; j++) {
                mergedDistances[i][j] = Math.min(state1.distances[i][j], state2.distances[i][j]);
            }
        }
        
        return new JohnsonState(state1.n, state1.edges, mergedDistances, mergedH, 
                               Math.max(state1.phase, state2.phase));
    }

    // ... (keep the same main method and test cases) ...
    public static void main(String[] args) {
        System.out.println("=== Johnson's Algorithm ===\n");
        
        testCase1();
        System.out.println("\n" + "=".repeat(60) + "\n");
        testCase2();
    }

    private static void testCase1() {
        System.out.println("Test Case 1: Small Graph");
        
        int[][] edges = {
            {0, 1, 3}, {0, 2, 8}, {1, 3, -4}, {2, 1, 7}, {3, 0, 2}
        };
        
        JohnsonProblem problem = new JohnsonProblem(4, edges);
        solveProblem(problem, 2, 50); // Reduced threads and iterations
    }

    private static void testCase2() {
        System.out.println("Test Case 2: Larger Graph");
        
        int[][] edges = {
            {0, 1, 4}, {0, 2, 2}, {1, 2, -3}, {1, 3, 2}, {1, 4, 4},
            {2, 3, 4}, {2, 4, 5}, {3, 4, -1}, {4, 0, 3}
        };
        
        JohnsonProblem problem = new JohnsonProblem(5, edges);
        solveProblem(problem, 2, 50);
    }

    private static void solveProblem(JohnsonProblem problem, int numThreads, int maxIterations) {
        LLPSolver<JohnsonState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.currentTimeMillis();
            JohnsonState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("Solution found!");
            printSolution(solution);
            System.out.println("Time: " + (endTime - startTime) + "ms");
            System.out.println("Valid: " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution)); // ADDED
            System.out.println("Phase: " + solution.phase);
            
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
    }

    private static void printSolution(JohnsonState solution) {
        System.out.println("All-pairs shortest distances:");
        
        // Print column headers
        System.out.print("     ");
        for (int j = 0; j < solution.n; j++) {
            System.out.printf("%6d", j);
        }
        System.out.println();
        
        // Print rows with row labels
        for (int i = 0; i < solution.n; i++) {
            System.out.printf("%2d: ", i); // Row label
            for (int j = 0; j < solution.n; j++) {
                if (solution.distances[i][j] >= JohnsonState.INF) {
                    System.out.printf("%6s", "∞");
                } else {
                    System.out.printf("%6d", solution.distances[i][j]);
                }
            }
            System.out.println();
        }
        
        System.out.println("H-values: " + Arrays.toString(solution.h));
    }
}