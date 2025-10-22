package com.llp.problems;

import com.llp.algorithm.LLPProblem;

/**
 * Johnson's Algorithm using LLP.
 * 
 * Johnson's algorithm finds shortest paths between all pairs of vertices in a
 * sparse, weighted, directed graph. It uses reweighting and Bellman-Ford/Dijkstra.
 * 
 * TODO: Implement the Forbidden, Ensure, and Advance methods
 * TODO: Define the state representation (e.g., distance matrix)
 */
public class JohnsonProblem implements LLPProblem<Object> {
    
    @Override
    public boolean Forbidden(Object state) {
        // TODO: Check if distance estimates violate constraints
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object Ensure(Object state) {
        // TODO: Fix distance estimates
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object Advance(Object state) {
        // TODO: Compute shortest paths or reweight edges
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object getInitialState() {
        // TODO: Return initial state
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public boolean isSolution(Object state) {
        // TODO: Check if all-pairs shortest paths are found
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
