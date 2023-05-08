package wiki.runescape.oldschool.pathfinder.logic;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PathFinderDijkstraReverse implements PathFinder {


    @Override
    public Result findPath(final Graph graph, final Coordinate start, final Coordinate end, final Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        // Determine ending position
        final GraphVertex endVertex = graph.vertices().get(end);
        assert endVertex != null;
        final DijkstraQueueEntry firstQueueEntry = new DijkstraQueueEntry(endVertex, null, "end", 0);

        // if start == end, return
        if(start.equals(end)) {
            return new PathFinder.Result(true, this.backtrack(firstQueueEntry), System.currentTimeMillis() - startTime);
        }

        // Init open_list and closed_list
        final Set<Coordinate> closed_list = new HashSet<>();
        final PriorityQueueTieByTime<DijkstraQueueEntry> open_list = new PriorityQueueTieByTime<>();
        open_list.add(firstQueueEntry);

        while(open_list.peek() != null) {
            final DijkstraQueueEntry current = open_list.remove();

            if(closed_list.contains(current.vertex().coordinate())) {
                continue;
            }
            closed_list.add(current.vertex().coordinate());

            // Start found?
            if(current.vertex().coordinate().equals(start)) {
                return new PathFinder.Result(true, this.backtrack(current), System.currentTimeMillis() - startTime);
            }

            // Add neighbours of vertex to open_list
            current.vertex().edgesIn().stream()
                    .filter(edgeIn -> !blacklist.contains(edgeIn.methodOfMovement()))
                    .map(edgeIn -> new DijkstraQueueEntry(edgeIn.from(), current, edgeIn.methodOfMovement(), current.totalCost() + edgeIn.cost()))
                    .forEachOrdered(open_list::add);
        }

        // No path found
        return new PathFinder.Result(false, null, System.currentTimeMillis() - startTime);

    }

    private List<Result.Movement> backtrack(DijkstraQueueEntry start) {
        List<PathFinder.Result.Movement> path = new LinkedList<>();
        path.add(new Result.Movement(start.vertex().coordinate(), "start"));

        DijkstraQueueEntry current = start;
        while(!current.isEnd()) {
            path.add(new Result.Movement(current.successor().vertex().coordinate(), current.methodOfMovement()));
            current = current.successor();
        }

        return path;
    }


    private record DijkstraQueueEntry(
            GraphVertex vertex,
            DijkstraQueueEntry successor,
            String methodOfMovement,
            float totalCost)

            implements Comparable<DijkstraQueueEntry> {

        public boolean isEnd() {
            return this.successor == null;
        }

        @Override
        public int compareTo(final DijkstraQueueEntry o) {
            return Float.compare(this.totalCost, o.totalCost);
        }
    }
}