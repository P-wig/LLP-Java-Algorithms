package com.llp.problems;

import com.llp.algorithm.LLPProblem;

/**
 * Bellman-Ford Algorithm using LLP.
 * 
 * The Bellman-Ford algorithm finds shortest paths from a source vertex to all
 * other vertices in a weighted directed graph, even with negative edge weights
 * (but no negative cycles).
 * 
 * TODO: Implement the Forbidden, Ensure, and Advance methods
 * TODO: Define the state representation (e.g., distance estimates to vertices)
 */
public class BellmanFordProblem implements LLPProblem<Object> {
    
    @Override
    public boolean Forbidden(Object state) {
        // TODO: Check if distance estimates violate triangle inequality
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object Ensure(Object state) {
        // TODO: Fix distance estimates that violate constraints
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object Advance(Object state) {
        // TODO: Relax edges to improve distance estimates
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object getInitialState() {
        // TODO: Return initial state (source at distance 0, others at infinity)
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public boolean isSolution(Object state) {
        // TODO: Check if all shortest paths are found
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
