package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingPriorityQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueue;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderDijkstraReverse extends Pathfinder {

    private static final String PHANTOM_EDGE_WILDY_EXIT_30_TITLE = "exit30";
    private static final String PHANTOM_EDGE_WILDY_EXIT_20_TITLE = "exit20";

    private final Map<GraphVertex, List<Teleport>> teleportsTo30Map;
    private final Map<GraphVertex, List<Teleport>> teleportsTo20Map;

    public PathfinderDijkstraReverse(final Graph graph) {
        super(graph);
        this.teleportsTo30Map = graph.teleports().stream().filter(Teleport::canTeleportUpTo30Wildy).collect(Collectors.groupingBy(Teleport::to));
        this.teleportsTo20Map = graph.teleports().stream().filter(tp -> !tp.canTeleportUpTo30Wildy()).collect(Collectors.groupingBy(Teleport::to));
    }

    @Override
    protected PartialPathfinderResult findPath(final GraphVertex start, final GraphVertex end, final HashSet<String> blacklist) {
        // If stuck in wilderness, return no path found
        final WildernessExits wildernessExits = this.findWildernessExits(start, blacklist);
        if(!wildernessExits.pathsFound()) {
            return new PartialPathfinderResult(false, null, 0, wildernessExits.amountExpandedVertices(), wildernessExits.amountVerticesLeftInQueue());
        }

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

            // Start found?
            if (currentVertex.equals(start)) {
                return new PartialPathfinderResult(
                        true,
                        this.makePath(currentEntry, wildernessExits),
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

            // If a wilderness-exit has been reached, enqueue a phantom edge from startVertex to wildernessExit
            if (currentVertex.equals(wildernessExits.exitTo30().exitVertex())) {
                openList.enqueue(new GraphEdgeImpl(start, currentVertex, wildernessExits.exitTo30().totalCostX2(), PHANTOM_EDGE_WILDY_EXIT_30_TITLE, wildernessExits.exitTo30().isWalking()), currentEntry);
            }
            if (currentVertex.equals(wildernessExits.exitTo20().exitVertex())) {
                openList.enqueue(new GraphEdgeImpl(start, currentVertex, wildernessExits.exitTo20().totalCostX2(), PHANTOM_EDGE_WILDY_EXIT_20_TITLE, wildernessExits.exitTo20().isWalking()), currentEntry);
            }

            // If a teleport goes here, enqueue a phantom edge representing the teleport originating from its corresponding wilderness exit
            final List<Teleport> teleportsUpTo30Here = this.teleportsTo30Map.get(currentVertex);
            if (teleportsUpTo30Here != null) {
                for (Teleport teleportHere : teleportsUpTo30Here) {
                    if (!blacklist.contains(teleportHere.title())) {
                        openList.enqueue(new GraphEdgeImpl(wildernessExits.exitTo30().exitVertex(), teleportHere.to(), teleportHere.costX2(), teleportHere.title(), teleportHere.isWalking()), currentEntry);
                    }
                }
            }
            final List<Teleport> teleportsUpTo20Here = this.teleportsTo20Map.get(currentVertex);
            if (teleportsUpTo20Here != null) {
                for (Teleport teleportHere : teleportsUpTo20Here) {
                    if (!blacklist.contains(teleportHere.title())) {
                        openList.enqueue(new GraphEdgeImpl(wildernessExits.exitTo20().exitVertex(), teleportHere.to(), teleportHere.costX2(), teleportHere.title(), teleportHere.isWalking()), currentEntry);
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

    private List<PathfinderResult.Movement> makePath(PathfindingQueue.Entry start, WildernessExits wildernessExits) {
        List<PathfinderResult.Movement> path = new ArrayList<>();
        PathfindingQueue.Entry current = start;

        if(current.edge().title().equals(PHANTOM_EDGE_WILDY_EXIT_30_TITLE)) {
            path.addAll(wildernessExits.exitTo30().path());
            current = current.previous();
        }
        else if(start.edge().title().equals(PHANTOM_EDGE_WILDY_EXIT_20_TITLE)) {
            path.addAll(wildernessExits.exitTo20().path());
            current = current.previous();
        }
        else {
            path.add(new PathfinderResult.Movement(start.edge().from().coordinate(), Pathfinder.MOVEMENT_START_TITLE));
        }

        while (current.previous() != null) {
            path.add(new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        return path;
    }

    private WildernessExits findWildernessExits(GraphVertex start, HashSet<String> blacklist) {
        if(start.wildernessLevel() == WildernessLevels.BELOW20) {
            return new WildernessExits(
                    true,
                    new WildernessExits.WildernessExit(List.of(new PathfinderResult.Movement(start.coordinate(), Pathfinder.MOVEMENT_START_TITLE)), start, 0, false),
                    new WildernessExits.WildernessExit(List.of(new PathfinderResult.Movement(start.coordinate(), Pathfinder.MOVEMENT_START_TITLE)), start, 0, false),
                    0,
                    0);
        }

        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueue openList = new PathfindingPriorityQueue();
        openList.enqueue(new GraphEdgeImpl(null, start, 0, Pathfinder.MOVEMENT_START_TITLE, false), null);

        WildernessExits.WildernessExit pathTo30 = null;
        WildernessExits.WildernessExit pathTo20 = null;

        while (openList.hasNext()) {
            final PathfindingQueue.Entry currentEntry = openList.dequeue();
            final GraphVertex currentVertex = currentEntry.edge().to();

            if (closedList.contains(currentVertex)) {
                continue;
            }
            closedList.add(currentVertex);

            // Exit found?
            if (currentVertex.wildernessLevel() == WildernessLevels.BELOW20) {
                // Remember path to 20 wilderness and return
                pathTo20 = new WildernessExits.WildernessExit(
                        this.backtrackWildernessExit(currentEntry),
                        currentVertex,
                        currentEntry.totalCostX2(),
                        currentEntry.edge().isWalking());

                if (pathTo30 == null) {
                    pathTo30 = pathTo20;
                }

                return new WildernessExits(
                        true,
                        pathTo30,
                        pathTo20,
                        closedList.size(),
                        openList.size());

            } else if (currentVertex.wildernessLevel() == WildernessLevels.BETWEEN20AND30) {
                if (pathTo30 == null) {
                    pathTo30 = new WildernessExits.WildernessExit(
                            this.backtrackWildernessExit(currentEntry),
                            currentVertex,
                            currentEntry.totalCostX2(),
                            currentEntry.edge().isWalking());

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

            // Add neighbours of vertex to openlist
            for (GraphEdge edge : currentVertex.edgesOut()) {
                if (!blacklist.contains(edge.title()) && !closedList.contains(edge.to())) {
                    openList.enqueue(edge, currentEntry);
                }
            }
        }

        return new WildernessExits(false, null, null, closedList.size(), openList.size());
    }

    private List<PathfinderResult.Movement> backtrackWildernessExit(PathfindingQueue.Entry exit) {
        List<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueue.Entry current = exit;
        while (current != null) {
            path.add(0, new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        return path;
    }

    private record WildernessExits(
            boolean pathsFound,
            WildernessExit exitTo30,
            WildernessExit exitTo20,
            int amountExpandedVertices,
            int amountVerticesLeftInQueue
    ) {
        private record WildernessExit(
                List<PathfinderResult.Movement> path,
                GraphVertex exitVertex,
                int totalCostX2,
                boolean isWalking
        ) {
        }
    }
}
