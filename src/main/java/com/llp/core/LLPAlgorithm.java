package com.llp.core;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Parallel LLP (Las Vegas + Learning) Algorithm implementation.
 * This is the core algorithm engine that can be applied to various problems.
 * 
 * @param <T> The type of the problem state
 */
public class LLPAlgorithm<T> {
    
    private final Problem<T> problem;
    private final int numberOfThreads;
    private final int maxIterations;
    private final ExecutorService executorService;
    
    /**
     * Constructs an LLP algorithm instance.
     * 
     * @param problem The problem to solve
     * @param numberOfThreads Number of parallel threads to use
     * @param maxIterations Maximum iterations per thread
     */
    public LLPAlgorithm(Problem<T> problem, int numberOfThreads, int maxIterations) {
        this.problem = problem;
        this.numberOfThreads = numberOfThreads;
        this.maxIterations = maxIterations;
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }
    
    /**
     * Solves the problem using parallel LLP algorithm.
     * 
     * @return The best solution found
     * @throws InterruptedException if the computation is interrupted
     * @throws ExecutionException if a computation throws an exception
     */
    public LLPResult<T> solve() throws InterruptedException, ExecutionException {
        List<Future<LLPResult<T>>> futures = new ArrayList<>();
        
        // Submit parallel tasks
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            futures.add(executorService.submit(() -> runLLPIteration(threadId)));
        }
        
        // Collect results and find the best one
        LLPResult<T> bestResult = null;
        for (Future<LLPResult<T>> future : futures) {
            LLPResult<T> result = future.get();
            if (bestResult == null || result.getScore() < bestResult.getScore()) {
                bestResult = result;
            }
        }
        
        executorService.shutdown();
        return bestResult;
    }
    
    /**
     * Runs a single LLP iteration in a thread.
     * 
     * @param threadId The ID of the thread
     * @return The result of this iteration
     */
    private LLPResult<T> runLLPIteration(int threadId) {
        T currentState = problem.getInitialState();
        T bestState = currentState;
        double bestScore = problem.evaluate(currentState);
        int iteration = 0;
        
        while (iteration < maxIterations && !problem.isGoal(currentState)) {
            T[] successors = problem.getSuccessors(currentState);
            
            if (successors == null || successors.length == 0) {
                break;
            }
            
            // Evaluate successors and select the best one
            T bestSuccessor = successors[0];
            double bestSuccessorScore = problem.evaluate(bestSuccessor);
            
            for (T successor : successors) {
                double score = problem.evaluate(successor);
                if (score < bestSuccessorScore) {
                    bestSuccessor = successor;
                    bestSuccessorScore = score;
                }
            }
            
            currentState = bestSuccessor;
            
            // Update best state if improved
            if (bestSuccessorScore < bestScore) {
                bestState = currentState;
                bestScore = bestSuccessorScore;
            }
            
            iteration++;
        }
        
        return new LLPResult<>(bestState, bestScore, iteration, threadId);
    }
    
    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executorService.shutdownNow();
    }
}
