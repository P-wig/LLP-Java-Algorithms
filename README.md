# LLP-Java-Algorithms

A simplified Java framework for implementing parallel LLP (Lattice-Linear Predicate) algorithms using Java Streams to solve various computational problems.

## Overview

This project provides a streamlined framework for solving problems using the LLP parallel algorithm paradigm. The LLP algorithm is based on lattice theory and uses three fundamental operations:
- **Forbidden**: Determines if a state violates problem constraints
- **Ensure**: Fixes states to satisfy local constraints
- **Advance**: Makes progress toward the solution

### Framework Features

✅ **Java Streams parallelism** for coordination-free execution  
✅ **Simplified state management** with immutable objects  
✅ **Clean API** through `LLPSolver`  
✅ **Embedded termination detection** with convergence monitoring  
✅ **Educational focus** on algorithm implementation  
✅ **Minimal complexity** for learning LLP concepts

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
│   │               ├── framework/         # Simplified framework
│   │               │   └── LLPEngine.java (streams-based)
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
├── build.sh                               # Build script
├── run_example.sh                         # Run example script
└── test.sh                                # Test script
```

## Simplified Architecture

**✅ Simplified Components:**
- **LLPEngine** - Now uses Java 8+ parallel streams for coordination
- **LLPSolver** - Clean constructors with direct parameters
- **Problem implementations** - Focus purely on algorithm logic

### Core Architecture

```
Your Problem Implementation
    ↓
LLPSolver (simple API)
    ↓
LLPEngine (streams-based execution)
    ↓
Java Parallel Streams (automatic coordination)
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

#### 2. **Ensure(state) → state**
This operation modifies the state to satisfy local constraints and remove forbidden configurations.

**Purpose**:
- Fix states that violate constraints
- Maintain problem invariants
- Ensure forward progress doesn't create permanent violations

**Key**: Always return a **new state object** (immutable pattern)

#### 3. **Advance(state) → state**
This operation moves the state forward toward the solution, potentially creating new forbidden configurations.

**Purpose**:
- Make progress toward the solution
- Explore the solution space
- Move up in the lattice ordering

**Key**: Focus on progress, not constraints (Ensure will fix violations)

## Quick Start

### Basic Usage (Simplified!)

```java
import com.llp.algorithm.LLPProblem;
import com.llp.algorithm.LLPSolver;

// 1. Define your state class (immutable!)
class MyState {
    final int value;  // Your problem data
    
    public MyState(int value) {
        this.value = value;
    }
    
    // Helper for creating new states
    public MyState withValue(int newValue) {
        return new MyState(newValue);
    }
}

// 2. Implement your problem
class MyProblem implements LLPProblem<MyState> {
    @Override
    public boolean Forbidden(MyState state) {
        // Return true if constraints violated
        return state.value < 0;  // Example: negative values forbidden
    }
    
    @Override
    public MyState Ensure(MyState state) {
        // Fix violations
        if (Forbidden(state)) {
            return state.withValue(0);  // Fix by setting to 0
        }
        return state;  // No fix needed
    }
    
    @Override
    public MyState Advance(MyState state) {
        // Make progress
        return state.withValue(state.value + 1);
    }
    
    @Override
    public MyState getInitialState() {
        return new MyState(0);
    }
    
    @Override
    public boolean isSolution(MyState state) {
        return state.value >= 10 && !Forbidden(state);  // Example: reach 10
    }
}

// 3. Solve (much simpler!)
public static void main(String[] args) {
    MyProblem problem = new MyProblem();
    
    // Simple constructors - no configuration objects!
    LLPSolver<MyState> solver = new LLPSolver<>(problem);           // Defaults
    // OR: LLPSolver<MyState> solver = new LLPSolver<>(problem, 4);     // 4 threads
    // OR: LLPSolver<MyState> solver = new LLPSolver<>(problem, 4, 100); // 4 threads, 100 max iterations
    
    try {
        MyState solution = solver.solve();
        System.out.println("Solution: " + solution.value);
        
        // Simple statistics
        System.out.println("Threads used: " + solver.getNumThreads());
        System.out.println("Max iterations: " + solver.getMaxIterations());
        
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        solver.shutdown();
    }
}
```

## Key Simplifications

### 🎯 **Immutable State Pattern**
```java
// Instead of complex thread-safe state management:
class CounterState {
    final int value;  // Immutable!
    
    public CounterState withValue(int newValue) {
        return new CounterState(newValue);  // Always create new
    }
}
```

### 🔄 **Java Streams Parallelism**
```java
// Framework uses streams internally:
IntStream.range(0, parallelism)
    .parallel()                              // Automatic parallelization
    .mapToObj(i -> problem.Advance(state))   // Your method called in parallel
    .reduce(state, this::mergeStates);       // Automatic coordination
```

### ⚡ **Simple Configuration**
```java
// Old way (removed):
// LLPConfiguration config = new LLPConfiguration().setNumThreads(4).setMaxIterations(100);
// LLPSolver<State> solver = new LLPSolver<>(problem, config);

// New way:
LLPSolver<State> solver = new LLPSolver<>(problem, 4, 100);  // Direct parameters
```

## Example: Simple Counter Problem

See `src/main/java/com/llp/examples/SimpleLLPExample.java` for a complete working example that demonstrates:
- How to define an immutable state class
- How to implement the three core methods
- How to use the simplified LLP framework

Run the example:
```bash
./run_example.sh
```

## Problems to Implement

This assignment requires implementing the following problems using the LLP framework:

1. **Stable Marriage Problem** (`StableMarriageProblem.java`)
2. **Parallel Prefix Problem** (`ParallelPrefixProblem.java`)  
3. **Connected Components** (`ConnectedComponentsProblem.java`)
4. **Bellman-Ford Algorithm** (`BellmanFordProblem.java`)
5. **Johnson's Algorithm** (`JohnsonProblem.java`)
6. **Boruvka's Algorithm** (`BoruvkaProblem.java`)

### Implementation Template

Each problem follows this pattern:

```java
// 1. State class (your data structure)
class YourState {
    final SomeType data;  // Immutable fields
    
    public YourState withData(SomeType newData) {
        return new YourState(newData);  // Immutable pattern
    }
}

// 2. Problem class (your algorithm)
class YourProblem implements LLPProblem<YourState> {
    public boolean Forbidden(YourState state) { /* constraint check */ }
    public YourState Ensure(YourState state) { /* fix violations */ }
    public YourState Advance(YourState state) { /* make progress */ }
    public YourState getInitialState() { /* starting point */ }
    public boolean isSolution(YourState state) { /* done check */ }
}

// 3. Main method (solve it)
public static void main(String[] args) {
    YourProblem problem = new YourProblem(/* parameters */);
    LLPSolver<YourState> solver = new LLPSolver<>(problem);
    YourState solution = solver.solve();
    solver.shutdown();
}
```

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build the Project
```bash
./build.sh
```

### Run Example
```bash
./run_example.sh
```

### Run Tests
```bash
./test.sh
```

## Framework Benefits

### 🚀 **Automatic Parallelization**
- Java Streams handle thread management
- ForkJoinPool provides work-stealing
- No manual barrier synchronization needed

### 📚 **Educational Focus**  
- Simple, understandable code
- Focus on algorithm logic, not framework complexity
- Clear separation of concerns

### 🔧 **Easy to Use**
- Minimal configuration
- Direct constructor parameters
- No complex builder patterns

## Execution Flow (Simplified)

```
Initialize State
     ↓
┌─────────────────────────┐
│ For each iteration      │←──────┐
└─────────────────────────┘       │
     ↓                             │
┌─────────────────────────┐       │
│ Parallel Advance        │       │
│ (Java Streams)          │       │
└─────────────────────────┘       │
     ↓                             │
┌─────────────────────────┐       │
│ Parallel Ensure         │       │
│ (Java Streams)          │       │
└─────────────────────────┘       │
     ↓                             │
┌─────────────────────────┐       │
│ Check Convergence       │       │
└─────────────────────────┘       │
     ↓                             │
   Continue ──────────────────────┘
     ↓
   Done
     ↓
  Return Solution
```

## License

This project is for educational purposes as part of a parallel algorithms course assignment.