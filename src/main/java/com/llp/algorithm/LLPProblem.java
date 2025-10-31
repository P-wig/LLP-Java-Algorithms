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
     * The Forbidden predicate determines if a given configuration violates problem constraints.
     * 
     * <p>This method is called frequently by the framework to detect when the state
     * violates problem invariants. It should be efficient and thread-safe.
     * 
     * <p><b>Implementation Requirements:</b>
     * <ul>
     *   <li>Must be thread-safe (read-only access to state)</li>
     *   <li>Should return true if and only if constraints are violated</li>
     *   <li>Should be deterministic for the same state</li>
     *   <li>Performance-critical: called many times during execution</li>
     * </ul>
     * 
     * @param state The current state/configuration to check
     * @return true if the state is forbidden (violates constraints), false otherwise
     */
    boolean Forbidden(T state);
    
    /**
     * The Ensure operation fixes constraint violations for this thread's partition.
     * 
     * <p>This method is called after Advance to repair any violations introduced.
     * Each thread should work on a subset of the violations or problem space,
     * using the threadId and totalThreads to partition the work.
     * 
     * <p><b>Implementation Requirements:</b>
     * <ul>
     *   <li>Must fix violations detected by Forbidden for this thread's partition</li>
     *   <li>Should be idempotent: Ensure(Ensure(s, tid, total), tid, total) = Ensure(s, tid, total)</li>
     *   <li>Must maintain monotonic progress in the lattice</li>
     *   <li>Should return the same state if no violations exist in this partition</li>
     * </ul>
     * 
     * <p><b>Single-threaded usage:</b> Call with threadId=0, totalThreads=1
     * 
     * @param state The current state to be modified
     * @param threadId The ID of this thread (0-based)
     * @param totalThreads The total number of threads
     * @return The updated state with constraint violations fixed for this partition
     */
    T Ensure(T state, int threadId, int totalThreads);
    
    /**
     * The Advance operation moves the state toward the solution for this thread's partition.
     * 
     * <p>This method makes progress on the problem by updating the state,
     * potentially creating new forbidden configurations that will be fixed
     * by subsequent Ensure operations. Each thread should work on a subset
     * of the problem space.
     * 
     * <p><b>Implementation Requirements:</b>
     * <ul>
     *   <li>Should make clear, measurable progress toward the solution</li>
     *   <li>May create forbidden states (that's expected and OK)</li>
     *   <li>Must eventually lead to convergence when alternated with Ensure</li>
     *   <li>Should advance in the lattice ordering</li>
     *   <li>Each thread processes only its assigned partition</li>
     * </ul>
     * 
     * <p><b>Single-threaded usage:</b> Call with threadId=0, totalThreads=1
     * 
     * @param state The current state
     * @param threadId The ID of this thread (0-based)
     * @param totalThreads The total number of threads
     * @return The advanced state with progress toward the solution for this partition
     */
    T Advance(T state, int threadId, int totalThreads);
    
    /**
     * Returns the initial state for the problem.
     * 
     * <p>This method is called once at the start of the algorithm to obtain
     * the starting configuration. The initial state should represent the
     * beginning of the problem (e.g., no matches in stable marriage,
     * infinite distances in shortest path, etc.).
     * 
     * @return The starting state/configuration for the problem
     */
    T getInitialState();
    
    /**
     * Checks if the current state represents a valid solution to the problem.
     * 
     * <p>This method determines if the algorithm has reached a valid solution.
     * Typically, a solution must satisfy all constraints (not Forbidden) and
     * meet any completion criteria specific to the problem.
     * 
     * <p><b>Typical Implementation:</b>
     * <pre>{@code
     * return !Forbidden(state) && isComplete(state);
     * }</pre>
     * 
     * @param state The current state to check
     * @return true if the state is a valid solution, false otherwise
     */
    boolean isSolution(T state);

    /**
     * Merge results from parallel thread operations.
     * 
     * <p>This method combines the results from multiple threads after parallel
     * Advance or Ensure operations. The implementation should intelligently
     * merge the states, typically taking the "best" progress from each.
     * 
     * <p><b>Common Patterns:</b>
     * <ul>
     *   <li>Take minimum distances (shortest path problems)</li>
     *   <li>Union of edges (MST problems)</li>
     *   <li>Logical OR of boolean arrays (reachability problems)</li>
     * </ul>
     * 
     * @param state1 Result from one thread
     * @param state2 Result from another thread
     * @return The merged state combining progress from both threads
     */
    default T merge(T state1, T state2) {
        // Default: prefer non-forbidden states
        if (Forbidden(state1) && !Forbidden(state2)) {
            return state2;
        }
        if (!Forbidden(state1) && Forbidden(state2)) {
            return state1;
        }
        return state2; // Both same constraint status, return either
    }
}
