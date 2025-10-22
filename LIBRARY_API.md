# LLP Library Framework API Documentation

## Overview

The LLP (Least Lattice Predicate) Library Framework provides a robust foundation for implementing parallel algorithms using the LLP paradigm. This document describes the core framework components and how to use them.

## Core Framework Components

### 1. LLPProblem<T> Interface

The fundamental interface that all LLP problems must implement.

```java
public interface LLPProblem<T> {
    boolean Forbidden(T state);
    T Ensure(T state);
    T Advance(T state);
    T getInitialState();
    boolean isSolution(T state);
}
```

**Methods:**

- **`Forbidden(state)`**: Returns `true` if the state violates problem constraints
- **`Ensure(state)`**: Fixes constraint violations and returns corrected state
- **`Advance(state)`**: Makes progress toward solution, returns advanced state
- **`getInitialState()`**: Returns the starting state for the algorithm
- **`isSolution(state)`**: Returns `true` if state is a valid solution

### 2. LLPSolver<T>

High-level API for executing LLP algorithms.

```java
// Create with default configuration
LLPSolver<MyState> solver = new LLPSolver<>(problem);

// Create with specific thread count
LLPSolver<MyState> solver = new LLPSolver<>(problem, 4);

// Create with custom configuration
LLPConfiguration config = new LLPConfiguration()
    .setNumThreads(4)
    .setMaxIterations(1000)
    .setLogging(true);
LLPSolver<MyState> solver = new LLPSolver<>(problem, config);

// Solve the problem
MyState solution = solver.solve();

// Get execution statistics
LLPTerminationDetector detector = solver.getTerminationDetector();
int iterations = detector.getIterationCount();

// Clean up resources
solver.shutdown();
```

**Key Methods:**

- **`solve()`**: Executes the parallel LLP algorithm and returns the solution
- **`getTerminationDetector()`**: Returns detector with execution statistics
- **`getConfiguration()`**: Returns the configuration used
- **`shutdown()`**: Releases all resources (must be called when done)

### 3. LLPEngine<T>

Core execution engine for parallel LLP algorithms. Typically used internally by LLPSolver, but can be used directly for advanced use cases.

```java
LLPEngine<MyState> engine = new LLPEngine<>(problem, numThreads, maxIterations);
MyState solution = engine.execute(initialState);
engine.shutdown();
```

**Key Features:**

- Orchestrates parallel execution of Advance and Ensure phases
- Manages thread synchronization using barriers
- Monitors termination conditions
- Handles thread pool lifecycle

### 4. LLPState<T>

Thread-safe container for problem state with synchronization support.

```java
LLPState<MyState> state = new LLPState<>(initialState);

// Thread-safe operations
MyState current = state.get();
state.set(newState);

// Manual locking for advanced use cases
state.acquireReadLock();
try {
    MyState s = state.get();
    // ... work with state ...
} finally {
    state.releaseReadLock();
}

// Version tracking for change detection
long version = state.getVersion();
```

**Key Methods:**

- **`get()`**: Thread-safe read of current state
- **`set(newState)`**: Thread-safe update of state
- **`getVersion()`**: Returns version number (increments on each update)
- **`snapshot()`**: Creates a snapshot of current state
- **`acquireReadLock()` / `releaseReadLock()`**: Manual read lock control
- **`acquireWriteLock()` / `releaseWriteLock()`**: Manual write lock control

### 5. LLPConfiguration

Fluent configuration builder for LLP execution parameters.

```java
LLPConfiguration config = new LLPConfiguration()
    .setNumThreads(8)              // Number of parallel threads
    .setMaxIterations(5000)        // Maximum iterations before timeout
    .setLogging(true)              // Enable execution logging
    .setTimeout(60000);            // Timeout in milliseconds
```

**Configuration Options:**

- **`numThreads`**: Number of parallel execution threads (default: CPU cores)
- **`maxIterations`**: Maximum iterations before forced termination (default: unlimited)
- **`enableLogging`**: Enable/disable execution logging (default: false)
- **`timeoutMillis`**: Maximum execution time in milliseconds (default: unlimited)

### 6. LLPBarrier

Synchronization barrier for coordinating parallel threads.

```java
LLPBarrier barrier = new LLPBarrier(numThreads);

// In each thread
barrier.await();  // Wait for all threads to reach this point

// With timeout
barrier.await(5, TimeUnit.SECONDS);

// Query barrier state
int waiting = barrier.getNumberWaiting();
boolean broken = barrier.isBroken();
```

**Key Methods:**

- **`await()`**: Block until all threads reach the barrier
- **`await(timeout, unit)`**: Wait with timeout
- **`reset()`**: Reset barrier to initial state
- **`getNumberWaiting()`**: Get count of threads currently waiting
- **`isBroken()`**: Check if barrier is broken

### 7. LLPTerminationDetector

Monitors termination conditions during execution.

```java
LLPTerminationDetector detector = new LLPTerminationDetector(maxIterations);

// Mark convergence
detector.markConverged();

// Check termination conditions
boolean converged = detector.hasConverged();
boolean maxed = detector.maxIterationsReached();
boolean shouldStop = detector.shouldTerminate();

// Get execution statistics
int iterations = detector.getIterationCount();

// Force early termination
detector.forceStop();
```

**Key Methods:**

- **`markConverged()`**: Mark algorithm as converged
- **`hasConverged()`**: Check if converged
- **`incrementIteration()`**: Increment iteration counter
- **`getIterationCount()`**: Get current iteration count
- **`maxIterationsReached()`**: Check if max iterations reached
- **`shouldTerminate()`**: Check any termination condition
- **`forceStop()`**: Request immediate termination
- **`reset()`**: Reset all flags and counters

## Using the Framework

### Step 1: Define Your State Class

Create a class to represent your problem state:

```java
public class MyProblemState {
    // Problem-specific data
    private int[] data;
    private boolean[] flags;
    
    public MyProblemState(int[] data, boolean[] flags) {
        this.data = data;
        this.flags = flags;
    }
    
    // Getters and setters
    public int[] getData() { return data; }
    public boolean[] getFlags() { return flags; }
}
```

### Step 2: Implement LLPProblem Interface

```java
public class MyProblem implements LLPProblem<MyProblemState> {
    
    @Override
    public boolean Forbidden(MyProblemState state) {
        // TODO: Implement constraint checking logic
        // Return true if state violates constraints
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public MyProblemState Ensure(MyProblemState state) {
        // TODO: Implement constraint fixing logic
        // Return state with violations corrected
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public MyProblemState Advance(MyProblemState state) {
        // TODO: Implement progress logic
        // Return state moved toward solution
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public MyProblemState getInitialState() {
        // TODO: Return appropriate initial state
        throw new UnsupportedOperationException("Not implemented");
    }
    
    @Override
    public boolean isSolution(MyProblemState state) {
        // TODO: Check if state is a valid solution
        throw new UnsupportedOperationException("Not implemented");
    }
}
```

### Step 3: Use LLPSolver to Execute

```java
public class Main {
    public static void main(String[] args) {
        try {
            // Create problem instance
            MyProblem problem = new MyProblem();
            
            // Create solver with configuration
            LLPConfiguration config = new LLPConfiguration()
                .setNumThreads(4)
                .setMaxIterations(1000);
            
            LLPSolver<MyProblemState> solver = new LLPSolver<>(problem, config);
            
            // Solve
            MyProblemState solution = solver.solve();
            
            // Get statistics
            LLPTerminationDetector detector = solver.getTerminationDetector();
            System.out.println("Iterations: " + detector.getIterationCount());
            System.out.println("Converged: " + detector.hasConverged());
            
            // Clean up
            solver.shutdown();
            
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
```

## Best Practices

### 1. State Immutability

Consider making state objects immutable for thread safety:

```java
public class MyState {
    private final int[] data;
    
    public MyState(int[] data) {
        this.data = data.clone();  // Defensive copy
    }
    
    public MyState withUpdatedData(int[] newData) {
        return new MyState(newData);  // Return new instance
    }
}
```

### 2. Efficient Forbidden Checks

Make `Forbidden()` efficient as it's called frequently:

```java
@Override
public boolean Forbidden(MyState state) {
    // Use early returns for quick checks
    if (quickCheck(state)) return true;
    
    // Avoid expensive operations if possible
    return expensiveCheck(state);
}
```

### 3. Incremental Ensure Operations

Make `Ensure()` fix violations incrementally:

```java
@Override
public MyState Ensure(MyState state) {
    // Fix one violation at a time
    if (violationType1(state)) {
        return fixViolationType1(state);
    }
    if (violationType2(state)) {
        return fixViolationType2(state);
    }
    return state;
}
```

### 4. Progressive Advance Operations

Make `Advance()` make measurable progress:

```java
@Override
public MyState Advance(MyState state) {
    // Make clear progress toward solution
    // It's OK if this creates forbidden states
    return makeProgress(state);
}
```

### 5. Resource Management

Always clean up resources:

```java
LLPSolver<MyState> solver = null;
try {
    solver = new LLPSolver<>(problem);
    MyState solution = solver.solve();
    // ... use solution ...
} catch (Exception e) {
    e.printStackTrace();
} finally {
    if (solver != null) {
        solver.shutdown();
    }
}
```

## Thread Safety Considerations

1. **State Objects**: Should be thread-safe or immutable
2. **LLPState**: Provides built-in thread safety
3. **Problem Methods**: Should be stateless or use proper synchronization
4. **Shared Data**: Use appropriate synchronization mechanisms

## Performance Tuning

### Thread Count

```java
// Start with CPU core count
int threads = Runtime.getRuntime().availableProcessors();

// Adjust based on problem characteristics
if (ioIntensive) {
    threads *= 2;  // More threads for I/O bound
} else if (cpuIntensive) {
    threads = threads;  // Match CPU count
}

config.setNumThreads(threads);
```

### Iteration Limits

```java
// Set reasonable limits to prevent infinite loops
config.setMaxIterations(10000);
```

### Logging

```java
// Enable for debugging, disable for performance
config.setLogging(false);  // Production
config.setLogging(true);   // Development
```

## Error Handling

The framework throws standard exceptions:

- **`InterruptedException`**: If execution is interrupted
- **`ExecutionException`**: If a thread encounters an error
- **`IllegalArgumentException`**: For invalid configuration

Always handle these appropriately:

```java
try {
    MyState solution = solver.solve();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    // Handle interruption
} catch (ExecutionException e) {
    Throwable cause = e.getCause();
    // Handle execution error
}
```

## Examples

See the `examples/` directory for complete working examples:

- **SimpleLLPExample.java**: Basic counter problem demonstrating framework usage
- Additional examples showing different problem types (to be implemented)

## Advanced Usage

### Custom Execution Phases

For advanced use cases, you can extend LLPEngine:

```java
public class CustomEngine<T> extends LLPEngine<T> {
    // Override methods for custom behavior
}
```

### Direct State Management

For fine-grained control, use LLPState directly:

```java
LLPState<MyState> state = new LLPState<>(initialState);
state.acquireWriteLock();
try {
    MyState current = state.get();
    MyState updated = transform(current);
    state.set(updated);
} finally {
    state.releaseWriteLock();
}
```

## Troubleshooting

### Problem: Algorithm doesn't converge

- Check that `Ensure()` actually fixes violations
- Verify `Advance()` makes meaningful progress
- Increase `maxIterations` or add logging

### Problem: Deadlock or hanging

- Ensure all locks are properly released
- Check barrier synchronization
- Verify thread count matches barrier parties

### Problem: Poor performance

- Tune thread count for your workload
- Profile to identify bottlenecks
- Consider state object overhead
- Optimize `Forbidden()` checks

## Further Reading

- Original LLP algorithm papers
- Java concurrency documentation
- Parallel algorithm design patterns
