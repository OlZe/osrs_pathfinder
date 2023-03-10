package wiki.runescape.oldschool.pathfinder.logic;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PathFinder {


    public PathFinderResult findPath(Graph graph, Coordinate start, Coordinate end, Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        final PriorityQueueTieByTime<DijkstraQueueEntry> open_list = new PriorityQueueTieByTime<>();
        final Set<Coordinate> closed_list = new HashSet<>();

        // Determine starting position
        final GraphVertex startVertex = graph.vertices().get(start);
        assert (startVertex != null);
        final DijkstraQueueEntry firstDijkstraQueueEntry = new DijkstraQueueEntry(startVertex, null, "start", 0);

        // start == end, return
        if (start.equals(end)) {
            return new PathFinderResult(true, this.backtrack(firstDijkstraQueueEntry), System.currentTimeMillis() - startTime);
        }

        // Add start to open_list
        open_list.add(firstDijkstraQueueEntry);
        closed_list.add(firstDijkstraQueueEntry.vertex.coordinate());

        // Add teleports to open_list
        graph.teleports().stream()
                .map(tp -> new DijkstraQueueEntry(graph.vertices().get(tp.destination()), firstDijkstraQueueEntry, tp.title(), tp.duration()))
                .filter(entry -> entry.vertex() != null)
                .filter(entry -> !blacklist.contains(entry.methodOfMovement))
                .filter(entry -> !closed_list.contains(entry.vertex.coordinate()))
                .forEachOrdered(entry -> {
                    open_list.add(entry);
                    closed_list.add(entry.vertex().coordinate());
                });

        while (open_list.peek() != null) {
            final DijkstraQueueEntry current = open_list.remove();

            // Goal found?
            if (current.vertex().coordinate().equals(end)) {
                return new PathFinderResult(true, this.backtrack(current), System.currentTimeMillis() - startTime);
            }

            // Add neighbours of vertex into open_list if not in closed_list
            current.vertex().neighbors().stream()
                    .map(n -> new DijkstraQueueEntry(n.to(), current, n.methodOfMovement(), (current.totalCost()) + n.cost()))
                    .filter(entry -> !blacklist.contains(entry.methodOfMovement()))
                    .filter(entry -> !closed_list.contains(entry.vertex().coordinate()))
                    .forEachOrdered(entry -> {
                        open_list.add(entry);
                        closed_list.add(entry.vertex().coordinate());
                    });
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
            path.add(0, new PathFinderResult.Movement(current.vertex().coordinate(), current.methodOfMovement()));
            current = current.previous();
        }

        // Reached the start
        path.add(0, new PathFinderResult.Movement(current.vertex().coordinate(), current.methodOfMovement()));

        return path;
    }

    private record DijkstraQueueEntry(
            GraphVertex vertex,
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
