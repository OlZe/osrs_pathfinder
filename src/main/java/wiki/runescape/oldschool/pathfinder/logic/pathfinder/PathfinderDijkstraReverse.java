package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphBuilder;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphVertex;
import wiki.runescape.oldschool.pathfinder.logic.graph.Teleport;
import wiki.runescape.oldschool.pathfinder.logic.queues.PriorityQueueTieByTime;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderDijkstraReverse extends Pathfinder {

    /**
     * Allows efficient query of teleports for a given coordinate.
     */
    private final Map<Coordinate, List<Teleport>> teleports;

    public PathfinderDijkstraReverse(final Graph graph) {
        super(graph);
        this.teleports = graph.teleports().stream().collect(Collectors.groupingBy(tp -> tp.to().coordinate()));
    }

    @Override
    public PathfinderResult findPath(final Coordinate start, final Coordinate end, final Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        // Determine ending position
        final GraphVertex endVertex = graph.vertices().get(end);
        assert endVertex != null;
        final GraphVertex startVertex = graph.vertices().get(start);
        assert startVertex != null;
        final DijkstraQueueEntry firstQueueEntry = new DijkstraQueueEntry(endVertex, null, "end", 0);

        // if start == end, return
        if (start.equals(end)) {
            return new PathfinderResult(true, this.backtrack(firstQueueEntry), 0, System.currentTimeMillis() - startTime, 0, 0);
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


            final boolean isWalking = current.methodOfMovement().startsWith(GraphBuilder.WALK_PREFIX);

            // Start found?
            if (current.vertex().coordinate().equals(start)) {
                float totalCost = current.totalCost();
                if (isWalking) {
                    // Round up because walking stops
                    totalCost = (float) Math.ceil(totalCost);
                }
                return new PathfinderResult(true, this.backtrack(current), (int) totalCost, System.currentTimeMillis() - startTime, closed_list.size(), open_list.size());
            }


            // Add neighbours of vertex to open_list
            current.vertex().edgesIn().stream()
                    .filter(edgeIn -> !blacklist.contains(edgeIn.title()))
                    .map(edgeIn -> {
                        float totalCost = current.totalCost();
                        if (isWalking) {
                            // Round up if walking stops
                            final boolean isGoingToWalk = edgeIn.title().startsWith(GraphBuilder.WALK_PREFIX);
                            if (!isGoingToWalk) {
                                totalCost = (float) Math.ceil(totalCost);
                            }
                        }
                        totalCost += edgeIn.cost();
                        return new DijkstraQueueEntry(edgeIn.from(), current, edgeIn.title(), totalCost);
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
                            totalCost += teleport.cost();
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
        return new PathfinderResult(false, null, 0, System.currentTimeMillis() - startTime, closed_list.size(), open_list.size());

    }

    private List<PathfinderResult.Movement> backtrack(DijkstraQueueEntry start) {
        List<PathfinderResult.Movement> path = new LinkedList<>();
        path.add(new PathfinderResult.Movement(start.vertex().coordinate(), "start"));

        DijkstraQueueEntry current = start;
        while (!current.isEnd()) {
            path.add(new PathfinderResult.Movement(current.successor().vertex().coordinate(), current.methodOfMovement()));
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
