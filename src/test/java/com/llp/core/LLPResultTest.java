package com.llp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LLPResultTest {
    
    @Test
    void testResultCreation() {
        Integer solution = 42;
        double score = 10.5;
        int iterations = 50;
        int threadId = 1;
        
        LLPResult<Integer> result = new LLPResult<>(solution, score, iterations, threadId);
        
        assertEquals(solution, result.getSolution());
        assertEquals(score, result.getScore(), 0.001);
        assertEquals(iterations, result.getIterations());
        assertEquals(threadId, result.getThreadId());
    }
    
    @Test
    void testResultToString() {
        LLPResult<Integer> result = new LLPResult<>(100, 5.5, 25, 2);
        String resultString = result.toString();
        
        assertTrue(resultString.contains("5.5"));
        assertTrue(resultString.contains("25"));
        assertTrue(resultString.contains("2"));
    }
}
