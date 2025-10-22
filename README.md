# LLP-Java-Algorithms

A Java library implementing a parallel LLP (Las Vegas + Learning) algorithm framework that can be applied to various optimization and search problems.

## Project Structure

```
LLP-Java-Algorithms/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── llp/
│   │               ├── core/           # Core LLP algorithm components
│   │               │   ├── Problem.java        # Interface for problem definition
│   │               │   ├── LLPAlgorithm.java   # Main algorithm implementation
│   │               │   └── LLPResult.java      # Result container
│   │               ├── problems/       # Problem implementations
│   │               │   └── ExampleProblem.java # Example problem
│   │               └── Main.java       # Main entry point
│   └── test/
│       └── java/
│           └── com/
│               └── llp/
│                   ├── core/           # Tests for core components
│                   └── problems/       # Tests for problems
├── pom.xml                             # Maven configuration
├── run.sh                              # Build and run script
└── README.md                           # This file
```

## Features

- **Parallel Execution**: Utilizes multiple threads for concurrent problem-solving
- **Flexible API**: Easy-to-implement `Problem<T>` interface for different problem types
- **Type-Safe**: Generic type support for various problem state representations
- **Modular Design**: Clean separation between algorithm engine and problem definitions

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building the Project

### Using Maven directly:
```bash
mvn clean compile
```

### Running tests:
```bash
mvn test
```

### Building the executable JAR:
```bash
mvn clean package
```

## Running the Program

### Using the provided script (recommended):
```bash
./run.sh
```

### Using Maven:
```bash
mvn exec:java -Dexec.mainClass="com.llp.Main"
```

### Using the JAR directly:
```bash
java -jar target/llp-algorithms-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## How to Use the Library

### 1. Implement the Problem Interface

Create a class that implements `Problem<T>` where `T` is your state representation:

```java
public class MyProblem implements Problem<MyStateType> {
    
    @Override
    public MyStateType getInitialState() {
        // Return the starting state
    }
    
    @Override
    public boolean isGoal(MyStateType state) {
        // Check if this state is a solution
    }
    
    @Override
    public MyStateType[] getSuccessors(MyStateType state) {
        // Generate possible next states
    }
    
    @Override
    public double evaluate(MyStateType state) {
        // Return fitness/cost (lower is better)
    }
    
    @Override
    public String formatSolution(MyStateType state) {
        // Format the solution for display
    }
}
```

### 2. Create and Configure the Algorithm

```java
Problem<MyStateType> problem = new MyProblem();
LLPAlgorithm<MyStateType> algorithm = new LLPAlgorithm<>(
    problem,
    4,      // Number of parallel threads
    1000    // Max iterations per thread
);
```

### 3. Solve and Get Results

```java
try {
    LLPResult<MyStateType> result = algorithm.solve();
    System.out.println("Solution: " + result.getSolution());
    System.out.println("Score: " + result.getScore());
    System.out.println("Iterations: " + result.getIterations());
} catch (Exception e) {
    e.printStackTrace();
} finally {
    algorithm.shutdown();
}
```

## Adding New Problems

1. Create a new class in `src/main/java/com/llp/problems/`
2. Implement the `Problem<T>` interface
3. Define your state representation type `T`
4. Implement all required methods
5. Optionally add tests in `src/test/java/com/llp/problems/`

## Example Problems to Implement

The LLP algorithm can be applied to various problems including:

- **Traveling Salesman Problem (TSP)**
- **Graph Coloring**
- **Knapsack Problem**
- **Scheduling Problems**
- **Constraint Satisfaction Problems**
- **Path Finding**
- **And many more...**

## API Documentation

### Core Components

#### `Problem<T>` Interface
The main interface that defines how to represent and solve a problem.

#### `LLPAlgorithm<T>` Class
The parallel algorithm implementation that solves problems implementing the Problem interface.

#### `LLPResult<T>` Class
Container for algorithm results including solution, score, iterations, and thread information.

## Contributing

When implementing new problems:
1. Follow the existing code style
2. Add appropriate tests
3. Update documentation as needed
4. Ensure thread-safety if sharing state

## License

This project is available for educational purposes.