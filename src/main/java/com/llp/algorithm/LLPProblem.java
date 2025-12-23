package com.llp.algorithm;

/**
 * Interface for problems that can be solved using the LLP (Least Lattice Predicate) parallel algorithm.
 * 
 * <p>The LLP algorithm is based on lattice theory and repeatedly applies three fundamental operations:
 * <ul>
 *   <li><b>Forbidden</b>: Detects constraint violations</li>
 *   <li><b>Ensure</b>: Fixes constraint violations</li>
 *   <li><b>Advance</b>: Makes progress toward the solution</li>
 * </ul>
 * 
 * <p>The framework executes these operations in parallel across multiple threads,
 * with each thread working on a subset of the problem space. For single-threaded
 * execution, simply call with threadId=0 and totalThreads=1.
 * 
 * <h3>Implementation Guidelines:</h3>
 * 
 * <h4>Forbidden(state)</h4>
 * <ul>
 *   <li>Should efficiently detect if state violates problem invariants</li>
 *   <li>Must be thread-safe (read-only access to state)</li>
 *   <li>Called frequently, so performance is important</li>
 *   <li>Returns true only when constraints are violated</li>
 * </ul>
 * 
 * <h4>Ensure(state, threadId, totalThreads)</h4>
 * <ul>
 *   <li>Must fix violations detected by Forbidden for this thread's partition</li>
 *   <li>Should maintain monotonic progress in the lattice</li>
 *   <li>Must be idempotent when no violations exist</li>
 *   <li>Each thread works on a subset of the problem space</li>
 * </ul>
 * 
 * <h4>Advance(state, threadId, totalThreads)</h4>
 * <ul>
 *   <li>Should make progress toward the solution for this thread's partition</li>
 *   <li>It's OK to create forbidden states (Ensure will fix them)</li>
 *   <li>Must eventually lead to convergence</li>
 *   <li>Each thread processes a subset of the work</li>
 * </ul>
 * 
 * <h3>Thread Distribution Pattern:</h3>
 * <pre>{@code
 * // Distribute work among threads using modulo arithmetic
 * for (int i = threadId; i < workItems.length; i += totalThreads) {
 *     // Process workItems[i] in this thread
 *     // Thread 0 gets: 0, 3, 6, 9...
 *     // Thread 1 gets: 1, 4, 7, 10...
 *     // Thread 2 gets: 2, 5, 8, 11...
 * }
 * }</pre>
 * 
 * <h3>Example Implementation Pattern:</h3>
 * <pre>{@code
 * public class MyProblem implements LLPProblem<MyState> {
 *     
 *     @Override
 *     public boolean Forbidden(MyState state) {
 *         // Check if any constraint is violated
 *         return hasViolation(state);
 *     }
 *     
 *     @Override
 *     public MyState Ensure(MyState state, int threadId, int totalThreads) {
 *         // Fix violations for this thread's partition
 *         for (int i = threadId; i < violations.length; i += totalThreads) {
 *             state = fixViolation(state, violations[i]);
 *         }
 *         return state;
 *     }
 *     
 *     @Override
 *     public MyState Advance(MyState state, int threadId, int totalThreads) {
 *         // Make progress for this thread's partition
 *         for (int i = threadId; i < workItems.length; i += totalThreads) {
 *             state = processItem(state, workItems[i]);
 *         }
 *         return state;
 *     }
 *     
 *     @Override
 *     public MyState getInitialState() {
 *         return new MyState();
 *     }
 *     
 *     @Override
 *     public boolean isSolution(MyState state) {
 *         return !Forbidden(state) && isComplete(state);
 *     }
 * }
 * }</pre>
 * 
 * @param <T> The type representing the state or configuration in the lattice.
 *           Should be thread-safe or immutable for safe parallel access.
 * 
 * @see com.llp.algorithm.LLPSolver
 * @see com.llp.framework.LLPEngine
 */
public interface LLPProblem<T> {
    /**
     * Check if state violates constraints
     */
    boolean Forbidden(T state);
    
    /**
     * Advance the state to fix forbidden conditions.
     * Should only be called on forbidden states.
     * After advancing, the state should no longer be forbidden.
     */
    T Advance(T state, int threadId, int totalThreads);
    
    /**
     * Get initial state
     */
    T getInitialState();
    
    /**
     * Check if we have a complete solution
     */
    boolean isSolution(T state);
}
