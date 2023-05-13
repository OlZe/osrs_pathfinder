package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.graph.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingPriorityQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueue;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderDijkstraReverse extends Pathfinder {

    /**
     * Allows efficient query of teleports for a given vertex.
     */
    private final Map<GraphVertex, List<Teleport>> teleports;

    public PathfinderDijkstraReverse(final Graph graph) {
        super(graph);
        this.teleports = graph.teleports().stream().collect(Collectors.groupingBy(Teleport::to));
    }

    @Override
    public PartialPathfinderResult findPath(final GraphVertex start, final GraphVertex end, final HashSet<String> blacklist) {
        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueue openList = new PathfindingPriorityQueue();
        openList.enqueue(new GraphEdgeImpl(end, null, 0, "end", false), null);

        while (openList.hasNext()) {
            final PathfindingQueue.Entry currentEntry = openList.dequeue();
            final GraphVertex currentVertex = currentEntry.edge().from();

            if (closedList.contains(currentVertex)) {
                continue;
            }
            closedList.add(currentVertex);

            // Teleport or start found?
            if (currentVertex.equals(start)) {
                return new PartialPathfinderResult(
                        true,
                        this.backtrack(currentEntry),
                        currentEntry.totalCostX2(),
                        closedList.size(),
                        openList.size());
            }


            // Add predecessors of vertex to openList
            for (GraphEdge edge : currentVertex.edgesIn()) {
                if (!blacklist.contains(edge.title()) && !closedList.contains(edge.from())) {
                    openList.enqueue(edge, currentEntry);
                }
            }


            // Add teleports going here to openList
            final List<Teleport> teleportsHere = this.teleports.get(currentVertex);
            if (teleportsHere != null) {
                for (Teleport teleportHere : teleportsHere) {
                    if (!blacklist.contains(teleportHere.title())) {
                        // Enqueue a new edge where the teleport originates from the start vertex
                        openList.enqueue(new GraphEdgeImpl(start, teleportHere.to(), teleportHere.costX2(), teleportHere.title(), teleportHere.isWalking()), currentEntry);
                    }
                }
            }
        }

        // No path found
        return new PartialPathfinderResult(
                false,
                null,
                0,
                closedList.size(),
                openList.size());

    }

    private List<PathfinderResult.Movement> backtrack(PathfindingQueue.Entry start) {
        List<PathfinderResult.Movement> path = new ArrayList<>();

        path.add(new PathfinderResult.Movement(start.edge().from().coordinate(), Pathfinder.MOVEMENT_START_TITLE));

        PathfindingQueue.Entry current = start;
        while (current.previous() != null) {
            path.add(new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        return path;
    }
}
