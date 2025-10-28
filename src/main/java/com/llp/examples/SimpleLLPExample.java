package com.llp.examples;

import com.llp.framework.LLPConfiguration;
import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

/**
 * Enhanced LLP Example with detailed explanations.
 * 
 * This example demonstrates the core LLP concepts:
 * 1. How the three predicates (Forbidden, Ensure, Advance) work together
 * 2. How states flow through the system
 * 3. How the framework coordinates parallel execution
 * 4. Manual step-by-step execution vs framework execution
 */
public class SimpleLLPExample {
    
    /**
     * State represents the "current situation" of our problem.
     * 
     * Think of it as a snapshot of progress:
     * - value: How far we've counted (current progress)
     * - target: Where we want to end up (goal)
     * 
     * KEY CONCEPT: States are IMMUTABLE - we create new ones instead of modifying existing ones.
     */
    static class CounterState {
        final int value;    // Current count
        final int target;   // Goal count
        
        public CounterState(int value, int target) {
            this.value = value;
            this.target = target;
        }
        
        // Helper method to create a new state with different value
        public CounterState withValue(int newValue) {
            return new CounterState(newValue, this.target);
        }
        
        @Override
        public String toString() {
            return String.format("Counter{value=%d, target=%d} %s", 
                value, target, 
                value == target ? "✓GOAL" : value > target ? "✗VIOLATION" : "→progress");
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CounterState)) return false;
            CounterState other = (CounterState) obj;
            return value == other.value && target == other.target;
        }
    }
    
    /**
     * The Problem Implementation - This is where YOU define the algorithm logic.
     * 
     * The three methods work together in a cycle:
     * 1. Advance: "How do we make progress?"
     * 2. Ensure: "How do we fix violations?"  
     * 3. Forbidden: "Are we violating constraints?"
     */
    static class CounterProblem implements LLPProblem<CounterState> {
        
        private final int target;
        
        public CounterProblem(int target) {
            this.target = target;
        }
        
        /**
         * FORBIDDEN: "Is this state violating our rules?"
         * 
         * This is our constraint checker. In counting, our rule is:
         * "Never count higher than the target"
         * 
         * Returns true if constraints are violated, false if state is valid.
         */
        @Override
        public boolean Forbidden(CounterState state) {
            boolean violation = state.value > state.target;
            
            // Helpful debug output
            if (violation) {
                System.out.println("    🚫 VIOLATION: value " + state.value + " > target " + state.target);
            }
            
            return violation;
        }
        
        /**
         * ENSURE: "How do we fix violations?"
         * 
         * This method repairs any constraint violations.
         * If Forbidden(state) returns true, Ensure must fix it.
         * If no violations exist, return the state unchanged.
         * 
         * Think of it as: "Make this state valid again"
         */
        @Override
        public CounterState Ensure(CounterState state) {
            if (Forbidden(state)) {
                // Fix the violation by capping at target
                CounterState fixed = state.withValue(state.target);
                System.out.println("    🔧 FIXED: " + state + " → " + fixed);
                return fixed;
            }
            
            // No violations to fix
            System.out.println("    ✓ VALID: No violations to fix");
            return state;
        }
        
        /**
         * ADVANCE: "How do we make progress toward the solution?"
         * 
         * This method moves us closer to the goal.
         * It's OK if this creates forbidden states - Ensure will fix them.
         * 
         * Think of it as: "Take a step toward the solution"
         */
        @Override
        public CounterState Advance(CounterState state) {
            if (state.value < state.target) {
                CounterState advanced = state.withValue(state.value + 1);
                System.out.println("    📈 ADVANCE: " + state + " → " + advanced);
                return advanced;
            } else {
                System.out.println("    ⏹️  NO ADVANCE: Already at target");
                return state;  // Already at target, can't advance further
            }
        }
        
        /**
         * INITIAL STATE: "Where do we start?"
         * 
         * This defines the starting point of our algorithm.
         */
        @Override
        public CounterState getInitialState() {
            return new CounterState(0, target);
        }
        
        /**
         * IS SOLUTION: "Are we done?"
         * 
         * A solution must satisfy two conditions:
         * 1. No constraint violations (!Forbidden)
         * 2. We've reached our goal (value == target)
         */
        @Override
        public boolean isSolution(CounterState state) {
            boolean isComplete = (state.value == state.target);
            boolean isValid = !Forbidden(state);
            boolean isSolution = isComplete && isValid;
            
            if (isSolution) {
                System.out.println("    🎯 SOLUTION FOUND!");
            }
            
            return isSolution;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        LLP Framework Tutorial        ║");
        System.out.println("╚══════════════════════════════════════╝\n");
        
        System.out.println("📚 LEARNING OBJECTIVES:");
        System.out.println("  1. Understand how Forbidden, Ensure, and Advance work together");
        System.out.println("  2. See how states flow through the LLP cycle");
        System.out.println("  3. Compare manual execution vs framework execution");
        System.out.println("  4. Understand parallel coordination\n");
        
        // Create our problem
        CounterProblem problem = new CounterProblem(5);  // Count from 0 to 5
        
        System.out.println("🎯 PROBLEM: Count from 0 to 5");
        System.out.println("📋 RULES: Never exceed the target");
        System.out.println("🏁 GOAL: Reach exactly the target value\n");
        
        // Part 1: Manual execution to understand the algorithm
        manualLLPExecution(problem);
        
        // Part 2: Framework execution 
        frameworkExecution(problem);
        
        // Part 3: Error demonstration
        demonstrateErrorHandling(problem);
        
        System.out.println("\n📚 TUTORIAL COMPLETE!");
        System.out.println("You now understand how LLP coordinates Forbidden, Ensure, and Advance!");
    }
    
    /**
     * Part 1: Manual step-by-step execution to understand the algorithm flow.
     */
    private static void manualLLPExecution(CounterProblem problem) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      PART 1: MANUAL EXECUTION       ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("🔍 Watch how the three methods work together...\n");
        
        CounterState state = problem.getInitialState();
        System.out.println("🏁 STARTING: " + state + "\n");
        
        for (int iteration = 0; iteration < 10; iteration++) {
            System.out.println("🔄 ITERATION " + iteration + ":");
            System.out.println("  📊 Current: " + state);
            
            // Check current state
            System.out.println("  🔍 Checking Forbidden: " + problem.Forbidden(state));
            System.out.println("  🎯 Checking isSolution: " + problem.isSolution(state));
            
            if (problem.isSolution(state)) {
                System.out.println("\n🎉 SUCCESS! Solution found in " + iteration + " iterations!");
                break;
            }
            
            // The core LLP cycle: Advance → Ensure
            System.out.println("\n  🔄 Executing LLP Cycle:");
            
            // Step 1: Advance (make progress)
            CounterState afterAdvance = problem.Advance(state);
            
            // Step 2: Ensure (fix any violations)
            CounterState afterEnsure = problem.Ensure(afterAdvance);
            
            // Check if we made progress
            if (afterEnsure.equals(state)) {
                System.out.println("\n⚠️  No progress made - algorithm has converged");
                break;
            }
            
            state = afterEnsure;
            System.out.println("  📊 Result: " + state + "\n");
            
            // Add a small delay for readability
            try { Thread.sleep(500); } catch (InterruptedException e) { /* ignore */ }
        }
        
        System.out.println("📈 MANUAL EXECUTION COMPLETE\n");
    }
    
    /**
     * Part 2: Framework execution to show parallel coordination.
     */
    private static void frameworkExecution(CounterProblem problem) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     PART 2: FRAMEWORK EXECUTION     ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("🚀 Now let the framework handle everything...\n");
        
        LLPSolver<CounterState> solver = null;
        
        try {
            // Create solver with custom configuration
            LLPConfiguration config = new LLPConfiguration()
                .setMaxIterations(100)
                .setNumThreads(2);
            solver = new LLPSolver<>(problem, config);
            
            System.out.println("⚙️  CONFIGURATION:");
            System.out.println("  📊 Max Iterations: 100");
            System.out.println("  🧵 Threads: 2");
            System.out.println("  🎯 Problem: Count to " + problem.target + "\n");
            
            System.out.println("🔄 Executing parallel LLP algorithm...");
            
            long startTime = System.currentTimeMillis();
            CounterState solution = solver.solve();
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n🎉 FRAMEWORK EXECUTION COMPLETE!");
            System.out.println("  📊 Final state: " + solution);
            System.out.println("  ⏱️  Execution time: " + (endTime - startTime) + "ms");
            
            // Show execution statistics
            if (solver.getTerminationDetector() != null) {
                System.out.println("  🔄 Iterations: " + solver.getTerminationDetector().getIterationCount());
                System.out.println("  ✅ Converged: " + solver.getTerminationDetector().hasConverged());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Framework execution failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (solver != null) {
                solver.shutdown();
                System.out.println("  🧹 Resources cleaned up");
            }
        }
        
        System.out.println();
    }
    
    /**
     * Part 3: Demonstrate error handling and edge cases.
     */
    private static void demonstrateErrorHandling(CounterProblem problem) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     PART 3: ERROR DEMONSTRATION      ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("🔧 Testing how Ensure fixes violations...\n");
        
        // Create a deliberately forbidden state
        CounterState violatingState = new CounterState(10, 5); // value > target
        
        System.out.println("🚨 TESTING VIOLATION HANDLING:");
        System.out.println("  📊 Forbidden state: " + violatingState);
        System.out.println("  🔍 Is Forbidden? " + problem.Forbidden(violatingState));
        
        // Watch Ensure fix it
        System.out.println("\n🔧 Applying Ensure to fix violation:");
        CounterState fixed = problem.Ensure(violatingState);
        System.out.println("  📊 After Ensure: " + fixed);
        System.out.println("  🔍 Is Forbidden? " + problem.Forbidden(fixed));
        System.out.println("  ✅ Violation successfully repaired!\n");
        
        System.out.println("💡 KEY INSIGHT:");
        System.out.println("  The LLP framework automatically handles violations by calling");
        System.out.println("  Ensure after every Advance operation. This allows Advance to");
        System.out.println("  be aggressive in making progress, knowing that Ensure will");
        System.out.println("  clean up any constraint violations that result.\n");
    }
}
