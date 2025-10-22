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
 * synchronizing between phases to ensure correctness while maximizing parallelism.
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
 * <h4>Ensure(state)</h4>
 * <ul>
 *   <li>Must fix all violations detected by Forbidden</li>
 *   <li>Should maintain monotonic progress in the lattice</li>
 *   <li>Must be idempotent when no violations exist</li>
 *   <li>May need to propagate fixes to related state elements</li>
 * </ul>
 * 
 * <h4>Advance(state)</h4>
 * <ul>
 *   <li>Should make clear progress toward the solution</li>
 *   <li>It's OK to create forbidden states (Ensure will fix them)</li>
 *   <li>Must eventually lead to convergence</li>
 *   <li>Should explore the solution space efficiently</li>
 * </ul>
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
 *     public MyState Ensure(MyState state) {
 *         // Fix violations incrementally
 *         if (Forbidden(state)) {
 *             return fixViolations(state);
 *         }
 *         return state;
 *     }
 *     
 *     @Override
 *     public MyState Advance(MyState state) {
 *         // Make progress toward solution
 *         return makeProgress(state);
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
     * The Ensure operation fixes constraint violations to make the state valid.
     * 
     * <p>This method is called after Advance to repair any violations introduced.
     * It should modify the state to satisfy all local constraints while maintaining
     * progress in the lattice ordering.
     * 
     * <p><b>Implementation Requirements:</b>
     * <ul>
     *   <li>Must fix all violations detected by Forbidden</li>
     *   <li>Should be idempotent: Ensure(Ensure(s)) = Ensure(s)</li>
     *   <li>Must maintain monotonic progress in the lattice</li>
     *   <li>Should return the same state if no violations exist</li>
     * </ul>
     * 
     * @param state The current state to be modified
     * @return The updated state with constraint violations fixed
     */
    T Ensure(T state);
    
    /**
     * The Advance operation moves the state toward the solution.
     * 
     * <p>This method makes progress on the problem by updating the state,
     * potentially creating new forbidden configurations that will be fixed
     * by subsequent Ensure operations. The key is making meaningful progress
     * toward the solution.
     * 
     * <p><b>Implementation Requirements:</b>
     * <ul>
     *   <li>Should make clear, measurable progress toward the solution</li>
     *   <li>May create forbidden states (that's expected and OK)</li>
     *   <li>Must eventually lead to convergence when alternated with Ensure</li>
     *   <li>Should advance in the lattice ordering</li>
     * </ul>
     * 
     * @param state The current state
     * @return The advanced state with progress toward the solution
     */
    T Advance(T state);
    
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
}
