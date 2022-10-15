package myImplementation;

import java.util.*;

public class PathFinder {

    /**
     * Attempts to find a path using Breadth First Search algorithm.
     * DISREGARDS teleports as it goes from start point to end point
     *
     * @param start The start node
     * @param end   The end coordinate
     */
    public PathFinderResult findPathBfsStartToEnd(GraphNode start, Point end) {
        final long startTime = System.currentTimeMillis();

        Queue<NodeWithBacktrack> queue = new LinkedList<>();
        Set<GraphNode> expandedNodes = new HashSet<>();

        queue.add(new NodeWithBacktrack(start, null, "start"));

        while (queue.peek() != null) {

            // Expand next node if new
            final NodeWithBacktrack currentNodeWithBacktrack = queue.remove();
            final GraphNode currentNode = currentNodeWithBacktrack.node;
            if (!expandedNodes.contains(currentNode)) {
                expandedNodes.add(currentNode);

                // Goal found?
                if (currentNode.coordinate.equals(end)) {
                    return new PathFinderResult(true, this.backtrack(currentNodeWithBacktrack), System.currentTimeMillis() - startTime);
                }

                // Add neighbours of node into queue
                for (GraphNodeNeighbour neighbor : currentNode.neighbors) {
                    queue.add(new NodeWithBacktrack(neighbor.node(), currentNodeWithBacktrack, neighbor.methodOfMovement()));
                }
            }
        }

        return new PathFinderResult(false, null, System.currentTimeMillis() - startTime);
    }

    /**
     * Attempts to find a path using Breadth First Search Algorithm.
     * Starts at the end goal and keeps looking until it finds either the start point or a starter teleport
     * @param graph The Graph
     * @param start The starting position of the character
     * @param end The destination position
     */
    public PathFinderResult findPathBfsEndToStarters(Graph graph, Point start, Point end) {
        final long startTime = System.currentTimeMillis();


        Queue<NodeWithBacktrack> queue = new LinkedList<>();
        Set<GraphNode> expandedNodes = new HashSet<>();

        final GraphNode endNode = graph.nodes().get(end);
        assert(endNode != null);

        queue.add(new NodeWithBacktrack(endNode, null, "end"));
        while(queue.peek() != null) {

            // Expand next node if new
            final NodeWithBacktrack currentNodeWithBacktrack = queue.remove();
            final GraphNode currentNode = currentNodeWithBacktrack.node;
            if (!expandedNodes.contains(currentNode)) {
                expandedNodes.add(currentNode);

                // Goal found?
                final boolean startPointReached = currentNode.coordinate.equals(start);
                if (startPointReached) {
                    final NodeWithBacktrack s = new NodeWithBacktrack(currentNode, currentNodeWithBacktrack, "start");
                    return new PathFinderResult(true, this.backtrack(s), System.currentTimeMillis() - startTime);
                }
                final Optional<Graph.Starter> reachedStarter = graph.starters().stream().filter(s -> s.coordinate().equals(currentNode.coordinate)).findAny();
                if(reachedStarter.isPresent()) {
                    final NodeWithBacktrack s = new NodeWithBacktrack(currentNode, currentNodeWithBacktrack, reachedStarter.get().title());
                    return new PathFinderResult(true, this.backtrack(s), System.currentTimeMillis() - startTime);
                }

                // Add neighbours of node into queue
                for (GraphNodeNeighbour neighbor : currentNode.neighbors) {
                    queue.add(new NodeWithBacktrack(neighbor.node(), currentNodeWithBacktrack, neighbor.methodOfMovement()));
                }
            }
        }


        return new PathFinderResult(false, null, System.currentTimeMillis() - startTime);
    }





    /**
     * Backtracks from a found goal node to the start
     *
     * @param goal The found goal node with backtracking references
     * @return the PathFinder Result
     */
    private List<PathFinderResult.Movement> backtrack(NodeWithBacktrack goal) {
        List<PathFinderResult.Movement> path = new LinkedList<>();

        NodeWithBacktrack current = goal;
        while (current.hasPrevious()) {
            path.add(0, new PathFinderResult.Movement(current.node.coordinate, current.methodOfMovement));
            current = current.previous;
        }

        // Reached the start
        path.add(0, new PathFinderResult.Movement(current.node.coordinate, current.methodOfMovement));

        return path;
    }

    /**
     * Used internally when pathfinding to backtrack from goal node back to start
     */
    private record NodeWithBacktrack(GraphNode node, NodeWithBacktrack previous, String methodOfMovement) {
        public boolean hasPrevious() {
            return this.previous != null;
        }
    }
}
