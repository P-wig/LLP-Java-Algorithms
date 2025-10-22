# LLP Library Framework Overview

## What This Repository Provides

This repository contains a **complete, production-ready LLP (Least Lattice Predicate) framework** for implementing parallel algorithms in Java. The framework provides all the infrastructure needed to implement LLP-based solutions, but **intentionally leaves the specific problem implementations as TODO items**.

## Framework Components

### Core Infrastructure (✓ Implemented)

1. **`LLPProblem<T>` Interface** (`com.llp.algorithm`)
   - Defines the contract for all LLP problems
   - Three core methods: `Forbidden()`, `Ensure()`, `Advance()`
   - Additional methods: `getInitialState()`, `isSolution()`
   - Comprehensive javadoc with implementation guidelines

2. **`LLPSolver<T>` Class** (`com.llp.algorithm`)
   - High-level API for solving LLP problems
   - Automatic resource management
   - Configurable thread pools and execution parameters
   - Returns execution statistics

3. **`LLPEngine<T>` Class** (`com.llp.framework`)
   - Core parallel execution orchestrator
   - Manages thread coordination and synchronization
   - Implements the parallel LLP algorithm loop
   - Handles phase transitions (Advance → Ensure)

4. **`LLPState<T>` Class** (`com.llp.framework`)
   - Thread-safe state container
   - Read/write lock support
   - Version tracking for change detection
   - Manual and automatic locking options

5. **`LLPBarrier` Class** (`com.llp.framework`)
   - Synchronization barrier for thread coordination
   - Ensures all threads reach synchronization points
   - Wraps Java's CyclicBarrier with LLP-specific API

6. **`LLPTerminationDetector` Class** (`com.llp.framework`)
   - Monitors convergence conditions
   - Tracks iteration counts
   - Supports maximum iteration limits
   - Provides force-stop capability

7. **`LLPConfiguration` Class** (`com.llp.framework`)
   - Fluent configuration API
   - Thread count settings
   - Iteration limits
   - Timeout configuration
   - Logging control

### Problem Skeletons (📝 TODO Templates)

Six well-documented problem skeleton classes in `com.llp.problems`:

1. **`StableMarriageProblem`** - Stable matching problem
2. **`ParallelPrefixProblem`** - Parallel scan/prefix sum
3. **`ConnectedComponentsProblem`** - Graph connectivity
4. **`BellmanFordProblem`** - Single-source shortest paths
5. **`JohnsonProblem`** - All-pairs shortest paths
6. **`BoruvkaProblem`** - Minimum spanning tree

Each skeleton includes:
- Problem description
- State representation guidelines
- Implementation guide for each method
- Example usage code
- Clear TODO markers
- Reference links

### Documentation (📚 Complete)

- **`README.md`** - Overview, quick start, project structure
- **`LIBRARY_API.md`** - Comprehensive API reference
- **`FRAMEWORK_OVERVIEW.md`** (this file) - Architecture overview

### Example Code (✓ Working)

- **`SimpleLLPExample.java`** - Complete working example
  - Demonstrates manual method invocation
  - Shows framework usage with `LLPSolver`
  - Includes execution statistics display

## Architecture

### Execution Flow

```
User Code
    ↓
LLPSolver (High-level API)
    ↓
LLPEngine (Orchestrator)
    ↓
┌─────────────────────────────────────┐
│  Parallel Execution Loop            │
│                                     │
│  For each iteration:                │
│    1. Advance Phase (parallel)     │
│       ├─ Thread 1: problem.Advance() │
│       ├─ Thread 2: problem.Advance() │
│       └─ Thread N: problem.Advance() │
│                                     │
│    2. Barrier Synchronization      │
│                                     │
│    3. Ensure Phase (parallel)      │
│       ├─ Thread 1: problem.Ensure() │
│       ├─ Thread 2: problem.Ensure() │
│       └─ Thread N: problem.Ensure() │
│                                     │
│    4. Barrier Synchronization      │
│                                     │
│    5. Check Termination            │
│       └─ TerminationDetector       │
└─────────────────────────────────────┘
    ↓
Solution State
```

### Thread Safety Model

- **`LLPState`**: Provides thread-safe access to shared state
- **`LLPBarrier`**: Coordinates thread synchronization
- **Problem Methods**: Should be stateless or properly synchronized
- **State Objects**: Should be immutable or thread-safe

### Configuration Model

```java
LLPConfiguration config = new LLPConfiguration()
    .setNumThreads(8)           // Parallel threads
    .setMaxIterations(10000)    // Iteration limit
    .setLogging(false)          // Disable logging
    .setTimeout(60000);         // 60 second timeout

LLPSolver<MyState> solver = new LLPSolver<>(problem, config);
```

## What You Need to Implement

To use this framework, you need to:

### 1. Define Your State Class

```java
public class MyProblemState {
    // Your state fields
    private int[] data;
    private boolean[] flags;
    
    // Constructor, getters, setters
    // Consider making immutable for thread safety
}
```

### 2. Implement LLPProblem<T>

```java
public class MyProblem implements LLPProblem<MyProblemState> {
    
    @Override
    public boolean Forbidden(MyProblemState state) {
        // TODO: Check if state violates constraints
    }
    
    @Override
    public MyProblemState Ensure(MyProblemState state) {
        // TODO: Fix constraint violations
    }
    
    @Override
    public MyProblemState Advance(MyProblemState state) {
        // TODO: Make progress toward solution
    }
    
    @Override
    public MyProblemState getInitialState() {
        // TODO: Return starting state
    }
    
    @Override
    public boolean isSolution(MyProblemState state) {
        // TODO: Check if state is a solution
    }
}
```

### 3. Use the Framework

```java
MyProblem problem = new MyProblem();
LLPSolver<MyProblemState> solver = new LLPSolver<>(problem);

try {
    MyProblemState solution = solver.solve();
    System.out.println("Solution: " + solution);
} catch (Exception e) {
    e.printStackTrace();
} finally {
    solver.shutdown();
}
```

## Framework Features

### ✅ What's Included

- **Complete parallel execution engine**
- **Thread management and pooling**
- **Synchronization primitives**
- **State management with locking**
- **Termination detection**
- **Configuration management**
- **Execution statistics**
- **Comprehensive documentation**
- **Working example code**

### ❌ What's NOT Included (By Design)

- **Problem-specific implementations** - These are left as TODOs
- **Algorithm logic** - Only the framework structure is provided
- **Working solutions** - All problems throw `UnsupportedOperationException`

This is intentional - the framework provides the infrastructure, but the actual algorithm implementations are exercises for the user.

## Building and Running

### Build
```bash
mvn clean compile
```

### Run Example
```bash
mvn exec:java -Dexec.mainClass="com.llp.examples.SimpleLLPExample"
```

### Test (when implemented)
```bash
mvn test
```

## Key Design Decisions

### 1. Separation of Concerns
- **Framework** handles parallelism, synchronization, termination
- **Problems** only need to implement three methods
- Clean separation allows focus on algorithm logic

### 2. Type Safety
- Generic `LLPProblem<T>` interface
- Type-safe state handling throughout
- Compile-time type checking

### 3. Flexibility
- Configurable thread counts
- Adjustable iteration limits
- Optional logging
- Timeout support

### 4. Simplicity
- High-level `LLPSolver` API for common use
- Low-level `LLPEngine` access for advanced users
- Clear, documented interfaces

### 5. Thread Safety
- Built-in state synchronization
- Barrier-based coordination
- Safe parallel execution by default

## Performance Considerations

### Thread Count
- Default: Number of CPU cores
- Adjust based on problem characteristics
- I/O-bound: More threads
- CPU-bound: Match core count

### State Management
- Consider immutable state objects
- Minimize state copying
- Use efficient data structures

### Synchronization
- Barriers ensure correctness
- May limit parallelism in some cases
- Trade-off between safety and speed

## Next Steps

1. **Choose a problem** from the 6 provided skeletons
2. **Read the problem documentation** in the skeleton file
3. **Implement the three core methods**: Forbidden, Ensure, Advance
4. **Test your implementation** using the framework
5. **Iterate and optimize** based on results

## Getting Help

- **API Reference**: See `LIBRARY_API.md`
- **Example Code**: See `src/main/java/com/llp/examples/SimpleLLPExample.java`
- **Problem Guides**: See individual problem files in `src/main/java/com/llp/problems/`
- **README**: See `README.md` for project overview

## License

This project is for educational purposes as part of a parallel algorithms course assignment.
