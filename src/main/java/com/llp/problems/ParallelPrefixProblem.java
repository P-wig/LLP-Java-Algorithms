package com.llp.problems;

import com.llp.algorithm.LLPProblem;

/**
 * Parallel Prefix Problem using LLP algorithm.
 * 
 * The parallel prefix (scan) problem computes all prefix sums of an array.
 * Given an array [a1, a2, ..., an] and an associative operator ⊕,
 * compute [a1, a1⊕a2, a1⊕a2⊕a3, ..., a1⊕a2⊕...⊕an].
 * 
 * TODO: Implement the Forbidden, Ensure, and Advance methods
 * TODO: Define the state representation (e.g., partial prefix computations)
 */
public class ParallelPrefixProblem implements LLPProblem<Object> {
    
    @Override
    public boolean Forbidden(Object state) {
        // TODO: Check if any prefix computation is incorrect
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object Ensure(Object state) {
        // TODO: Fix incorrect prefix values
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object Advance(Object state) {
        // TODO: Compute more prefix values or propagate partial results
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public Object getInitialState() {
        // TODO: Return initial state (e.g., original array)
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public boolean isSolution(Object state) {
        // TODO: Check if all prefix values are computed correctly
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
