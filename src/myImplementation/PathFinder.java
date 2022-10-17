package myImplementation;

import java.util.*;

public class PathFinder {

    /**
     * Attempts to find a path using Breadth First Search algorithm.
     * DISREGARDS teleports as it goes from start coordinate to end coordinate
     *
     * @param start The start vertex
     * @param end   The end coordinate
     */
    public PathFinderResult findPathBfsOnlyStartPos(GraphVertex start, Coordinate end) {
        final long startTime = System.currentTimeMillis();

        Queue<BacktrackableVertex> queue = new LinkedList<>();
        Set<GraphVertex> expandedVertices = new HashSet<>();

        queue.add(new BacktrackableVertex(start, null, "start"));

        while (queue.peek() != null) {

            // Expand next vertex if new
            final BacktrackableVertex currentBacktrackableVertex = queue.remove();
            final GraphVertex currentVertex = currentBacktrackableVertex.vertex;
            if (!expandedVertices.contains(currentVertex)) {
                expandedVertices.add(currentVertex);

                // Goal found?
                if (currentVertex.coordinate.equals(end)) {
                    return new PathFinderResult(true, this.backtrack(currentBacktrackableVertex), System.currentTimeMillis() - startTime);
                }

                // Add neighbours of vertex into queue
                for (GraphEdge neighbor : currentVertex.neighbors) {
                    queue.add(new BacktrackableVertex(neighbor.to(), currentBacktrackableVertex, neighbor.methodOfMovement()));
                }
            }
        }

        return new PathFinderResult(false, null, System.currentTimeMillis() - startTime);
    }

    /**
     * Attempts to find a path using Breadth First Search algorithm.
     * Uses start position and teleports
     *
     * @param start The start vertex
     * @param end   The end coordinate
     */
    public PathFinderResult findPathBfs(Graph graph, Coordinate start, Coordinate end) {
        final long startTime = System.currentTimeMillis();

        Queue<BacktrackableVertex> queue = new LinkedList<>();
        Set<GraphVertex> expandedVertices = new HashSet<>();

        // Add start coordinate and teleports to queue
        queue.add(new BacktrackableVertex(graph.vertices().get(start), null, "start"));
        for(Teleport teleport : graph.teleports()) {
            final GraphVertex teleportVertex = graph.vertices().get(teleport.destination());
            assert(teleportVertex != null);
            queue.add(new BacktrackableVertex(teleportVertex, null, teleport.title()));
        }

        while (queue.peek() != null) {

            // Expand next vertex if new
            final BacktrackableVertex currentBacktrackableVertex = queue.remove();
            final GraphVertex currentVertex = currentBacktrackableVertex.vertex;
            if (!expandedVertices.contains(currentVertex)) {
                expandedVertices.add(currentVertex);

                // Goal found?
                if (currentVertex.coordinate.equals(end)) {
                    return new PathFinderResult(true, this.backtrack(currentBacktrackableVertex), System.currentTimeMillis() - startTime);
                }

                // Add neighbours of vertex into queue
                for (GraphEdge neighbor : currentVertex.neighbors) {
                    queue.add(new BacktrackableVertex(neighbor.to(), currentBacktrackableVertex, neighbor.methodOfMovement()));
                }
            }
        }

        return new PathFinderResult(false, null, System.currentTimeMillis() - startTime);
    }



    /**
     * Attempts to find a path using Breadth First Search Algorithm.
     * Starts at the end goal and keeps looking until it finds either the start coordinate or a teleport
     * WARNING: Doesn't make sense because there's unidirectional transports
     * @param graph The Graph
     * @param start The starting position of the character
     * @param end The destination position
     */
    public PathFinderResult findPathBfsEndToStarters(Graph graph, Coordinate start, Coordinate end) {
        final long startTime = System.currentTimeMillis();


        Queue<BacktrackableVertex> queue = new LinkedList<>();
        Set<GraphVertex> expandedVertices = new HashSet<>();

        final GraphVertex endVertex = graph.vertices().get(end);
        assert(endVertex != null);

        queue.add(new BacktrackableVertex(endVertex, null, "end"));
        while(queue.peek() != null) {

            // Expand next vertex if new
            final BacktrackableVertex currentBacktrackableVertex = queue.remove();
            final GraphVertex currentVertex = currentBacktrackableVertex.vertex;
            if (!expandedVertices.contains(currentVertex)) {
                expandedVertices.add(currentVertex);

                // Goal found?
                final boolean startCoordinateReached = currentVertex.coordinate.equals(start);
                if (startCoordinateReached) {
                    final BacktrackableVertex s = new BacktrackableVertex(currentVertex, currentBacktrackableVertex, "start");
                    return new PathFinderResult(true, this.backtrack(s), System.currentTimeMillis() - startTime);
                }
                final Optional<Teleport> reachedStarter = graph.teleports().stream().filter(s -> s.destination().equals(currentVertex.coordinate)).findAny();
                if(reachedStarter.isPresent()) {
                    final BacktrackableVertex s = new BacktrackableVertex(currentVertex, currentBacktrackableVertex, reachedStarter.get().title());
                    return new PathFinderResult(true, this.backtrack(s), System.currentTimeMillis() - startTime);
                }

                // Add neighbours of vertex into queue
                for (GraphEdge neighbor : currentVertex.neighbors) {
                    queue.add(new BacktrackableVertex(neighbor.to(), currentBacktrackableVertex, neighbor.methodOfMovement()));
                }
            }
        }


        return new PathFinderResult(false, null, System.currentTimeMillis() - startTime);
    }





    /**
     * Backtracks from a found goal vertex to the start
     *
     * @param goal The found goal vertex with backtracking references
     * @return the PathFinder Result
     */
    private List<PathFinderResult.Movement> backtrack(BacktrackableVertex goal) {
        List<PathFinderResult.Movement> path = new LinkedList<>();

        BacktrackableVertex current = goal;
        while (current.hasPrevious()) {
            path.add(0, new PathFinderResult.Movement(current.vertex.coordinate, current.methodOfMovement));
            current = current.previous;
        }

        // Reached the start
        path.add(0, new PathFinderResult.Movement(current.vertex.coordinate, current.methodOfMovement));

        return path;
    }

    /**
     * Used internally when pathfinding to backtrack from goal vertex back to start
     */
    private record BacktrackableVertex(GraphVertex vertex, BacktrackableVertex previous, String methodOfMovement) {
        public boolean hasPrevious() {
            return this.previous != null;
        }
    }
}
