# LLP-Java-Algorithms

A comprehensive Java framework for implementing parallel LLP (Least Lattice Predicate) algorithms to solve various computational problems.

## Overview

This project provides a robust, production-ready framework for solving problems using the LLP parallel algorithm paradigm. The LLP algorithm is based on the concept of lattice theory and uses three fundamental operations:
- **Forbidden**: Determines if a state violates problem constraints
- **Ensure**: Fixes states to satisfy local constraints
- **Advance**: Makes progress toward the solution

### Framework Features

✅ **Thread-safe state management** with `LLPState`  
✅ **Parallel execution orchestration** via `LLPEngine`  
✅ **Flexible configuration** through `LLPConfiguration`  
✅ **Synchronization primitives** including barriers and locks  
✅ **Termination detection** with convergence monitoring  
✅ **High-level API** through `LLPSolver`  
✅ **Comprehensive documentation** and examples

## Project Structure

```
LLP-Java-Algorithms/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── llp/
│   │               ├── algorithm/         # Core LLP API
│   │               │   ├── LLPProblem.java
│   │               │   └── LLPSolver.java
│   │               ├── framework/         # Framework infrastructure
│   │               │   ├── LLPEngine.java
│   │               │   ├── LLPState.java
│   │               │   ├── LLPBarrier.java
│   │               │   ├── LLPConfiguration.java
│   │               │   └── LLPTerminationDetector.java
│   │               ├── problems/          # Problem implementations (skeletons)
│   │               │   ├── StableMarriageProblem.java
│   │               │   ├── ParallelPrefixProblem.java
│   │               │   ├── ConnectedComponentsProblem.java
│   │               │   ├── BellmanFordProblem.java
│   │               │   ├── JohnsonProblem.java
│   │               │   └── BoruvkaProblem.java
│   │               └── examples/          # Example usage
│   │                   └── SimpleLLPExample.java
│   └── test/
│       └── java/
│           └── com/
│               └── llp/                   # Test cases
├── pom.xml                                # Maven configuration
├── LIBRARY_API.md                         # Comprehensive API documentation
├── build.sh                               # Build script
├── run_example.sh                         # Run example script
└── test.sh                                # Test script
```

## LLP Algorithm Core Concepts

### The Three Core Methods

When implementing a problem using the LLP framework, you need to define these three methods:

#### 1. **Forbidden(state) → boolean**
This predicate determines if a given configuration is invalid or violates problem constraints.

**Purpose**: 
- Detect states that violate problem invariants
- Identify configurations that need correction

**Example use cases**:
- In Stable Marriage: Check if there are unstable pairs
- In Bellman-Ford: Check if distances violate triangle inequality
- In Connected Components: Check if component labels are inconsistent

**Implementation tips**:
- Should be efficient as it may be called frequently
- Must correctly identify all constraint violations
- Should return `true` only when constraints are violated

#### 2. **Ensure(state) → state**
This operation modifies the state to satisfy local constraints and remove forbidden configurations.

**Purpose**:
- Fix states that violate constraints
- Maintain problem invariants
- Ensure forward progress doesn't create permanent violations

**Example use cases**:
- In Stable Marriage: Resolve unstable pairs
- In Bellman-Ford: Update distances that violate triangle inequality
- In Connected Components: Merge component labels

**Implementation tips**:
- Should fix all detected violations
- Must maintain monotonic progress in the lattice
- May need to propagate fixes to related elements

#### 3. **Advance(state) → state**
This operation moves the state forward toward the solution, potentially creating new forbidden configurations.

**Purpose**:
- Make progress toward the solution
- Explore the solution space
- Move up in the lattice ordering

**Example use cases**:
- In Stable Marriage: Propose new matches
- In Bellman-Ford: Relax edges
- In Connected Components: Propagate labels

**Implementation tips**:
- Focus on making progress, not maintaining constraints
- May create forbidden states (that's okay - Ensure will fix them)
- Should eventually lead to convergence when alternated with Ensure

## Quick Start

### Basic Usage

```java
import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;
import com.llp.framework.LLPConfiguration;

// 1. Implement your problem
public class MyProblem implements LLPProblem<MyState> {
    @Override
    public boolean Forbidden(MyState state) {
        // TODO: Implement constraint checking
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public MyState Ensure(MyState state) {
        // TODO: Implement constraint fixing
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public MyState Advance(MyState state) {
        // TODO: Implement progress logic
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public MyState getInitialState() {
        // TODO: Return initial state
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public boolean isSolution(MyState state) {
        // TODO: Check if solution
        throw new UnsupportedOperationException("Not implemented");
    }
}

// 2. Configure and solve
public static void main(String[] args) {
    try {
        MyProblem problem = new MyProblem();
        
        // Configure the solver
        LLPConfiguration config = new LLPConfiguration()
            .setNumThreads(4)
            .setMaxIterations(1000);
        
        // Create solver and execute
        LLPSolver<MyState> solver = new LLPSolver<>(problem, config);
        MyState solution = solver.solve();
        
        // Get statistics
        System.out.println("Iterations: " + 
            solver.getTerminationDetector().getIterationCount());
        
        solver.shutdown();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### Complete API Documentation

See [LIBRARY_API.md](LIBRARY_API.md) for comprehensive framework documentation including:
- Detailed API reference for all classes
- Usage patterns and best practices
- Performance tuning guidelines
- Thread safety considerations
- Troubleshooting guide

## Example: Simple Counter Problem

See `src/main/java/com/llp/examples/SimpleLLPExample.java` for a complete working example that demonstrates:
- How to define a state class
- How to implement the three core methods
- How to use the LLP framework

Run the example:
```bash
./run_example.sh
```

## Problems to Implement

This assignment requires implementing the following problems using the LLP framework:

1. **Stable Marriage Problem** (`StableMarriageProblem.java`)
   - Match n men and n women based on preference lists
   - Ensure no unstable pairs exist

2. **Parallel Prefix Problem** (`ParallelPrefixProblem.java`)
   - Compute all prefix sums of an array in parallel
   - Also known as scan operation

3. **Connected Components** (`ConnectedComponentsProblem.java`)
   - Find connected components in an undirected graph
   - Use the fast parallel algorithm

4. **Bellman-Ford Algorithm** (`BellmanFordProblem.java`)
   - Find shortest paths from a source vertex
   - Handle negative edge weights

5. **Johnson's Algorithm** (`JohnsonProblem.java`)
   - Find all-pairs shortest paths
   - Combine reweighting with Bellman-Ford/Dijkstra

6. **Boruvka's Algorithm** (`BoruvkaProblem.java`)
   - Find minimum spanning tree/forest
   - Use parallel edge selection

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build the Project
```bash
./build.sh
```

Or manually:
```bash
mvn clean compile
```

### Run Tests
```bash
./test.sh
```

Or manually:
```bash
mvn test
```

### Run Example
```bash
./run_example.sh
```

Or manually:
```bash
mvn compile exec:java -Dexec.mainClass="com.llp.examples.SimpleLLPExample"
```

## Development Tips

1. **Start with the state representation**: Think carefully about how to represent the problem state. It should capture all necessary information.

2. **Implement Forbidden first**: Clearly define what makes a state invalid. This guides the implementation of Ensure.

3. **Make Ensure correct**: Ensure should fix all violations detected by Forbidden. Test this relationship thoroughly.

4. **Make Advance progressive**: Advance should move toward the solution. It's okay if it creates forbidden states temporarily.

5. **Test incrementally**: Test each method independently before combining them in the full algorithm.

6. **Consider parallelism**: Think about which operations can be performed in parallel on independent parts of the state.

## Framework Architecture

### Core Components

1. **LLPProblem Interface**: Defines the three core operations (Forbidden, Ensure, Advance)
2. **LLPSolver**: High-level API for executing LLP algorithms
3. **LLPEngine**: Orchestrates parallel execution across threads
4. **LLPState**: Thread-safe state container with synchronization
5. **LLPBarrier**: Synchronization barrier for thread coordination
6. **LLPTerminationDetector**: Monitors convergence and termination conditions
7. **LLPConfiguration**: Flexible configuration builder

### Execution Flow

```
Initialize State
     ↓
┌────────────────────┐
│ For each iteration │←─────┐
└────────────────────┘      │
     ↓                       │
┌────────────────────┐      │
│ Parallel Advance   │      │
│ across threads     │      │
└────────────────────┘      │
     ↓                       │
┌────────────────────┐      │
│ Barrier Sync       │      │
└────────────────────┘      │
     ↓                       │
┌────────────────────┐      │
│ Parallel Ensure    │      │
│ across threads     │      │
└────────────────────┘      │
     ↓                       │
┌────────────────────┐      │
│ Check Termination  │      │
└────────────────────┘      │
     ↓                       │
   Not Done ─────────────────┘
     ↓
   Done
     ↓
  Return Solution
```

## Implementation Notes

- The framework provides complete parallel execution infrastructure
- Problem implementations must provide the three core methods (Forbidden, Ensure, Advance)
- Each problem skeleton includes TODO comments indicating what needs to be implemented
- Focus on correctness first, then optimize for performance
- Use appropriate data structures for efficient parallel access
- The framework handles all thread management, synchronization, and termination detection

## Submission Requirements

When submitting your assignment:
1. Source code for all implemented problems
2. Test cases demonstrating correctness
3. A script file that runs your program
4. Partner name (if working in a pair)
5. Any test case generation programs you created

## References

- Lattice theory and LLP algorithms
- Parallel algorithm design patterns
- Problem-specific algorithm documentation

## License

This project is for educational purposes as part of a parallel algorithms course assignment.