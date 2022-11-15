package myImplementation;

import java.util.*;

public class PathFinder {


    public PathFinderResult findPath(Graph graph, Coordinate start, Coordinate end) {
        final long startTime = System.currentTimeMillis();

        final Queue<DijkstraQueueEntry> queue = new PriorityQueue<>();
        final Set<GraphVertex> expandedVertices = new HashSet<>();

        // Add starting points to queue
        final GraphVertex startVertex = graph.vertices().get(start);
        assert (startVertex != null);
        queue.add(new DijkstraQueueEntry(startVertex, null, "start", 0));
        for (Teleport teleport : graph.teleports()) {
            final GraphVertex tpVertex = graph.vertices().get(teleport.destination());
            if (tpVertex == null) {
                // Teleport destination does not exist in graph, skip
                System.out.println("Teleport : " + teleport.title() + " coordinate " + teleport.destination() + " is not in graph.");
                continue;
            }
            queue.add(new DijkstraQueueEntry(tpVertex, null, teleport.title(), teleport.duration()));
        }

        while (queue.peek() != null) {

            // Expand next vertex if new
            final DijkstraQueueEntry current = queue.remove();
            final GraphVertex currentVertex = current.vertex;
            if (!expandedVertices.contains(currentVertex)) {
                expandedVertices.add(currentVertex);

                // Goal found?
                if (currentVertex.coordinate.equals(end)) {
                    return new PathFinderResult(true, this.backtrack(current), System.currentTimeMillis() - startTime);
                }

                // Add neighbours of vertex into queue
                for (GraphEdge neighbor : currentVertex.neighbors) {
                    final int newCostToNeighbour = current.totalCost + neighbor.cost();
                    final DijkstraQueueEntry newQueueEntry = new DijkstraQueueEntry(neighbor.to(), current, neighbor.methodOfMovement(), newCostToNeighbour);
                    queue.add(newQueueEntry);
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
    private List<PathFinderResult.Movement> backtrack(DijkstraQueueEntry goal) {
        List<PathFinderResult.Movement> path = new LinkedList<>();

        DijkstraQueueEntry current = goal;
        while (current.hasPrevious()) {
            path.add(0, new PathFinderResult.Movement(current.vertex.coordinate, current.methodOfMovement));
            current = current.previous;
        }

        // Reached the start
        path.add(0, new PathFinderResult.Movement(current.vertex.coordinate, current.methodOfMovement));

        return path;
    }


    private record DijkstraQueueEntry(GraphVertex vertex,
                                      DijkstraQueueEntry previous,
                                      String methodOfMovement,
                                      int totalCost)
            implements Comparable<DijkstraQueueEntry> {

        public boolean hasPrevious() {
            return this.previous != null;
        }

        @Override
        public int compareTo(final DijkstraQueueEntry o) {
            return this.totalCost - o.totalCost;
        }
    }
}
