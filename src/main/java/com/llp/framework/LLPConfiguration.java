package com.llp.framework;

/**
 * Configuration settings for LLP execution.
 * Provides a fluent API for configuring LLP algorithm execution parameters.
 */
public class LLPConfiguration {
    
    private int numThreads;
    private int maxIterations;
    private boolean enableLogging;
    private long timeoutMillis;
    
    /**
     * Creates a default configuration with sensible defaults.
     */
    public LLPConfiguration() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.maxIterations = Integer.MAX_VALUE;
        this.enableLogging = false;
        this.timeoutMillis = Long.MAX_VALUE;
    }
    
    /**
     * Sets the number of parallel threads to use.
     * 
     * @param numThreads The number of threads
     * @return This configuration object for chaining
     */
    public LLPConfiguration setNumThreads(int numThreads) {
        if (numThreads < 1) {
            throw new IllegalArgumentException("Number of threads must be at least 1");
        }
        this.numThreads = numThreads;
        return this;
    }
    
    /**
     * Gets the number of threads.
     * 
     * @return The number of threads
     */
    public int getNumThreads() {
        return numThreads;
    }
    
    /**
     * Sets the maximum number of iterations.
     * 
     * @param maxIterations The maximum iterations
     * @return This configuration object for chaining
     */
    public LLPConfiguration setMaxIterations(int maxIterations) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("Max iterations must be at least 1");
        }
        this.maxIterations = maxIterations;
        return this;
    }
    
    /**
     * Gets the maximum iterations.
     * 
     * @return The maximum iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }
    
    /**
     * Enables or disables execution logging.
     * 
     * @param enableLogging true to enable logging, false otherwise
     * @return This configuration object for chaining
     */
    public LLPConfiguration setLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
        return this;
    }
    
    /**
     * Checks if logging is enabled.
     * 
     * @return true if logging enabled, false otherwise
     */
    public boolean isLoggingEnabled() {
        return enableLogging;
    }
    
    /**
     * Sets the execution timeout in milliseconds.
     * 
     * @param timeoutMillis The timeout in milliseconds
     * @return This configuration object for chaining
     */
    public LLPConfiguration setTimeout(long timeoutMillis) {
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.timeoutMillis = timeoutMillis;
        return this;
    }
    
    /**
     * Gets the timeout in milliseconds.
     * 
     * @return The timeout
     */
    public long getTimeout() {
        return timeoutMillis;
    }
    
    /**
     * Creates a copy of this configuration.
     * 
     * @return A new configuration with the same settings
     */
    public LLPConfiguration copy() {
        LLPConfiguration copy = new LLPConfiguration();
        copy.numThreads = this.numThreads;
        copy.maxIterations = this.maxIterations;
        copy.enableLogging = this.enableLogging;
        copy.timeoutMillis = this.timeoutMillis;
        return copy;
    }
}
