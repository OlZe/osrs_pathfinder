package wiki.runescape.oldschool.pathfinder.logic;

import java.util.*;
import java.util.stream.Collectors;

public class PathFinderDijkstraReverse implements PathFinder {

    /**
     * Allows efficient query of teleports for a given coordinate.
     */
    private final Map<Coordinate, List<Teleport>> teleports;

    public PathFinderDijkstraReverse(final Collection<Teleport> teleports) {
        this.teleports = teleports.stream().collect(Collectors.groupingBy(Teleport::destination));
    }

    @Override
    public Result findPath(final Graph graph, final Coordinate start, final Coordinate end, final Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        // Determine ending position
        final GraphVertex endVertex = graph.vertices().get(end);
        assert endVertex != null;
        final GraphVertex startVertex = graph.vertices().get(start);
        assert startVertex != null;
        final DijkstraQueueEntry firstQueueEntry = new DijkstraQueueEntry(endVertex, null, "end", 0);

        // if start == end, return
        if (start.equals(end)) {
            return new PathFinder.Result(true, this.backtrack(firstQueueEntry), 0, System.currentTimeMillis() - startTime, 0, 0);
        }

        // Init open_list and closed_list
        final Set<Coordinate> closed_list = new HashSet<>();
        final PriorityQueueTieByTime<DijkstraQueueEntry> open_list = new PriorityQueueTieByTime<>();
        open_list.add(firstQueueEntry);

        while (open_list.peek() != null) {
            final DijkstraQueueEntry current = open_list.remove();

            if (closed_list.contains(current.vertex().coordinate())) {
                continue;
            }
            closed_list.add(current.vertex().coordinate());

            // Start found?
            if (current.vertex().coordinate().equals(start)) {
                return new PathFinder.Result(true, this.backtrack(current), (int) current.totalCost(), System.currentTimeMillis() - startTime, closed_list.size(), open_list.size());
            }

            final boolean isWalking = current.methodOfMovement().startsWith(GraphBuilder.WALK_PREFIX);

            // Add neighbours of vertex to open_list
            current.vertex().edgesIn().stream()
                    .filter(edgeIn -> !blacklist.contains(edgeIn.methodOfMovement()))
                    .map(edgeIn -> {
                        float totalCost = current.totalCost();
                        if (isWalking) {
                            // Round up if walking stops
                            final boolean isGoingToWalk = edgeIn.methodOfMovement().startsWith(GraphBuilder.WALK_PREFIX);
                            if (!isGoingToWalk) {
                                totalCost = (float) Math.ceil(totalCost);
                            }
                        }
                        totalCost += edgeIn.cost();
                        return new DijkstraQueueEntry(edgeIn.from(), current, edgeIn.methodOfMovement(), totalCost);
                    })
                    .forEachOrdered(open_list::add);

            // Add teleports going here to open_list
            final List<Teleport> teleportsHere = this.teleports.get(current.vertex().coordinate());
            if (teleportsHere != null) {
                teleportsHere.stream()
                        .filter(teleport -> !blacklist.contains(teleport.title()))
                        .map(teleport -> {
                            float totalCost = current.totalCost();
                            if (isWalking) {
                                totalCost = (float) Math.ceil(totalCost);
                            }
                            totalCost += teleport.duration();
                            return new DijkstraQueueEntry(
                                    graph.vertices().get(start),
                                    current,
                                    teleport.title(),
                                    totalCost);
                        })
                        .forEachOrdered(open_list::add);
            }
        }

        // No path found
        return new PathFinder.Result(false, null, 0, System.currentTimeMillis() - startTime, closed_list.size(), open_list.size());

    }

    private List<Result.Movement> backtrack(DijkstraQueueEntry start) {
        List<PathFinder.Result.Movement> path = new LinkedList<>();
        path.add(new Result.Movement(start.vertex().coordinate(), "start"));

        DijkstraQueueEntry current = start;
        while (!current.isEnd()) {
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
