package myImplementation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class PathFinder {

    /**
     * Attempts to find a path using Breadth First Search algorithm
     * @param start The start node
     * @param end The end coordinate
     */
    public void findPathBfs(GraphNode start, Point end) {
        Queue<GraphNode> queue = new LinkedList<>();
        Set<GraphNode> expandedNodes = new HashSet<>();

        queue.add(start);

        while(queue.peek() != null) {

            // Expand next node if new
            GraphNode currentNode = queue.remove();
            if(!expandedNodes.contains(currentNode)) {
                expandedNodes.add(currentNode);

                // Goal found?
                if (currentNode.coordinate.equals(end)) {
                    System.out.println("found!");
                    break;
                }

                // Add neighbours of node into queue
                queue.addAll(currentNode.neighbors);
            }
        }

        return;
    }
}
