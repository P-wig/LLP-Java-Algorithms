package com.llp.problems;

import java.util.Arrays;
import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

/**
 * Stable Marriage Problem using the LLP framework.
 * 
 * <h3>Problem Description:</h3>
 * The stable marriage problem involves matching n men and n women where each person
 * has a preference list ranking all members of the opposite gender. The goal is to find
 * a stable matching where no two people would prefer each other over their current partners.
 * 
 * <h3>State Representation:</h3>
 * StableMarriageState represents:
 * <ul>
 *   <li>Current matching configuration (who is matched with whom)</li>
 *   <li>Preference lists for men and women</li>
 *   <li>Ranking arrays for efficient preference lookups</li>
 * </ul>
 * 
 * <h3>Implementation Guide:</h3>
 * <ul>
 *   <li><b>Forbidden(state):</b> Check if the current matching has any unstable pairs.
 *       An unstable pair is a man and woman who are not matched to each other but would
 *       both prefer each other over their current partners.</li>
 *   
 *   <li><b>Ensure(state, threadId, totalThreads):</b> Fix unstable pairs by updating the matching.
 *       When an unstable pair is found, break existing matches and create new ones
 *       to resolve the instability. Distribute the checking and fixing of pairs
 *       among threads using threadId and totalThreads for parallel processing.</li>
 *   
 *   <li><b>Advance(state, threadId, totalThreads):</b> Propose new matches or improve the current matching.
 *       For example, have unmatched men propose to their next preferred woman,
 *       or explore better matching configurations. Use thread distribution to
 *       parallelize proposal processing across different men/women.</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Define preferences
 * int[][] menPrefs = {{0, 1, 2}, {1, 2, 0}, {0, 1, 2}};
 * int[][] womenPrefs = {{1, 0, 2}, {0, 2, 1}, {0, 1, 2}};
 * 
 * // Create and solve
 * StableMarriageProblem problem = new StableMarriageProblem(menPrefs, womenPrefs);
 * LLPSolver<StableMarriageState> solver = new LLPSolver<>(problem);
 * StableMarriageState solution = solver.solve();
 * }</pre>
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Stable_marriage_problem">Stable Marriage Problem</a>
 */
public class StableMarriageProblem implements LLPProblem<StableMarriageProblem.StableMarriageState> {
    
    /**
     * State class representing a matching configuration in the stable marriage problem.
     */
    public static class StableMarriageState {
        public final int n;                  // Number of men/women
        public final int[][] menPrefs;       // menPrefs[i][j] = j-th preference of man i
        public final int[][] womenPrefs;     // womenPrefs[i][j] = j-th preference of woman i
        public final int[][] menRanking;     // menRanking[i][j] = rank of woman j in man i's preferences
        public final int[][] womenRanking;   // womenRanking[i][j] = rank of man j in woman i's preferences
        public final int[] menPartner;       // menPartner[i] = partner of man i (-1 if unmatched)
        public final int[] womenPartner;     // womenPartner[i] = partner of woman i (-1 if unmatched)

        /**
         * Creates initial state with given preferences and no matches.
         */
        public StableMarriageState(int n, int[][] menPrefs, int[][] womenPrefs) {
            this.n = n;
            this.menPrefs = new int[n][n];
            this.womenPrefs = new int[n][n];
            this.menRanking = new int[n][n];
            this.womenRanking = new int[n][n];

            // Deep copy preferences to ensure immutability
            for (int i = 0; i < n; i++){
                System.arraycopy(menPrefs[i], 0, this.menPrefs[i], 0, n);
                System.arraycopy(womenPrefs[i], 0, this.womenPrefs[i], 0, n);
            }

            // Build ranking arrays for fast preference comparisons
            for (int i = 0; i < n; i++){
                for (int j = 0; j < n; j++){
                    menRanking[i][menPrefs[i][j]] = j;
                    womenRanking[i][womenPrefs[i][j]] = j;
                }
            }

            // Initialize all as unmatched -1
            this.menPartner = new int[n];
            this.womenPartner = new int[n];
            Arrays.fill(menPartner, -1);
            Arrays.fill(womenPartner, -1);
        }

        /**
         * Copy constructor for creating new states with updated matches.
         */
        public StableMarriageState(int n, int[][] menPrefs, int[][] womenPrefs, 
                                 int[][] menRanking, int[][] womenRanking,
                                 int[] menPartner, int[] womenPartner) {
            this.n = n;
            this.menPrefs = menPrefs; // Share immutable preference data
            this.womenPrefs = womenPrefs;
            this.menRanking = menRanking;
            this.womenRanking = womenRanking;
            
            // Copy mutable matching arrays
            this.menPartner = Arrays.copyOf(menPartner, n);
            this.womenPartner = Arrays.copyOf(womenPartner, n);
        }

        /**
         * Creates new state with the specified man-woman match, breaking existing matches as needed.
         */
        public StableMarriageState withMatch(int man, int woman) {
            int[] newMenPartner = Arrays.copyOf(menPartner, n);
            int[] newWomenPartner = Arrays.copyOf(womenPartner, n);
            
            // Break existing matches
            if (newMenPartner[man] != -1) {
                newWomenPartner[newMenPartner[man]] = -1;
            }
            if (newWomenPartner[woman] != -1) {
                newMenPartner[newWomenPartner[woman]] = -1;
            }
            
            // Create new match
            newMenPartner[man] = woman;
            newWomenPartner[woman] = man;
            
            return new StableMarriageState(n, menPrefs, womenPrefs, menRanking, womenRanking,
                                         newMenPartner, newWomenPartner);
        }
        
        /**
         * Returns true if the man prefers the woman over his current partner.
         */
        public boolean manPrefers(int man, int woman) {
            if (menPartner[man] == -1) return true; // unmatched man prefers anyone
            return menRanking[man][woman] < menRanking[man][menPartner[man]];
        }
        
        /**
         * Returns true if the woman prefers the man over her current partner.
         */
        public boolean womanPrefers(int woman, int man) {
            if (womenPartner[woman] == -1) return true; // unmatched woman prefers anyone
            return womenRanking[woman][man] < womenRanking[woman][womenPartner[woman]];
        }

        /**
         * Returns true if all people are matched (complete matching).
         */
        public boolean isComplete() {
            for (int i = 0; i < n; i++) {
                if (menPartner[i] == -1 || womenPartner[i] == -1) {
                    return false;
                }
            }
            return true;
        }
    }

    private final int n;                 // Number of men/women
    private final int[][] menPrefs;      // Men's preference lists
    private final int[][] womenPrefs;    // Women's preference lists

    /**
     * Creates a new stable marriage problem instance with given preference lists.
     */
    public StableMarriageProblem(int[][] menPrefs, int[][] womenPrefs) {
        this.n = menPrefs.length;
        if (n != womenPrefs.length || n != menPrefs[0].length || n != womenPrefs[0].length) {
            throw new IllegalArgumentException("Preference arrays must be n x n");
        }
        
        this.menPrefs = new int[n][n];
        this.womenPrefs = new int[n][n];
        
        // Deep copy to ensure immutability
        for (int i = 0; i < n; i++) {
            System.arraycopy(menPrefs[i], 0, this.menPrefs[i], 0, n);
            System.arraycopy(womenPrefs[i], 0, this.womenPrefs[i], 0, n);
        }
    }
    
    /**
     * Returns the initial state with no matches.
     */
    @Override
    public StableMarriageState getInitialState() {
        return new StableMarriageState(n, menPrefs, womenPrefs);
    }

    /**
     * Returns true if the current matching has any unstable pairs.
     */
    @Override
    public boolean Forbidden(StableMarriageState state) {      
        // Check all possible pairs for instability
        for (int man = 0; man < state.n; man++){
            for (int woman = 0; woman < state.n; woman++){
                if (state.menPartner[man] == woman){
                    continue; // Skip if already matched to each other
                }

                // Check if this pair would prefer each other over current partners
                boolean manPrefersWoman = state.manPrefers(man, woman);
                boolean womanPrefersMan = state.womanPrefers(woman, man);

                if (manPrefersWoman && womanPrefersMan) {
                    return true; // Found unstable pair
                }
            }
        }
        
        return false; 
    }
    
    /**
     * Fixes unstable pairs by creating new matches, distributed across threads.
     */
    @Override
    public StableMarriageState Ensure(StableMarriageState state, int threadId, int totalThreads) {
        StableMarriageState currentState = state;

        // Distribute men among threads for parallel processing
        for (int man = threadId; man < state.n; man += totalThreads) {
            for (int woman = 0; woman < state.n; woman++) {
                if (currentState.menPartner[man] == woman) {
                    continue; // Skip if already matched
                }

                // Check for unstable pair and fix it
                boolean manPrefersWoman = currentState.manPrefers(man, woman);
                boolean womanPrefersMan = currentState.womanPrefers(woman, man);

                if (manPrefersWoman && womanPrefersMan) {
                    currentState = currentState.withMatch(man, woman); // Fix unstable pair
                }
            }
        }

        return currentState;
    }
    
    /**
     * Makes progress by having unmatched men propose to their preferred women.
     */
    @Override
    public StableMarriageState Advance(StableMarriageState state, int threadId, int totalThreads) {       
        StableMarriageState currentState = state;

        // Distribute unmatched men among threads for parallel proposals
        for (int man = threadId; man < state.n; man += totalThreads) {
            if (currentState.menPartner[man] == -1) { // If man is unmatched
                // Try preferences in order until finding an acceptable match
                for (int prefRank = 0; prefRank < state.n; prefRank++) {
                    int woman = currentState.menPrefs[man][prefRank];

                    if (currentState.womenPartner[woman] == -1) {
                        // Woman is unmatched, create match
                        currentState = currentState.withMatch(man, woman);
                        break;
                    }
                    else if (currentState.womanPrefers(woman, man)) {
                        // Woman prefers this man over current partner
                        currentState = currentState.withMatch(man, woman);
                        break;
                    }
                }
            }
        }

        return currentState;
    }
    
    /**
     * Returns true if the matching is both stable and complete.
     */
    @Override
    public boolean isSolution(StableMarriageState state) {
        return !Forbidden(state) && state.isComplete();
    }
    
    /**
     * Merges two partial matchings from different threads, resolving conflicts by preferences.
     */
    @Override
    public StableMarriageState merge(StableMarriageState state1, StableMarriageState state2) {
        StableMarriageState result = new StableMarriageState(state1.n, state1.menPrefs, state1.womenPrefs);

        // First, add all matches from state1
        for (int man = 0; man < state1.n; man++){
            if (state1.menPartner[man] != -1) {
                int woman = state1.menPartner[man];
                result = result.withMatch(man, woman);
            }
        }

        // Then, add matches from state2, resolving conflicts by preference
        for (int man = 0; man < state2.n; man++){
            if (state2.menPartner[man] != -1) {
                int woman = state2.menPartner[man];

                if (result.menPartner[man] == -1) { 
                    // Man is unmatched in result, check if woman is available or prefers him
                    if (result.womenPartner[woman] == -1 || result.womanPrefers(woman, man)) {
                        result = result.withMatch(man, woman);
                    }
                }
                else {
                    // Man is already matched, check if he prefers the new woman
                    int currentWoman = result.menPartner[man];

                    if (result.menRanking[man][woman] < result.menRanking[man][currentWoman]) {
                        if (result.womenPartner[woman] == -1 || result.womanPrefers(woman, man)) {
                            result = result.withMatch(man, woman);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Main method demonstrating the stable marriage problem with test cases.
     */
    public static void main(String[] args) {
        System.out.println("=== Stable Marriage Problem Example ===\n");

        // Test Case 1: Simple 3x3 matching
        testCase1();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 2: More complex 3x3 matching
        testCase2();
    }

    /**
     * Test case 1: Classic 3x3 stable marriage scenario.
     */
    private static void testCase1(){
        System.out.println("Test Case 1: Classic 3x3 Stable Marriage");
        System.out.println("----------------------------------------");

        int[][] menPrefs = {
            {0, 1, 2},  
            {1, 2, 0},   
            {0, 1, 2}   
        };

        int[][] womenPrefs = {
            {1, 0, 2},  
            {0, 2, 1}, 
            {0, 1, 2} 
        };
        
        int numThreads = 4;
        int maxIterations = 100;
        
        StableMarriageProblem problem = new StableMarriageProblem(menPrefs, womenPrefs);
        StableMarriageState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected: One possible stable matching is M0↔W0, M1↔W2, M2↔W1");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Test case 2: Complex 3x3 stable marriage scenario.
     */
    private static void testCase2() {
        System.out.println("Test Case 2: Complex 3x3 Stable Marriage");
        System.out.println("----------------------------------------");
        
        int[][] menPrefs = {
            {2, 1, 0},
            {0, 1, 2},
            {1, 0, 2} 
        };
        
        int[][] womenPrefs = {
            {2, 1, 0}, 
            {0, 2, 1}, 
            {1, 0, 2} 
        };
        
        int numThreads = 4;
        int maxIterations = 100;
        
        StableMarriageProblem problem = new StableMarriageProblem(menPrefs, womenPrefs);
        StableMarriageState initial = problem.getInitialState();
        
        System.out.println("Initial state: " + initial);
        System.out.println("Threads: " + numThreads);
        System.out.println("Expected: One possible stable matching is (M0,W2), (M1,W0), (M2,W1)");
        
        solveProblem(problem, numThreads, maxIterations);
    }

    /**
     * Solves the stable marriage problem using the LLP framework and prints results.
     */
    private static void solveProblem(StableMarriageProblem problem, int numThreads, int maxIterations) {
        System.out.println("\n--- Framework Solution ---");
        
        LLPSolver<StableMarriageState> solver = null;
        
        try {
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            System.out.println("Solving with LLP framework...");
            
            long startTime = System.currentTimeMillis();
            StableMarriageState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\nSolution found!");
            printSolution(solution);
            System.out.println("\nExecution time: " + (endTime - startTime) + "ms");
            System.out.println("Is valid solution? " + problem.isSolution(solution));
            System.out.println("Is forbidden? " + problem.Forbidden(solution));
            System.out.println("Is complete? " + solution.isComplete());
            
            // Verify no unstable pairs exist
            verifyStability(solution);
            
            // Get statistics
            LLPSolver.ExecutionStats stats = solver.getExecutionStats();
            if (stats != null) {
                System.out.println("Total iterations: " + stats.getIterationCount());
                System.out.println("Converged: " + stats.hasConverged());
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printSolution(StableMarriageState solution) {
        System.out.println("Final matching:");
        for (int man = 0; man < solution.n; man++) {
            int woman = solution.menPartner[man];
            if (woman != -1) {
                System.out.printf("  Man %d - Woman %d\n", man, woman);
            } else {
                System.out.printf("  Man %d is unmatched\n", man);
            }
        }
    }

    private static void verifyStability(StableMarriageState state) {
        System.out.println("\nStability verification:");
        boolean hasUnstablePair = false;
        
        for (int man = 0; man < state.n; man++) {
            for (int woman = 0; woman < state.n; woman++) {
                if (state.menPartner[man] == woman) continue; // Already matched
                
                boolean manPrefersWoman = state.manPrefers(man, woman);
                boolean womanPrefersMan = state.womanPrefers(woman, man);
                
                if (manPrefersWoman && womanPrefersMan) {
                    System.out.printf("Unstable pair found: Man %d prefers Woman %d over his current partner\n", man, woman);
                    hasUnstablePair = true;
                }
            }
        }
        
        if (!hasUnstablePair) {
            System.out.println("No unstable pairs found - matching is stable!");
        }
    }
}
