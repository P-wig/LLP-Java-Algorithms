package com.llp.framework;

import com.llp.problems.BoruvkaProblem.Edge;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class for reading graph files and converting them for use with LLP algorithms.
 * Supports multiple graph file formats including .egr (ECL graph) files.
 */
public class GraphFileReader {
    
    /**
     * Result of reading a graph file.
     */
    public static class GraphData {
        public final int numVertices;
        public final Edge[] edges;
        public final String filename;
        public final long fileSize;
        
        public GraphData(int numVertices, Edge[] edges, String filename, long fileSize) {
            this.numVertices = numVertices;
            this.edges = edges;
            this.filename = filename;
            this.fileSize = fileSize;
        }
        
        @Override
        public String toString() {
            return String.format("Graph{vertices=%d, edges=%d, file=%s, size=%.1fMB}", 
                numVertices, edges.length, filename, fileSize / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Read an ECL graph file (.egr format) with improved binary handling.
     * Tries different endianness and validates the header values.
     */
    public static GraphData readEclGraphFile(String filename) throws IOException {
        System.out.println("Reading ECL graph file: " + filename);
        
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException("Graph file not found: " + filename);
        }
        
        // Try little-endian first (most common on x86), then big-endian
        ByteOrder[] endianness = {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN};
        
        for (ByteOrder order : endianness) {
            try {
                System.out.println("Trying " + order + " byte order...");
                GraphData result = readEclGraphWithEndianness(filename, order);
                if (result != null) {
                    System.out.println("Successfully read with " + order + " byte order");
                    return result;
                }
            } catch (Exception e) {
                System.err.println("Failed with " + order + ": " + e.getMessage());
            }
        }
        
        throw new IOException("Could not read ECL graph file with any supported format");
    }
    
    /**
     * Read ECL graph with specific byte order.
     */
    private static GraphData readEclGraphWithEndianness(String filename, ByteOrder order) throws IOException {
        File file = new File(filename);
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {
            
            // Read and validate header (8 bytes: nodes + edges)
            ByteBuffer headerBuffer = ByteBuffer.allocate(8);
            headerBuffer.order(order);
            
            if (channel.read(headerBuffer) != 8) {
                throw new IOException("Could not read header");
            }
            
            headerBuffer.flip();
            int nodes = headerBuffer.getInt();
            int edges = headerBuffer.getInt();
            
            System.out.printf("Header with %s: %,d nodes, %,d edges\n", order, nodes, edges);
            
            // Sanity check the values
            if (nodes < 1 || nodes > 100_000_000 || edges < 0 || edges > 500_000_000) {
                throw new IOException(String.format("Unrealistic values: nodes=%d, edges=%d", nodes, edges));
            }
            
            // Calculate expected file size
            long expectedSize = 8 + (long)(nodes + 1) * 4 + (long)edges * 4 + (long)edges * 4; // header + nindex + nlist + eweight
            long minExpectedSize = 8 + (long)(nodes + 1) * 4 + (long)edges * 4; // without weights
            
            if (file.length() < minExpectedSize) {
                throw new IOException(String.format("File too small: %d bytes, expected at least %d", 
                    file.length(), minExpectedSize));
            }
            
            // Read neighbor index array
            System.out.println("Reading neighbor index array...");
            ByteBuffer nindexBuffer = ByteBuffer.allocate((nodes + 1) * 4);
            nindexBuffer.order(order);
            
            if (channel.read(nindexBuffer) != (nodes + 1) * 4) {
                throw new IOException("Could not read neighbor index array");
            }
            
            nindexBuffer.flip();
            int[] nindex = new int[nodes + 1];
            for (int i = 0; i <= nodes; i++) {
                nindex[i] = nindexBuffer.getInt();
            }
            
            // Validate nindex array
            if (nindex[0] != 0 || nindex[nodes] != edges) {
                throw new IOException(String.format("Invalid nindex: start=%d (should be 0), end=%d (should be %d)", 
                    nindex[0], nindex[nodes], edges));
            }
            
            // Read neighbor list
            System.out.println("Reading neighbor list...");
            ByteBuffer nlistBuffer = ByteBuffer.allocate(edges * 4);
            nlistBuffer.order(order);
            
            if (channel.read(nlistBuffer) != edges * 4) {
                throw new IOException("Could not read neighbor list");
            }
            
            nlistBuffer.flip();
            int[] nlist = new int[edges];
            for (int i = 0; i < edges; i++) {
                nlist[i] = nlistBuffer.getInt();
                
                // Validate neighbor indices
                if (nlist[i] < 0 || nlist[i] >= nodes) {
                    throw new IOException(String.format("Invalid neighbor index: %d (should be 0-%d)", 
                        nlist[i], nodes - 1));
                }
            }
            
            // Try to read edge weights
            int[] eweight = null;
            if (channel.position() < file.length()) {
                System.out.println("Reading edge weights...");
                ByteBuffer eweightBuffer = ByteBuffer.allocate(edges * 4);
                eweightBuffer.order(order);
                
                int bytesRead = channel.read(eweightBuffer);
                if (bytesRead == edges * 4) {
                    eweightBuffer.flip();
                    eweight = new int[edges];
                    for (int i = 0; i < edges; i++) {
                        eweight[i] = eweightBuffer.getInt();
                    }
                    System.out.println("Successfully read edge weights");
                } else {
                    System.out.println("Partial or no edge weights found, using unit weights");
                }
            } else {
                System.out.println("No edge weights found, using unit weights");
            }
            
            // Convert to edge list with adaptive progress reporting
            System.out.println("Converting ECL format to edge list...");
            List<Edge> edgeList = new ArrayList<>();
            Set<String> seenEdges = new HashSet<>(); // Track edges we've already added

            // Process all vertices in the file
            int processedVertices = nodes;
            System.out.printf("Processing all %,d vertices from the graph file\n", processedVertices);

            // Determine appropriate progress interval based on graph size
            int progressInterval = getProgressInterval(edges);

            for (int src = 0; src < processedVertices; src++) {
                int start = nindex[src];
                int end = nindex[src + 1];
                
                for (int i = start; i < end; i++) {
                    if (i >= edges) break;
                    
                    int dst = nlist[i];
                    
                    // For undirected graphs, only add each edge once
                    // Create a canonical edge representation (smaller vertex first)
                    int u = Math.min(src, dst);
                    int v = Math.max(src, dst);
                    String edgeKey = u + "," + v;
                    
                    if (!seenEdges.contains(edgeKey)) {
                        double weight = (eweight != null) ? eweight[i] : 1.0;
                        edgeList.add(new Edge(u, v, weight));
                        seenEdges.add(edgeKey);
                        
                        // Adaptive progress reporting
                        if (edgeList.size() % progressInterval == 0) {
                            System.out.printf("  Converted %,d edges...\n", edgeList.size());
                        }
                    }
                }
            }
            
            Edge[] edgeArray = edgeList.toArray(new Edge[0]);
            
            System.out.printf("Successfully loaded ECL graph: %,d vertices, %,d edges\n", 
                processedVertices, edgeArray.length);
            
            return new GraphData(processedVertices, edgeArray, file.getName(), file.length());
        }
    }
    
    /**
     * Read a DIMACS format text file with improved header detection.
     */
    public static GraphData readDimacsFile(String filename) throws IOException {
        System.out.println("Reading DIMACS text file: " + filename);
        
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException("Graph file not found: " + filename);
        }
        
        List<Edge> edgeList = new ArrayList<>();
        int nodes = 0, expectedEdges = 0;
        int lineCount = 0;
        boolean headerFound = false;
        
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            String line;
            
            // First pass: look for header in first 1000 lines
            reader.mark(100000); // Mark position for reset
            
            for (int i = 0; i < 1000 && (line = reader.readLine()) != null; i++) {
                line = line.trim();
                
                if (line.startsWith("p ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            nodes = Integer.parseInt(parts[2]);
                            expectedEdges = Integer.parseInt(parts[3]);
                            System.out.printf("Found DIMACS header at line %d: %,d nodes, %,d edges\n", 
                                i + 1, nodes, expectedEdges);
                            headerFound = true;
                            break;
                        } catch (NumberFormatException e) {
                            // Continue searching
                        }
                    }
                }
            }
            
            if (!headerFound) {
                System.out.println("No DIMACS header found, assuming text edge list format...");
                // Try to read as simple edge list without header
                return readSimpleEdgeList(filename);
            }
            
            // Reset to beginning and parse edges
            reader.reset();
            lineCount = 0;
            
            // Determine progress interval based on expected edges
            int progressInterval = getProgressInterval(expectedEdges);
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();
                
                // Skip empty lines, comments, and header
                if (line.isEmpty() || line.startsWith("c") || line.startsWith("p ")) {
                    continue;
                }
                
                // Parse edge line: "a u v weight"
                if (line.startsWith("a ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            int u = Integer.parseInt(parts[1]) - 1; // Convert to 0-based
                            int v = Integer.parseInt(parts[2]) - 1; // Convert to 0-based
                            double weight = Double.parseDouble(parts[3]);
                            
                            edgeList.add(new Edge(u, v, weight));
                            
                            if (edgeList.size() % progressInterval == 0) {
                                System.out.printf("  Read %,d edges...\n", edgeList.size());
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Invalid edge format at line " + lineCount + ": " + line);
                        }
                    }
                }
            }
        }
        
        Edge[] edges = edgeList.toArray(new Edge[0]);
        
        System.out.printf("DIMACS read complete: %,d vertices, %,d edges (expected %,d)\n", 
            nodes, edges.length, expectedEdges);
        
        return new GraphData(nodes, edges, file.getName(), file.length());
    }
    
    /**
     * Read a simple edge list format (no header).
     */
    private static GraphData readSimpleEdgeList(String filename) throws IOException {
        System.out.println("Reading simple edge list format...");
        
        List<Edge> edgeList = new ArrayList<>();
        Set<Integer> vertexSet = new HashSet<>();
        int lineCount = 0;
        
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null && lineCount < 1000000) { // Limit for safety
                lineCount++;
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("c")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int u = Integer.parseInt(parts[0]);
                        int v = Integer.parseInt(parts[1]);
                        double weight = parts.length > 2 ? Double.parseDouble(parts[2]) : 1.0;
                        
                        edgeList.add(new Edge(u, v, weight));
                        vertexSet.add(u);
                        vertexSet.add(v);
                        
                        if (edgeList.size() % 100000 == 0) {
                            System.out.printf("  Read %,d edges...\n", edgeList.size());
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                    }
                }
            }
        }
        
        int numVertices = vertexSet.isEmpty() ? 0 : vertexSet.stream().mapToInt(Integer::intValue).max().getAsInt() + 1;
        Edge[] edges = edgeList.toArray(new Edge[0]);
        
        System.out.printf("Simple edge list read: %,d vertices, %,d edges\n", numVertices, edges.length);
        
        return new GraphData(numVertices, edges, new File(filename).getName(), new File(filename).length());
    }
    
    /**
     * Read an .egr file with automatic format detection between ECL binary and DIMACS text.
     */
    public static GraphData readEgrFile(String filename) throws IOException {
        // First try ECL binary format
        try {
            return readEclGraphFile(filename);
        } catch (Exception e) {
            System.err.println("ECL binary read failed: " + e.getMessage());
            System.out.println("Trying DIMACS text format...");
            
            // Fall back to DIMACS text format
            return readDimacsFile(filename);
        }
    }
    
    /**
     * Read a graph file with automatic format detection and optional vertex limit.
     */
    public static GraphData readGraphFile(String filename, int maxVertices) throws IOException {
        String extension = getFileExtension(filename).toLowerCase();
        
        GraphData fullGraph;
        switch (extension) {
            case "egr":
                fullGraph = readEgrFile(filename);
                break;
            case "gr":
            case "dimacs":
                fullGraph = readDimacsFile(filename);
                break;
            case "txt":
            case "graph":
                fullGraph = readDimacsFile(filename);
                break;
            default:
                System.out.println("Unknown file extension '" + extension + "', trying ECL format...");
                fullGraph = readEgrFile(filename);
                break;
        }
        
        // Apply vertex limit if specified and necessary
        if (maxVertices > 0 && fullGraph.numVertices > maxVertices) {
            System.out.printf("Applying vertex limit: reducing from %,d to %,d vertices\n", 
                fullGraph.numVertices, maxVertices);
            return createSample(fullGraph, maxVertices);
        }
        
        return fullGraph;
    }
    
    /**
     * Read a graph file with automatic format detection (no vertex limit).
     */
    public static GraphData readGraphFile(String filename) throws IOException {
        return readGraphFile(filename, -1); // -1 means no limit
    }
    
    /**
     * Create a sample subset of a large graph for testing.
     */
    public static GraphData createSample(GraphData graphData, int maxVertices) {
        if (graphData.numVertices <= maxVertices) {
            return graphData;
        }
        
        System.out.printf("Creating sample graph: %,d vertices from original %,d\n", 
            maxVertices, graphData.numVertices);
        
        List<Edge> sampleEdges = new ArrayList<>();
        
        for (Edge edge : graphData.edges) {
            if (edge.u < maxVertices && edge.v < maxVertices) {
                sampleEdges.add(edge);
            }
        }
        
        Edge[] edges = sampleEdges.toArray(new Edge[0]);
        
        System.out.printf("Sample graph: %,d vertices, %,d edges (%.2f%% of original)\n",
            maxVertices, edges.length, 
            graphData.edges.length > 0 ? 100.0 * edges.length / graphData.edges.length : 0.0);
        
        return new GraphData(maxVertices, edges, "sample_" + graphData.filename, 0);
    }
    
    /**
     * Print statistics about the graph.
     */
    public static void printGraphStats(GraphData graphData) {
        System.out.println("\n=== Graph Statistics ===");
        System.out.println("File: " + graphData.filename);
        System.out.printf("Vertices: %,d\n", graphData.numVertices);
        System.out.printf("Edges: %,d\n", graphData.edges.length);
        System.out.printf("File size: %.1f MB\n", graphData.fileSize / (1024.0 * 1024.0));
        
        if (graphData.numVertices > 1) {
            double maxPossibleEdges = (long) graphData.numVertices * (graphData.numVertices - 1) / 2.0;
            System.out.printf("Density: %.8f\n", graphData.edges.length / maxPossibleEdges);
        } else {
            System.out.println("Density: N/A (insufficient vertices)");
        }
        
        // Weight statistics
        if (graphData.edges.length > 0) {
            double minWeight = Arrays.stream(graphData.edges).mapToDouble(e -> e.weight).min().getAsDouble();
            double maxWeight = Arrays.stream(graphData.edges).mapToDouble(e -> e.weight).max().getAsDouble();
            double avgWeight = Arrays.stream(graphData.edges).mapToDouble(e -> e.weight).average().getAsDouble();
            
            System.out.printf("Edge weights: min=%.2f, max=%.2f, avg=%.2f\n", 
                minWeight, maxWeight, avgWeight);
        }
        
        System.out.println("========================\n");
    }
    
    /**
     * Get file extension from filename.
     */
    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
    
    /**
     * Determine appropriate progress reporting interval based on number of edges.
     */
    private static int getProgressInterval(int edgeCount) {
        if (edgeCount < 100_000) {
            return 10_000;        // Report every 10K edges for small graphs
        } else if (edgeCount < 1_000_000) {
            return 50_000;        // Report every 50K edges for medium graphs (like NY)
        } else if (edgeCount < 10_000_000) {
            return 500_000;       // Report every 500K edges for large graphs
        } else {
            return 1_000_000;     // Report every 1M edges for very large graphs
        }
    }
}
