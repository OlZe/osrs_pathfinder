package wiki.runescape.oldschool.pathfinder.logic;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PathFinderDijkstra implements PathFinder {

    @Override
    public PathFinder.Result findPath(Graph graph, Coordinate start, Coordinate end, Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        // Determine starting position
        final GraphVertex startVertex = graph.vertices().get(start);
        assert (startVertex != null);
        final DijkstraQueueEntry firstDijkstraQueueEntry = new DijkstraQueueEntry(startVertex, null, "start", 0);

        // if start == end, return
        if (start.equals(end)) {
            return new PathFinder.Result(true, this.backtrack(firstDijkstraQueueEntry), System.currentTimeMillis() - startTime);
        }

        // Init open_list and closed_list
        final Set<Coordinate> closed_list = new HashSet<>();
        final PriorityQueueTieByTime<DijkstraQueueEntry> open_list = new PriorityQueueTieByTime<>();
        open_list.add(firstDijkstraQueueEntry);

        boolean addedTeleportsUpTo30Wildy = false;
        boolean addedTeleportsUpTo20Wildy = false;
        while (open_list.peek() != null) {
            final DijkstraQueueEntry current = open_list.remove();

            if (closed_list.contains(current.vertex().coordinate())) {
                continue;
            }
            closed_list.add(current.vertex().coordinate());

            // Goal found?
            if (current.vertex().coordinate().equals(end)) {
                return new PathFinder.Result(true, this.backtrack(current), System.currentTimeMillis() - startTime);
            }

            final boolean walkedHere = current.methodOfMovement().startsWith(GraphBuilder.WALK_PREFIX);

            // Add neighbours of vertex to open_list
            current.vertex().neighbors().stream()
                    .filter(n -> !blacklist.contains(n.methodOfMovement()))
                    .map(n -> {
                        float totalCost = current.totalCost();
                        if (walkedHere) {
                            // Round up if walking stops
                            final boolean isGoingToWalk = n.methodOfMovement().startsWith(GraphBuilder.WALK_PREFIX);
                            if (!isGoingToWalk) {
                                totalCost = (float) Math.ceil(totalCost);
                            }
                        }
                        totalCost += n.cost();
                        return new DijkstraQueueEntry(n.to(), current, n.methodOfMovement(), totalCost);
                    })
                    .forEachOrdered(open_list::add);

            // If teleports haven't been added, add them to open_list, depending on wildy level
            // Teleports up to lvl 30
            final boolean addTeleportsUpTo30Wildy = !addedTeleportsUpTo30Wildy && !(current.vertex.wildernessLevel().equals(PositionInfo.WildernessLevels.ABOVE30));
            if (addTeleportsUpTo30Wildy) {
                graph.teleports().stream()
                        .filter(Teleport::canTeleportUpTo30Wildy)
                        .filter(tp -> !blacklist.contains(tp.title()))
                        .filter(tp -> graph.vertices().containsKey(tp.destination()))
                        .map(tp -> {
                            float totalCost = current.totalCost();
                            if (walkedHere) {
                                totalCost = (float) Math.ceil(totalCost);
                            }
                            totalCost += tp.duration();
                            return new DijkstraQueueEntry(graph.vertices().get(tp.destination()), current, tp.title(), totalCost);
                        })
                        .forEachOrdered(open_list::add);
                addedTeleportsUpTo30Wildy = true;
            }

            // Teleports up to lvl 20
            final boolean addTeleportsUpTo20Wildy = !addedTeleportsUpTo20Wildy && current.vertex.wildernessLevel().equals(PositionInfo.WildernessLevels.BELOW20);
            if (addTeleportsUpTo20Wildy) {
                graph.teleports().stream()
                        .filter(tp -> !tp.canTeleportUpTo30Wildy())
                        .filter(tp -> !blacklist.contains(tp.title()))
                        .filter(tp -> graph.vertices().containsKey(tp.destination()))
                        .map(tp -> {
                            float totalCost = current.totalCost();
                            if (walkedHere) {
                                totalCost = (float) Math.ceil(totalCost);
                            }
                            totalCost += tp.duration();
                            return new DijkstraQueueEntry(graph.vertices().get(tp.destination()), current, tp.title(), totalCost);
                        })
                        .forEachOrdered(open_list::add);
                addedTeleportsUpTo20Wildy = true;
            }
        }

        // No Path found
        return new PathFinder.Result(false, null, System.currentTimeMillis() - startTime);
    }


    /**
     * Backtracks from a found goal vertex to the start
     *
     * @param goal The found goal vertex with backtracking references
     * @return the PathFinder Result
     */
    private List<PathFinder.Result.Movement> backtrack(DijkstraQueueEntry goal) {
        List<PathFinder.Result.Movement> path = new LinkedList<>();

        DijkstraQueueEntry current = goal;
        while (current.hasPrevious()) {
            path.add(0, new PathFinder.Result.Movement(current.vertex().coordinate(), current.methodOfMovement()));
            current = current.previous();
        }

        // Reached the start
        path.add(0, new PathFinder.Result.Movement(current.vertex().coordinate(), current.methodOfMovement()));

        return path;
    }

    private record DijkstraQueueEntry(
            GraphVertex vertex,
            DijkstraQueueEntry previous,
            String methodOfMovement,
            float totalCost)

            implements Comparable<DijkstraQueueEntry> {

        public boolean hasPrevious() {
            return this.previous != null;
        }

        @Override
        public int compareTo(final DijkstraQueueEntry o) {
            return Float.compare(this.totalCost, o.totalCost);
        }
    }
}
