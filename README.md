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
│   │               ├── problems/          # Problem implementations
│   │               │   ├── StableMarriageProblem.java (TODO)
│   │               │   ├── ParallelPrefixProblem.java (TODO)
│   │               │   ├── ConnectedComponentsProblem.java ✅
│   │               │   ├── BellmanFordProblem.java ✅
│   │               │   ├── JohnsonProblem.java (TODO)
│   │               │   └── BoruvkaProblem.java ✅
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

## Implemented Problems

### ✅ **Boruvka's Minimum Spanning Tree Algorithm** (`BoruvkaProblem.java`)
Complete implementation of Boruvka's MST algorithm using the LLP framework.

**Features:**
- Recursive and LLP parallel implementations
- Union-Find data structure with path compression
- Symmetry breaking for cycle prevention
- Graph reduction for component abstraction
- Performance testing with multiple thread counts

**Key Concepts Demonstrated:**
- **Forbidden**: Detects when `G[j] ≠ G[G[j]]` (Union-Find compression violations)
- **Ensure**: Applies path compression to fix parent array inconsistencies
- **Advance**: Performs edge selection, parent assignment, and graph reduction
- **Merge**: Combines MST edges from parallel threads

### ✅ **Connected Components Problem** (`ConnectedComponentsProblem.java`)
Parallel algorithm for finding connected components in an undirected graph.

**Features:**
- Component label propagation
- Round-robin thread distribution
- Efficient component merging
- Multiple graph topologies support

**Key Concepts Demonstrated:**
- **Forbidden**: Detects edges connecting vertices with different component labels
- **Ensure**: Merges components by assigning consistent labels
- **Advance**: Propagates minimum labels along edges
- **Merge**: Combines component assignments from parallel threads

### ✅ **Bellman-Ford Single-Source Shortest Path** (`BellmanFordProblem.java`)
Shortest path algorithm that handles negative edge weights.

**Features:**
- Parallel edge relaxation
- Triangle inequality constraint enforcement
- Negative cycle detection capability
- Distance estimation convergence

**Key Concepts Demonstrated:**
- **Forbidden**: Detects triangle inequality violations
- **Ensure**: Fixes distance estimate violations
- **Advance**: Relaxes edges to improve distance estimates
- **Merge**: Combines shortest distance estimates

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

### The LLP Framework Pattern

The LLP framework separates two critical concerns that are essential for correct parallel algorithm execution:

#### **`Forbidden` vs `isSolution`**
- **`Forbidden`**: Detects **data structure integrity violations** (e.g., Union-Find compression issues)
- **`isSolution`**: Detects **algorithm completion** (e.g., MST fully constructed)

This separation ensures that:
1. **Data structures remain consistent** during parallel execution
2. **Algorithm termination** is detected correctly
3. **Parallel threads** can work safely without corruption

### The Three Core Methods

When implementing a problem using the LLP framework, you need to define these three methods:

#### 1. **Forbidden(state) → boolean**
This predicate determines if a given configuration is invalid or violates problem constraints.

**Purpose**: 
- Detect states that violate problem invariants
- Identify configurations that need correction

**Example use cases**:
- In Boruvka MST: Check if `G[j] ≠ G[G[j]]` (Union-Find compression violations)
- In Bellman-Ford: Check if distances violate triangle inequality
- In Connected Components: Check if component labels are inconsistent

#### 2. **Ensure(state, threadId, totalThreads) → state**
This operation modifies the state to satisfy local constraints and remove forbidden configurations.

**Purpose**:
- Fix states that violate constraints
- Maintain problem invariants
- Ensure forward progress doesn't create permanent violations

**Key Features**:
- **Thread Distribution**: Uses `threadId` and `totalThreads` for parallel work distribution
- **Immutable Pattern**: Always return a **new state object**
- **Round-Robin Distribution**: `for (int i = threadId; i < work.length; i += totalThreads)`

#### 3. **Advance(state, threadId, totalThreads) → state**
This operation moves the state forward toward the solution, potentially creating new forbidden configurations.

**Purpose**:
- Make progress toward the solution
- Explore the solution space
- Move up in the lattice ordering

**Key Features**:
- **Parallel Execution**: Each thread processes different parts of the problem
- **Progress Focus**: Focus on advancement, not constraint satisfaction
- **Thread Safety**: Uses immutable state pattern for safe parallel execution

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
    public MyState Ensure(MyState state, int threadId, int totalThreads) {
        // Fix violations for this thread's partition
        if (Forbidden(state)) {
            return state.withValue(0);  // Fix by setting to 0
        }
        return state;  // No fix needed
    }
    
    @Override
    public MyState Advance(MyState state, int threadId, int totalThreads) {
        // Make progress for this thread's partition
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
        LLPSolver.ExecutionStats stats = solver.getExecutionStats();
        System.out.println("Iterations: " + stats.getIterationCount());
        System.out.println("Converged: " + stats.hasConverged());
        
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
// Thread-safe state management:
class GraphState {
    final int[] labels;  // Immutable!
    final Edge[] edges;  // Immutable!
    
    public GraphState withLabels(int[] newLabels) {
        return new GraphState(edges, newLabels);  // Always create new
    }
}
```

### 🔄 **Parallel Thread Distribution**
```java
// Round-robin work distribution pattern:
for (int i = threadId; i < workItems.length; i += totalThreads) {
    // Thread 0 gets: 0, 3, 6, 9...
    // Thread 1 gets: 1, 4, 7, 10...
    // Thread 2 gets: 2, 5, 8, 11...
    processWorkItem(workItems[i]);
}
```

### ⚡ **Simple Configuration**
```java
// Direct parameter constructors:
LLPSolver<State> solver = new LLPSolver<>(problem, 4, 100);  // 4 threads, 100 max iterations
```

## Running Implemented Problems

### Boruvka's Algorithm
```bash
# Compile and run
javac -cp . src/main/java/com/llp/problems/BoruvkaProblem.java
java -cp . com.llp.problems.BoruvkaProblem
```

### Connected Components
```bash
# Compile and run
javac -cp . src/main/java/com/llp/problems/ConnectedComponentsProblem.java
java -cp . com.llp.problems.ConnectedComponentsProblem
```

### Bellman-Ford Algorithm
```bash
# Compile and run
javac -cp . src/main/java/com/llp/problems/BellmanFordProblem.java
java -cp . com.llp.problems.BellmanFordProblem
```

## Remaining Problems to Implement

1. **Stable Marriage Problem** (`StableMarriageProblem.java`)
2. **Parallel Prefix Problem** (`ParallelPrefixProblem.java`)  
3. **Johnson's Algorithm** (`JohnsonProblem.java`)

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
    public YourState Ensure(YourState state, int threadId, int totalThreads) { /* fix violations */ }
    public YourState Advance(YourState state, int threadId, int totalThreads) { /* make progress */ }
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

### 🧪 **Production-Ready Examples**
- Complete working implementations
- Performance testing and analysis
- Educational debugging output

## Execution Flow

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
│ Each thread: threadId   │       │
└─────────────────────────┘       │
     ↓                             │
┌─────────────────────────┐       │
│ Merge thread results    │       │
└─────────────────────────┘       │
     ↓                             │
┌─────────────────────────┐       │
│ Parallel Ensure         │       │
│ (Java Streams)          │       │
│ Fix violations          │       │
└─────────────────────────┘       │
     ↓                             │
┌─────────────────────────┐       │
│ Check Convergence       │       │
│ isSolution(state)       │       │
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