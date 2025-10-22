package com.llp.framework;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a thread-safe state container for LLP algorithms.
 * This class wraps the problem-specific state and provides synchronization
 * mechanisms for safe concurrent access during parallel execution.
 * 
 * @param <T> The type representing the problem-specific state
 */
public class LLPState<T> {
    
    private volatile T state;
    private final ReadWriteLock lock;
    private volatile long version;
    
    /**
     * Creates a new LLPState with the given initial state.
     * 
     * @param initialState The initial state value
     */
    public LLPState(T initialState) {
        this.state = initialState;
        this.lock = new ReentrantReadWriteLock();
        this.version = 0;
    }
    
    /**
     * Gets the current state value with read lock protection.
     * 
     * @return The current state
     */
    public T get() {
        lock.readLock().lock();
        try {
            return state;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Updates the state with write lock protection.
     * Increments the version number on each update.
     * 
     * @param newState The new state value
     */
    public void set(T newState) {
        lock.writeLock().lock();
        try {
            this.state = newState;
            this.version++;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current version number of the state.
     * Useful for detecting changes during parallel execution.
     * 
     * @return The current version number
     */
    public long getVersion() {
        return version;
    }
    
    /**
     * Acquires a read lock on the state.
     * Must be paired with releaseReadLock().
     */
    public void acquireReadLock() {
        lock.readLock().lock();
    }
    
    /**
     * Releases the read lock on the state.
     */
    public void releaseReadLock() {
        lock.readLock().unlock();
    }
    
    /**
     * Acquires a write lock on the state.
     * Must be paired with releaseWriteLock().
     */
    public void acquireWriteLock() {
        lock.writeLock().lock();
    }
    
    /**
     * Releases the write lock on the state.
     */
    public void releaseWriteLock() {
        lock.writeLock().unlock();
    }
    
    /**
     * Creates a snapshot of the current state.
     * Note: The returned value is not thread-safe itself.
     * 
     * @return A snapshot of the current state
     */
    public T snapshot() {
        return get();
    }
}
