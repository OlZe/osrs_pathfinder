package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueue;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderDijkstraBackwards extends PathfinderWeighted {

    private final Map<GraphVertex, List<Teleport>> teleportsTo30Map;
    private final Map<GraphVertex, List<Teleport>> teleportsTo20Map;
    private final Class<? extends PathfindingQueue> queueClass;

    public PathfinderDijkstraBackwards(final Graph graph, final Class<? extends PathfindingQueue> queueClass) {
        super(graph);
        this.teleportsTo30Map = graph.teleports().stream().filter(Teleport::canTeleportUpTo30Wildy).collect(Collectors.groupingBy(Teleport::to));
        this.teleportsTo20Map = graph.teleports().stream().filter(tp -> !tp.canTeleportUpTo30Wildy()).collect(Collectors.groupingBy(Teleport::to));
        this.queueClass = queueClass;
    }

    @Override
    protected PartialPathfinderResult findPath(final GraphVertex start, final GraphVertex end, final HashSet<String> blacklist) {
        final WildernessExits wildernessExits = this.findWildernessExits(start, end, blacklist);

        // If goal found by chance, return path
        if (wildernessExits.pathFound()) {
            return new PartialPathfinderResult(true, wildernessExits.path(), wildernessExits.pathTotalCostX2(), wildernessExits.amountExpandedVertices(), wildernessExits.amountVerticesLeftInQueue());
        }

        // If stuck in wilderness, return no path found
        if (!wildernessExits.exitsFound()) {
            return new PartialPathfinderResult(false, null, 0, wildernessExits.amountExpandedVertices(), wildernessExits.amountVerticesLeftInQueue());
        }

        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueue openList = this.instantiatePathfindingQueue();
        openList.enqueue(new GraphEdgeImpl(end, null, 0, "end", false), null);

        while (openList.hasNext()) {
            final PathfindingQueue.Entry currentEntry = openList.dequeue();
            final GraphVertex currentVertex = currentEntry.edge().from();

            if (closedList.contains(currentVertex)) {
                continue;
            }
            closedList.add(currentVertex);

            // Start found?
            if (currentVertex.equals(start)) {
                return new PartialPathfinderResult(
                        true,
                        this.makePathBackwards(currentEntry),
                        currentEntry.totalCostX2(),
                        closedList.size() + wildernessExits.amountVerticesLeftInQueue(),
                        openList.size() + wildernessExits.amountVerticesLeftInQueue());
            }

            // Add predecessors of vertex to openList
            for (GraphEdge edge : currentVertex.edgesIn()) {
                if (!blacklist.contains(edge.title()) && !closedList.contains(edge.from())) {
                    openList.enqueue(edge, currentEntry);
                }
            }

            // If a teleport goes here, enqueue a phantom edge representing the teleport originating from its corresponding wilderness exit
            final List<Teleport> teleportsUpTo30Here = this.teleportsTo30Map.get(currentVertex);
            if (teleportsUpTo30Here != null && !closedList.contains(wildernessExits.exit30)) {
                for (Teleport teleportHere : teleportsUpTo30Here) {
                    if (!blacklist.contains(teleportHere.title())) {
                        openList.enqueue(new GraphEdgeImpl(wildernessExits.exit30(), teleportHere.to(), teleportHere.costX2(), teleportHere.title(), teleportHere.isWalking()), currentEntry);
                    }
                }
            }
            final List<Teleport> teleportsUpTo20Here = this.teleportsTo20Map.get(currentVertex);
            if (teleportsUpTo20Here != null && !closedList.contains(wildernessExits.exit20())) {
                for (Teleport teleportHere : teleportsUpTo20Here) {
                    if (!blacklist.contains(teleportHere.title())) {
                        openList.enqueue(new GraphEdgeImpl(wildernessExits.exit20(), teleportHere.to(), teleportHere.costX2(), teleportHere.title(), teleportHere.isWalking()), currentEntry);
                    }
                }
            }
        }

        // No path found
        return new PartialPathfinderResult(
                false,
                null,
                0,
                closedList.size() + wildernessExits.amountExpandedVertices(),
                openList.size() + wildernessExits.amountVerticesLeftInQueue());

    }

    private List<PathfinderResult.Movement> makePathBackwards(PathfindingQueue.Entry start) {
        List<PathfinderResult.Movement> path = new ArrayList<>();
        path.add(new PathfinderResult.Movement(start.edge().from().coordinate(), PathfinderResult.MOVEMENT_START_TITLE));

        PathfindingQueue.Entry current = start;
        while (current.previous() != null) {
            path.add(new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        return path;
    }

    /**
     * Finds the nearest Lvl 30 and Lvl 20 wilderness exits. If by chance the end is found, it is returned as a path
     */
    private WildernessExits findWildernessExits(GraphVertex start, GraphVertex end, HashSet<String> blacklist) {
        if (start.wildernessLevel() == WildernessLevels.BELOW20) {
            return new WildernessExits(false, null, 0, true, start, start, 0, 0);
        }

        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueue openList = this.instantiatePathfindingQueue();
        openList.enqueue(new GraphEdgeImpl(null, start, 0, PathfinderResult.MOVEMENT_START_TITLE, false), null);

        GraphVertex exit30 = null;
        GraphVertex exit20 = null;

        while (openList.hasNext()) {
            final PathfindingQueue.Entry currentEntry = openList.dequeue();
            final GraphVertex currentVertex = currentEntry.edge().to();

            if (closedList.contains(currentVertex)) {
                continue;
            }
            closedList.add(currentVertex);

            // Goal found?
            if (currentVertex.equals(end)) {
                return new WildernessExits(true,
                        makePathForwards(currentEntry),
                        currentEntry.totalCostX2(),
                        false,
                        null,
                        null,
                        closedList.size(),
                        openList.size());
            }

            // Exit found?
            if (currentVertex.wildernessLevel() == WildernessLevels.BELOW20) {
                exit20 = currentVertex;

                // If no exit to lvl 30 has been found yet, use the lvl 20 exit as it is faster
                if (exit30 == null) {
                    exit30 = exit20;
                }

                return new WildernessExits(false, null, 0, true, exit30, exit20, closedList.size(), openList.size());

            } else if (currentVertex.wildernessLevel() == WildernessLevels.BETWEEN20AND30) {
                if (exit30 == null) {
                    exit30 = currentVertex;

                    // Add lvl 30 wildy teleports
                    for (List<Teleport> teleportsTo30 : this.teleportsTo30Map.values()) {
                        for (Teleport teleportTo30 : teleportsTo30) {
                            if (!blacklist.contains(teleportTo30.title()) && !closedList.contains(teleportTo30.to())) {
                                openList.enqueue(teleportTo30, currentEntry);
                            }
                        }

                    }
                }
            }

            // Add neighbours of vertex to openList
            for (GraphEdge edge : currentVertex.edgesOut()) {
                if (!blacklist.contains(edge.title()) && !closedList.contains(edge.to())) {
                    openList.enqueue(edge, currentEntry);
                }
            }
        }

        return new WildernessExits(false, null, 0, false, null, null, closedList.size(), openList.size());
    }

    private List<PathfinderResult.Movement> makePathForwards(final PathfindingQueue.Entry end) {
        List<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueue.Entry current = end;
        while (current != null) {
            path.add(0, new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        return path;
    }

    private PathfindingQueue instantiatePathfindingQueue() {
        try {
            return this.queueClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate PathfindingQueue - Class: " + this.queueClass.toString(), e);
        }
    }

    private record WildernessExits(
            boolean pathFound,
            List<PathfinderResult.Movement> path,
            int pathTotalCostX2,
            boolean exitsFound,
            GraphVertex exit30,
            GraphVertex exit20,
            int amountExpandedVertices,
            int amountVerticesLeftInQueue
    ) {
    }
}
