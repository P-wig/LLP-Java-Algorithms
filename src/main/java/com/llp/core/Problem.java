package com.llp.core;

/**
 * Interface representing a problem that can be solved using the LLP algorithm.
 * 
 * @param <T> The type of the solution/state
 */
public interface Problem<T> {
    
    /**
     * Returns the initial state of the problem.
     * 
     * @return The initial state
     */
    T getInitialState();
    
    /**
     * Checks if the given state is a goal state.
     * 
     * @param state The state to check
     * @return true if the state is a goal state, false otherwise
     */
    boolean isGoal(T state);
    
    /**
     * Generates successor states from the given state.
     * 
     * @param state The current state
     * @return Array of successor states
     */
    T[] getSuccessors(T state);
    
    /**
     * Evaluates the cost or fitness of a given state.
     * Lower values typically indicate better states.
     * 
     * @param state The state to evaluate
     * @return The evaluation score
     */
    double evaluate(T state);
    
    /**
     * Returns a string representation of the solution.
     * 
     * @param state The solution state
     * @return String representation of the solution
     */
    String formatSolution(T state);
}
