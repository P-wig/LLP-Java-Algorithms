package com.llp.problems;

import com.llp.core.Problem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleProblemTest {
    
    @Test
    void testInitialState() {
        Problem<Integer> problem = new ExampleProblem(100, 50);
        assertEquals(50, problem.getInitialState());
    }
    
    @Test
    void testIsGoal() {
        Problem<Integer> problem = new ExampleProblem(100, 50);
        assertFalse(problem.isGoal(50));
        assertTrue(problem.isGoal(100));
    }
    
    @Test
    void testEvaluate() {
        Problem<Integer> problem = new ExampleProblem(100, 50);
        assertEquals(50.0, problem.evaluate(50), 0.001);
        assertEquals(0.0, problem.evaluate(100), 0.001);
        assertEquals(10.0, problem.evaluate(110), 0.001);
    }
    
    @Test
    void testGetSuccessors() {
        Problem<Integer> problem = new ExampleProblem(100, 50);
        Integer[] successors = problem.getSuccessors(50);
        
        assertNotNull(successors);
        assertEquals(6, successors.length);
        assertTrue(contains(successors, 51));
        assertTrue(contains(successors, 49));
    }
    
    private boolean contains(Integer[] array, int value) {
        for (Integer item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }
}
