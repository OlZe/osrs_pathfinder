package myImplementation;

import java.util.*;

public class PathFinder {

    /**
     * Attempts to find a path using Breadth First Search algorithm
     * @param start The start node
     * @param end The end coordinate
     */
    public PathFinderResult findPathBfs(GraphNode start, Point end) {
        Queue<NodeWithBacktrack> queue = new LinkedList<>();
        Set<GraphNode> expandedNodes = new HashSet<>();

        queue.add(new NodeWithBacktrack(start, null));

        while (queue.peek() != null) {

            // Expand next node if new
            final NodeWithBacktrack currentNodeWithBacktrack = queue.remove();
            final GraphNode currentNode = currentNodeWithBacktrack.node;
            if (!expandedNodes.contains(currentNode)) {
                expandedNodes.add(currentNode);

                // Goal found?
                if (currentNode.coordinate.equals(end)) {
                    return this.backtrack(currentNodeWithBacktrack);
                }

                // Add neighbours of node into queue
                for(GraphNode neighbor : currentNode.neighbors) {
                    queue.add(new NodeWithBacktrack(neighbor, currentNodeWithBacktrack));
                }
            }
        }

        return new PathFinderResult(false, null);
    }

    /**
     * Backtracks from a found goal node to the start
     * @param goal The found goal node with backtracking references
     * @return the PathFinder Result
     */
    private PathFinderResult backtrack(NodeWithBacktrack goal) {
        List<Point> path = new LinkedList<>();

        NodeWithBacktrack current = goal;
        while(current.hasPrevious()) {
            path.add(0,current.node.coordinate);
            current = current.previous;
        }

        // Reached the start
        path.add(0,current.node.coordinate);

        return new PathFinderResult(true, path);
    }

    /**
     * Used internally when pathfinding to backtrack from goal node back to start
     */
    private record NodeWithBacktrack(GraphNode node, NodeWithBacktrack previous) {
        public boolean hasPrevious() {
            return this.previous != null;
        }
    }
}
