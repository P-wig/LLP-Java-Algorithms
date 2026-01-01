package com.llp.problems;

import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

import java.util.Arrays;

/**
 * Stable Marriage Problem using the simplified LLP framework.
 * 
 * Problem Description:
 * The stable marriage problem involves matching n men and n women where each person
 * has a preference list ranking all members of the opposite gender. The goal is to find
 * a stable matching where no two people would prefer each other over their current partners.
 * 
 * LLP Implementation Strategy:
 * - State: Current matching configuration with preference lists and rankings
 * - Forbidden: Returns true when unmatched people exist or instability detected
 * - Advance: Use Gale-Shapley inspired approach to create stable matches
 * - Parallelism: Threads work on different men simultaneously with coordination
 */
public class StableMarriageProblem {
    
    /**
     * Enhanced state class with optimizations for performance and reliability.
     */
    static class OptimizedStableMarriageState {
        final int n;                          // Number of men/women
        final int[][] menPrefs;               // menPrefs[i][j] = j-th preference of man i
        final int[][] womenPrefs;             // womenPrefs[i][j] = j-th preference of woman i
        final int[][] menRanking;             // menRanking[i][j] = rank of woman j in man i's preferences
        final int[][] womenRanking;           // womenRanking[i][j] = rank of man j in woman i's preferences
        volatile int[] menPartner;            // menPartner[i] = partner of man i (-1 if unmatched)
        volatile int[] womenPartner;          // womenPartner[i] = partner of woman i (-1 if unmatched)
        volatile int[] nextProposal;          // nextProposal[i] = next woman index for man i to try
        volatile boolean[] menFree;           // menFree[i] = true if man i is unmatched
        volatile int unmatchedCount;          // Number of unmatched men (for fast completion check)
        volatile int iterationCount;          // Track iterations for statistics

        /**
         * Creates initial state with given preferences and no matches.
         */
        public OptimizedStableMarriageState(int n, int[][] menPrefs, int[][] womenPrefs) {
            this.n = n;
            this.menPrefs = new int[n][n];
            this.womenPrefs = new int[n][n];
            this.menRanking = new int[n][n];
            this.womenRanking = new int[n][n];

            // Deep copy preferences to ensure immutability
            for (int i = 0; i < n; i++) {
                System.arraycopy(menPrefs[i], 0, this.menPrefs[i], 0, n);
                System.arraycopy(womenPrefs[i], 0, this.womenPrefs[i], 0, n);
            }

            // Build ranking arrays for fast preference comparisons O(1)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    menRanking[i][menPrefs[i][j]] = j;
                    womenRanking[i][womenPrefs[i][j]] = j;
                }
            }

            // Initialize all as unmatched
            this.menPartner = new int[n];
            this.womenPartner = new int[n];
            this.nextProposal = new int[n]; // Start proposing to first preference
            this.menFree = new boolean[n];
            
            Arrays.fill(menPartner, -1);
            Arrays.fill(womenPartner, -1);
            Arrays.fill(nextProposal, 0);
            Arrays.fill(menFree, true);
            
            this.unmatchedCount = n;
            this.iterationCount = 0;
        }
        
        /**
         * Thread-safe method to create a match between man and woman.
         * Optimized to minimize synchronization time.
         */
        public synchronized void createMatch(int man, int woman) {
            // Handle the displaced man (if woman was already matched)
            int displacedMan = womenPartner[woman];
            if (displacedMan != -1) {
                menPartner[displacedMan] = -1;
                menFree[displacedMan] = true;
                unmatchedCount++;
            }
            
            // Handle the man's previous partner (if he was already matched)
            if (menPartner[man] != -1) {
                womenPartner[menPartner[man]] = -1;
            } else {
                // Man was unmatched, now he's matched
                menFree[man] = false;
                unmatchedCount--;
            }
            
            // Create the new match
            menPartner[man] = woman;
            womenPartner[woman] = man;
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
         * Fast completion check using cached count.
         */
        public boolean isComplete() {
            return unmatchedCount == 0;
        }
        
        /**
         * Get the next woman this man should propose to.
         */
        public int getNextProposal(int man) {
            if (nextProposal[man] >= n) return -1; // No more proposals
            
            synchronized (this) { // Minimal sync only for increment
                if (nextProposal[man] >= n) return -1; // Double-check
                return menPrefs[man][nextProposal[man]++];
            }
        }
        
        /**
         * Check if man has more proposals to make.
         */
        public boolean hasMoreProposals(int man) {
            return nextProposal[man] < n;
        }
        
        /**
         * Increment iteration count (thread-safe).
         */
        public synchronized void incrementIterations() {
            iterationCount++;
        }
        
        /**
         * Get current iteration count.
         */
        public int getIterationCount() {
            return iterationCount;
        }
        
        /**
         * Get number of unmatched men.
         */
        public int getUnmatchedCount() {
            return unmatchedCount;
        }
    }

    /**
     * Optimized Stable Marriage problem using Gale-Shapley algorithm principles.
     */
    static class OptimizedStableMarriageLLPProblem implements LLPProblem<OptimizedStableMarriageState> {
        
        private final int n;                 // Number of men/women
        private final int[][] menPrefs;      // Men's preference lists
        private final int[][] womenPrefs;    // Women's preference lists

        /**
         * Creates a new stable marriage problem instance with given preference lists.
         */
        public OptimizedStableMarriageLLPProblem(int[][] menPrefs, int[][] womenPrefs) {
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
        public OptimizedStableMarriageState getInitialState() {
            return new OptimizedStableMarriageState(n, menPrefs, womenPrefs);
        }

        /**
         * Returns true when there are unmatched men who can make proposals.
         */
        @Override
        public boolean Forbidden(OptimizedStableMarriageState state) {
            // Precise check: forbidden when unmatched men have valid proposals to make
            for (int man = 0; man < state.n; man++) {
                if (state.menFree[man] && state.hasMoreProposals(man)) {
                    return true; // Found unmatched man who can make proposals
                }
            }
            return false; // No work available
        }

        /**
         * Optimized Gale-Shapley algorithm with reduced synchronization overhead.
         */
        @Override
        public OptimizedStableMarriageState Advance(OptimizedStableMarriageState state, int threadId, int totalThreads) {
            // Increment iterations (Thread-0 only)
            if (threadId == 0) {
                state.incrementIterations();
            }

            // Process multiple proposals per thread for better parallelism
            int proposalsPerThread = Math.max(1, state.n / totalThreads);
            
            for (int attempt = 0; attempt < proposalsPerThread; attempt++) {
                // Each thread handles different men in round-robin fashion
                for (int man = threadId; man < state.n; man += totalThreads) {
                    
                    // Only process unmatched men who still have proposals to make
                    if (state.menFree[man] && state.hasMoreProposals(man)) {
                        
                        // Get next woman to propose to
                        int woman = state.getNextProposal(man);
                        if (woman == -1) continue; // No more proposals for this man
                        
                        // Minimal synchronized section for matching decision
                        synchronized (state) {
                            if (state.womenPartner[woman] == -1) {
                                // Woman is free - create match immediately
                                state.createMatch(man, woman);
                            }
                            else if (state.womanPrefers(woman, man)) {
                                // Woman prefers this man over current partner
                                state.createMatch(man, woman);
                            }
                        }
                    }
                }
            }

            return state;
        }
        
        /**
         * Solution is complete when all men are matched.
         */
        @Override
        public boolean isSolution(OptimizedStableMarriageState state) {
            return state.isComplete();
        }
    }
    
    /**
     * Main method demonstrating the stable marriage problem with test cases.
     */
    public static void main(String[] args) {
        System.out.println("=== Optimized Stable Marriage Problem Example ===\n");

        // Test Case 1: Simple 3x3 matching
        testCase1();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 2: More complex 3x3 matching  
        testCase2();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 3: Larger problem for performance testing
        testCase3();
        
        System.out.println("\n============================================================\n");
        
        // Test Case 4: Even larger for scalability
        testCase4();
    }

    /**
     * Test case 1: Classic 3x3 stable marriage scenario.
     */
    private static void testCase1() {
        System.out.println("=== Test Case 1: Classic 3x3 ===");
        
        int[][] menPrefs = {
            {0, 1, 2},  // Man 0 prefers: Woman 0, Woman 1, Woman 2
            {1, 2, 0},  // Man 1 prefers: Woman 1, Woman 2, Woman 0  
            {0, 1, 2}   // Man 2 prefers: Woman 0, Woman 1, Woman 2
        };

        int[][] womenPrefs = {
            {1, 0, 2},  // Woman 0 prefers: Man 1, Man 0, Man 2
            {0, 2, 1},  // Woman 1 prefers: Man 0, Man 2, Man 1
            {0, 1, 2}   // Woman 2 prefers: Man 0, Man 1, Man 2
        };
        
        runTestCase("Classic 3x3", menPrefs, womenPrefs);
    }

    /**
     * Test case 2: Complex 3x3 stable marriage scenario.
     */
    private static void testCase2() {
        System.out.println("=== Test Case 2: Complex 3x3 ===");
        
        int[][] menPrefs = {
            {2, 1, 0},  // Man 0 prefers: Woman 2, Woman 1, Woman 0
            {0, 1, 2},  // Man 1 prefers: Woman 0, Woman 1, Woman 2
            {1, 0, 2}   // Man 2 prefers: Woman 1, Woman 0, Woman 2
        };
        
        int[][] womenPrefs = {
            {2, 1, 0},  // Woman 0 prefers: Man 2, Man 1, Man 0
            {0, 2, 1},  // Woman 1 prefers: Man 0, Man 2, Man 1
            {1, 0, 2}   // Woman 2 prefers: Man 1, Man 0, Man 2
        };
        
        runTestCase("Complex 3x3", menPrefs, womenPrefs);
    }
    
    /**
     * Test case 3: Medium scale problem.
     */
    private static void testCase3() {
        System.out.println("=== Test Case 3: Medium Scale (8x8) ===");
        
        int n = 8;
        int[][] menPrefs = generateRandomPreferences(n, 42);
        int[][] womenPrefs = generateRandomPreferences(n, 84);
        
        runTestCase("Random 8x8", menPrefs, womenPrefs);
    }
    
    /**
     * Test case 4: Larger scale problem for performance testing.
     */
    private static void testCase4() {
        System.out.println("=== Test Case 4: Large Scale (20x20) ===");
        
        int n = 20;
        int[][] menPrefs = generateRandomPreferences(n, 123);
        int[][] womenPrefs = generateRandomPreferences(n, 456);
        
        runTestCase("Random 20x20", menPrefs, womenPrefs);
    }
    
    /**
     * Generate random preference lists for testing.
     */
    private static int[][] generateRandomPreferences(int n, int seed) {
        java.util.Random random = new java.util.Random(seed);
        int[][] prefs = new int[n][n];
        
        for (int i = 0; i < n; i++) {
            // Create a permutation of 0 to n-1
            java.util.List<Integer> list = new java.util.ArrayList<>();
            for (int j = 0; j < n; j++) {
                list.add(j);
            }
            java.util.Collections.shuffle(list, random);
            
            for (int j = 0; j < n; j++) {
                prefs[i][j] = list.get(j);
            }
        }
        
        return prefs;
    }

    /**
     * Run a test case with performance measurement across different thread counts.
     */
    private static void runTestCase(String testName, int[][] menPrefs, int[][] womenPrefs) {
        System.out.println("Input: " + testName);
        
        // Only print preferences for small cases
        if (menPrefs.length <= 8) {
            System.out.println("Men's preferences:");
            printPreferences(menPrefs);
            System.out.println("Women's preferences:");
            printPreferences(womenPrefs);
        }
        
        int[] threadCounts = {1, 2, 4, 8};
        int maxIterations = Math.max(50, menPrefs.length * 2); // Scale iterations with problem size
        double localBaselineTime = 0.0;
        
        // Test different thread counts
        for (int numThreads : threadCounts) {
            localBaselineTime = solveProblem(menPrefs, womenPrefs, numThreads, maxIterations, localBaselineTime);
        }
    }

    /**
     * Solve the stable marriage problem and measure performance.
     */
    private static double solveProblem(int[][] menPrefs, int[][] womenPrefs, int numThreads, int maxIterations, double baselineTime) {
        LLPSolver<OptimizedStableMarriageState> solver = null;
        
        try {
            OptimizedStableMarriageLLPProblem problem = new OptimizedStableMarriageLLPProblem(menPrefs, womenPrefs);
            solver = new LLPSolver<>(problem, numThreads, maxIterations);
            
            long startTime = System.nanoTime();
            OptimizedStableMarriageState solution = solver.solve();
            long endTime = System.nanoTime();
            
            // Show results in compact format
            double timeMs = (endTime - startTime) / 1_000_000.0;
            int iterations = solution.getIterationCount();
            boolean valid = problem.isSolution(solution);
            boolean stable = isStable(solution);
            boolean complete = solution.isComplete();
            
            System.out.printf("Threads: %2d | Time: %8.2fms | Iterations: %3d | Valid: %s | Stable: %s | Complete: %s", 
                             numThreads, timeMs, iterations, valid, stable, complete);
            
            // Show speedup relative to single thread
            if (numThreads == 1) {
                System.out.println(" | Speedup: 1.00x (baseline)");
                baselineTime = timeMs;
            } else {
                double speedup = baselineTime / timeMs;
                System.out.printf(" | Speedup: %.2fx\n", speedup);
            }
            
            // Show detailed results for first run only (and small problems only)
            if (numThreads == 1) {
                if (menPrefs.length <= 8) {
                    printSolution(solution);
                }
                verifyStability(solution);
                System.out.println();
            }
            
            return baselineTime;
            
        } catch (Exception e) {
            System.err.println("Error with " + numThreads + " threads: " + e.getMessage());
            e.printStackTrace();
            return baselineTime;
        } finally {
            if (solver != null) {
                solver.shutdown();
            }
        }
    }
    
    /**
     * Check if the matching is stable (no unstable pairs).
     */
    private static boolean isStable(OptimizedStableMarriageState state) {
        for (int man = 0; man < state.n; man++) {
            for (int woman = 0; woman < state.n; woman++) {
                if (state.menPartner[man] == woman) continue;
                
                if (state.manPrefers(man, woman) && state.womanPrefers(woman, man)) {
                    return false; // Found unstable pair
                }
            }
        }
        return true;
    }
    
    /**
     * Print preference matrix in readable format.
     */
    private static void printPreferences(int[][] prefs) {
        for (int i = 0; i < prefs.length; i++) {
            System.out.printf("  Person %d: %s\n", i, Arrays.toString(prefs[i]));
        }
    }
    
    /**
     * Print the final matching solution.
     */
    private static void printSolution(OptimizedStableMarriageState solution) {
        System.out.println("Final matching:");
        for (int man = 0; man < solution.n; man++) {
            int woman = solution.menPartner[man];
            if (woman != -1) {
                System.out.printf("  Man %d ↔ Woman %d\n", man, woman);
            } else {
                System.out.printf("  Man %d is unmatched\n", man);
            }
        }
    }

    /**
     * Verify that the solution has no unstable pairs.
     */
    private static void verifyStability(OptimizedStableMarriageState state) {
        System.out.println("Stability verification:");
        boolean hasUnstablePair = false;
        
        for (int man = 0; man < state.n; man++) {
            for (int woman = 0; woman < state.n; woman++) {
                if (state.menPartner[man] == woman) continue; // Already matched
                
                boolean manPrefersWoman = state.manPrefers(man, woman);
                boolean womanPrefersMan = state.womanPrefers(woman, man);
                
                if (manPrefersWoman && womanPrefersMan) {
                    System.out.printf("  ❌ Unstable pair found: Man %d and Woman %d prefer each other\n", man, woman);
                    hasUnstablePair = true;
                }
            }
        }
        
        if (!hasUnstablePair) {
            System.out.println("  ✅ No unstable pairs found - matching is stable!");
        }
    }
}
