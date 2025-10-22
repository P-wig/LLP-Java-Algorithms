package com.llp.problems;

import com.llp.core.Problem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Example problem: Simple Number Optimization
 * Goal: Find a number close to a target value through incremental changes.
 * This serves as a simple example to demonstrate the LLP API usage.
 */
public class ExampleProblem implements Problem<Integer> {
    
    private final int target;
    private final int initialValue;
    private final Random random;
    
    public ExampleProblem(int target, int initialValue) {
        this.target = target;
        this.initialValue = initialValue;
        this.random = new Random();
    }
    
    @Override
    public Integer getInitialState() {
        return initialValue;
    }
    
    @Override
    public boolean isGoal(Integer state) {
        return state.equals(target);
    }
    
    @Override
    public Integer[] getSuccessors(Integer state) {
        // Generate neighboring states (±1, ±5, ±10)
        List<Integer> successors = new ArrayList<>();
        successors.add(state + 1);
        successors.add(state - 1);
        successors.add(state + 5);
        successors.add(state - 5);
        successors.add(state + 10);
        successors.add(state - 10);
        
        return successors.toArray(new Integer[0]);
    }
    
    @Override
    public double evaluate(Integer state) {
        // Return the absolute difference from target (lower is better)
        return Math.abs(state - target);
    }
    
    @Override
    public String formatSolution(Integer state) {
        return String.format("Solution: %d (Target: %d, Distance: %.0f)", 
                           state, target, evaluate(state));
    }
}
