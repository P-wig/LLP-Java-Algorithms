package com.llp.algorithm;

/**
 * Interface for problems that can be solved using the LLP (Least Lattice Predicate) parallel algorithm.
 * 
 * @param <T> The type representing the state or configuration in the lattice
 */
public interface LLPProblem<T> {
    
    /**
     * The Forbidden predicate determines if a given configuration is invalid or forbidden.
     * This method should return true if the configuration violates problem constraints.
     * 
     * @param state The current state/configuration to check
     * @return true if the state is forbidden (invalid), false otherwise
     */
    boolean Forbidden(T state);
    
    /**
     * The Ensure operation modifies the state to satisfy local constraints.
     * This method should update the state to remove any forbidden configurations
     * while maintaining progress toward the solution.
     * 
     * @param state The current state to be modified
     * @return The updated state that satisfies local constraints
     */
    T Ensure(T state);
    
    /**
     * The Advance operation moves the state forward in the lattice toward the solution.
     * This method should make progress on the problem while potentially creating
     * new forbidden configurations that will be resolved by Ensure.
     * 
     * @param state The current state
     * @return The advanced state with progress toward the solution
     */
    T Advance(T state);
    
    /**
     * Returns the initial state for the problem.
     * 
     * @return The starting state/configuration
     */
    T getInitialState();
    
    /**
     * Checks if the current state represents a solution to the problem.
     * 
     * @param state The current state to check
     * @return true if the state is a valid solution, false otherwise
     */
    boolean isSolution(T state);
}
