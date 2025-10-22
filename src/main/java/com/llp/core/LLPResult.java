package com.llp.core;

/**
 * Represents the result of an LLP algorithm execution.
 * 
 * @param <T> The type of the solution state
 */
public class LLPResult<T> {
    
    private final T solution;
    private final double score;
    private final int iterations;
    private final int threadId;
    
    /**
     * Constructs an LLP result.
     * 
     * @param solution The solution state found
     * @param score The evaluation score of the solution
     * @param iterations Number of iterations taken
     * @param threadId The ID of the thread that found this solution
     */
    public LLPResult(T solution, double score, int iterations, int threadId) {
        this.solution = solution;
        this.score = score;
        this.iterations = iterations;
        this.threadId = threadId;
    }
    
    public T getSolution() {
        return solution;
    }
    
    public double getScore() {
        return score;
    }
    
    public int getIterations() {
        return iterations;
    }
    
    public int getThreadId() {
        return threadId;
    }
    
    @Override
    public String toString() {
        return String.format("LLPResult{score=%.4f, iterations=%d, threadId=%d}", 
                           score, iterations, threadId);
    }
}
