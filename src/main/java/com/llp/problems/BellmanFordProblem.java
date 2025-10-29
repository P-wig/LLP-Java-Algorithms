package com.llp.problems;

import com.llp.algorithm.LLPProblem;

/**
 * Bellman-Ford Single-Source Shortest Path Algorithm using the LLP framework.
 * 
 * <h3>Problem Description:</h3>
 * The Bellman-Ford algorithm finds shortest paths from a source vertex to all
 * other vertices in a weighted directed graph. It can handle negative edge weights
 * (unlike Dijkstra's algorithm) but the graph must not contain negative cycles.
 * 
 * <h3>LLP Implementation Strategy:</h3>
 * <ul>
 *   <li><b>State:</b> Distance estimates from source to each vertex</li>
 *   <li><b>Forbidden:</b> Distance estimates that violate triangle inequality</li>
 *   <li><b>Advance:</b> Relax edges to improve distance estimates</li>
 *   <li><b>Ensure:</b> Fix triangle inequality violations</li>
 *   <li><b>Parallelism:</b> Multiple edges can be relaxed simultaneously</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Graph: 0->1(5), 0->2(3), 1->3(1), 2->1(-2), 2->3(4)
 * Edge[] edges = {
 *     new Edge(0, 1, 5),
 *     new Edge(0, 2, 3),
 *     new Edge(1, 3, 1),
 *     new Edge(2, 1, -2),  // Negative edge
 *     new Edge(2, 3, 4)
 * };
 * BellmanFordProblem problem = new BellmanFordProblem(4, edges, 0);
 * LLPSolver<BellmanFordState> solver = new LLPSolver<>(problem);
 * BellmanFordState solution = solver.solve();
 * System.out.println("Shortest distances: " + Arrays.toString(solution.getDistances()));
 * }</pre>
 */
public class BellmanFordProblem implements LLPProblem<BellmanFordState> {
    
    // TODO: Add problem instance fields
    private final int numVertices;
    private final Edge[] edges;
    private final int source;
    
    /**
     * Creates a new Bellman-Ford problem instance.
     * 
     * @param numVertices Number of vertices in the graph
     * @param edges Array of weighted edges
     * @param source Source vertex (0-indexed)
     */
    public BellmanFordProblem(int numVertices, Edge[] edges, int source) {
        // TODO: Initialize fields
        this.numVertices = numVertices;
        this.edges = edges.clone();  // Defensive copy
        this.source = source;
    }
    
    @Override
    public boolean Forbidden(BellmanFordState state) {
        // TODO: Check if distance estimates violate triangle inequality
        // For each edge (u, v) with weight w:
        //   if distance[v] > distance[u] + w + EPSILON, the state is forbidden
        //
        // HINT: Use small epsilon (1e-10) for floating-point comparison
        // HINT: Skip edges where source vertex is unreachable (distance = INFINITY)
        //
        // Return true if any edge violates the triangle inequality
        throw new UnsupportedOperationException("TODO: Implement triangle inequality checking");
    }
    
    @Override
    public BellmanFordState Ensure(BellmanFordState state) {
        // TODO: Fix distance estimates that violate constraints
        // For each edge (u, v) with weight w where distance[v] > distance[u] + w:
        //   Create new state with distance[v] = distance[u] + w
        //
        // HINT: Use state.withDistance(vertex, newDistance) to create new state
        // HINT: Process all violations in sequence
        // HINT: If no violations, return original state unchanged
        //
        // Return state with all triangle inequality violations fixed
        throw new UnsupportedOperationException("TODO: Implement constraint fixing via edge relaxation");
    }
    
    @Override
    public BellmanFordState Advance(BellmanFordState state) {
        // TODO: Relax edges to improve distance estimates
        // For each edge (u, v) with weight w:
        //   if distance[u] + w < distance[v]:
        //     Create new state with distance[v] = distance[u] + w
        //
        // HINT: This is standard Bellman-Ford edge relaxation
        // HINT: Skip edges where source vertex is unreachable
        // HINT: Process all edges and accumulate improvements
        //
        // Return state with improved distance estimates
        throw new UnsupportedOperationException("TODO: Implement edge relaxation for progress");
    }
    
    @Override
    public BellmanFordState getInitialState() {
        // TODO: Create initial state with proper distance initialization
        // distance[source] = 0.0
        // distance[v] = Double.POSITIVE_INFINITY for all other vertices v
        //
        // HINT: return new BellmanFordState(numVertices, edges, source);
        //
        // Return starting state for the algorithm
        throw new UnsupportedOperationException("TODO: Create initial state with source=0, others=infinity");
    }
    
    @Override
    public boolean isSolution(BellmanFordState state) {
        // TODO: Check if shortest paths are correctly computed
        // A state is a solution if:
        // 1. No constraint violations exist (!Forbidden(state))
        // 2. All shortest paths have been found (no more improvements possible)
        //
        // HINT: For Bellman-Ford, solution = !Forbidden(state) is usually sufficient
        // HINT: Advanced: Check if another Advance() would make changes
        //
        // Return true if shortest paths are optimal
        throw new UnsupportedOperationException("TODO: Check if shortest paths are optimal");
    }
}

/**
 * State representation for Bellman-Ford algorithm.
 * 
 * TODO: Implement this class with:
 * - Immutable distance array
 * - Graph structure (edges)
 * - Source vertex
 * - Helper methods for creating new states
 */
class BellmanFordState {
    // TODO: Add fields for distances, edges, source, numVertices
    
    // TODO: Add constructor
    
    // TODO: Add helper method: withDistance(int vertex, double distance)
    
    // TODO: Add getters: getDistance(int vertex), getDistances(), etc.
    
    // TODO: Add toString() for debugging
    
    // TODO: Add equals() for convergence detection
}

/**
 * Represents a weighted directed edge in the graph.
 * 
 * TODO: Implement this class with:
 * - Source vertex (from)
 * - Destination vertex (to) 
 * - Edge weight
 */
class Edge {
    // TODO: Add fields: from, to, weight
    
    // TODO: Add constructor
    
    // TODO: Add toString() for debugging
}
