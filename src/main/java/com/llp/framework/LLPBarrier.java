package com.llp.framework;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A synchronization barrier for coordinating parallel LLP threads.
 * Provides mechanisms for ensuring all threads reach synchronization points
 * before proceeding to the next phase of execution.
 */
public class LLPBarrier {
    
    private final CyclicBarrier barrier;
    private final int parties;
    
    /**
     * Creates a new LLPBarrier for the specified number of threads.
     * 
     * @param parties The number of threads that must wait at the barrier
     */
    public LLPBarrier(int parties) {
        this.parties = parties;
        this.barrier = new CyclicBarrier(parties);
    }
    
    /**
     * Creates a new LLPBarrier with an action to execute when all threads arrive.
     * 
     * @param parties The number of threads that must wait at the barrier
     * @param barrierAction Action to execute when all threads reach the barrier
     */
    public LLPBarrier(int parties, Runnable barrierAction) {
        this.parties = parties;
        this.barrier = new CyclicBarrier(parties, barrierAction);
    }
    
    /**
     * Waits for all threads to reach the barrier.
     * 
     * @throws InterruptedException if interrupted while waiting
     * @throws BrokenBarrierException if the barrier is broken while waiting
     */
    public void await() throws InterruptedException, BrokenBarrierException {
        barrier.await();
    }
    
    /**
     * Waits for all threads to reach the barrier with a timeout.
     * 
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout
     * @throws InterruptedException if interrupted while waiting
     * @throws BrokenBarrierException if the barrier is broken while waiting
     * @throws TimeoutException if the timeout is reached
     */
    public void await(long timeout, TimeUnit unit) 
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        barrier.await(timeout, unit);
    }
    
    /**
     * Resets the barrier to its initial state.
     * Should be called with caution as it may break waiting threads.
     */
    public void reset() {
        barrier.reset();
    }
    
    /**
     * Gets the number of threads required to trip this barrier.
     * 
     * @return The number of parties
     */
    public int getParties() {
        return parties;
    }
    
    /**
     * Gets the number of threads currently waiting at the barrier.
     * 
     * @return The number of waiting threads
     */
    public int getNumberWaiting() {
        return barrier.getNumberWaiting();
    }
    
    /**
     * Checks if the barrier is broken.
     * 
     * @return true if broken, false otherwise
     */
    public boolean isBroken() {
        return barrier.isBroken();
    }
}
