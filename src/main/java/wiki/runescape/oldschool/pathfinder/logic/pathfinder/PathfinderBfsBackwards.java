package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueueUnweighted;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderBfsBackwards extends PathfinderUnweighted {

    private final Map<GraphVertexReal, List<Teleport>> teleportsTo30Map;
    private final Map<GraphVertexReal, List<Teleport>> teleportsTo20Map;

    public PathfinderBfsBackwards(Graph unweightedGraph) {
        super(unweightedGraph);
        teleportsTo20Map = unweightedGraph.teleports().stream().filter(tp -> !tp.canTeleportUpTo30Wildy()).collect(Collectors.groupingBy(Teleport::realTo));
        teleportsTo30Map = unweightedGraph.teleports().stream().filter(Teleport::canTeleportUpTo30Wildy).collect(Collectors.groupingBy(Teleport::realTo));
    }


    @Override
    protected PartialPathfinderResult findPath(final GraphVertexReal start, final GraphVertexReal end, final HashSet<String> blacklist) {
        final WildernessExits wildernessExits = this.findWildernessExits(start, end, blacklist);

        // If goal found by chance, return path
        if (wildernessExits.pathFound()) {
            return new PartialPathfinderResult(true, wildernessExits.path(), wildernessExits.pathTotalCostX2(), wildernessExits.amountExpandedVertices(), wildernessExits.amountVerticesLeftInQueue());
        }

        // If stuck in wilderness, return no path found
        if(!wildernessExits.exitsFound()) {
            return new PartialPathfinderResult(false, null, 0, wildernessExits.amountExpandedVertices(), wildernessExits.amountVerticesLeftInQueue());
        }

        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueueUnweighted openList = new PathfindingQueueUnweighted();
        openList.enqueue(end, null, "end", false);

        while (openList.hasNext()) {
            final PathfindingQueueUnweighted.Entry currentEntry = openList.dequeue();

            if(closedList.contains(currentEntry.vertex())) {
                continue;
            }
            closedList.add(currentEntry.vertex());

            if(currentEntry.vertex() instanceof final GraphVertexPhantom currentVertex) {
                // Add predecessors to queue
                if(!closedList.contains(currentVertex.fromReal)) {
                    openList.enqueue(currentVertex.from, currentEntry, currentEntry.edgeTitle(), false);
                }
            } else {
                final GraphVertexReal currentVertex = (GraphVertexReal) currentEntry.vertex();

                // Start found?
                if(currentVertex.equals(start)) {
                    return new PartialPathfinderResult(
                            true,
                            this.makePathBackwards(currentEntry),
                            currentEntry.totalCostX2(),
                            closedList.size() + wildernessExits.amountExpandedVertices(),
                            openList.size() + wildernessExits.amountVerticesLeftInQueue());
                }

                // Add predecessors to queue
                for (GraphEdge edgeIn : currentVertex.edgesIn()) {
                    if(!blacklist.contains(edgeIn.title()) && !closedList.contains(edgeIn.realFrom())) {
                        openList.enqueue(edgeIn.from(), currentEntry, edgeIn.title(), edgeIn.isWalking());
                    }
                }

                // If a teleport goes here, enqueue a copy originating from its corresponding wilderness exit
                final List<Teleport> teleportsUpTo30Here = this.teleportsTo30Map.get(currentVertex);
                if(teleportsUpTo30Here != null && !closedList.contains(wildernessExits.exit30())) {
                    for (Teleport teleportUpTo30Here : teleportsUpTo30Here) {
                        if(!blacklist.contains(teleportUpTo30Here.title())) {
                            final GraphVertexPhantom newTeleport = this.createTeleportWithOrigin(teleportUpTo30Here, wildernessExits.exit30());
                            openList.enqueue(newTeleport, currentEntry, teleportUpTo30Here.title(), false);
                        }
                    }
                }
                final List<Teleport> teleportsUpTo20Here = this.teleportsTo20Map.get(currentVertex);
                if(teleportsUpTo20Here != null && !closedList.contains(wildernessExits.exit20())) {
                    for (Teleport teleportUpTo20Here : teleportsUpTo20Here) {
                        if(!blacklist.contains(teleportUpTo20Here.title())) {
                            final GraphVertexPhantom newTeleport = this.createTeleportWithOrigin(teleportUpTo20Here, wildernessExits.exit20());
                            openList.enqueue(newTeleport, currentEntry, teleportUpTo20Here.title(), false);
                        }
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
                openList.size() + wildernessExits.amountExpandedVertices());
    }

    private List<PathfinderResult.Movement> makePathBackwards(final PathfindingQueueUnweighted.Entry start) {
        List<PathfinderResult.Movement> path = new ArrayList<>();
        path.add(new PathfinderResult.Movement(((GraphVertexReal) start.vertex()).coordinate(), PathfinderResult.MOVEMENT_START_TITLE));

        PathfindingQueueUnweighted.Entry before = start;
        PathfindingQueueUnweighted.Entry current = start.previous();
        while(current != null) {
            if(current.vertex() instanceof final GraphVertexReal currentVertex) {
                path.add(new PathfinderResult.Movement(currentVertex.coordinate(), before.edgeTitle()));
                before = current;
            }
            current = current.previous();
        }

        return path;
    }

    private GraphVertexPhantom createTeleportWithOrigin(Teleport teleport, GraphVertexReal newOrigin) {
        final GraphVertexPhantom first = new GraphVertexPhantom();
        first.from = newOrigin;
        first.fromReal = newOrigin;
        first.toReal = teleport.realTo();

        GraphVertex currentOriginal = teleport.to();
        GraphVertexPhantom previousCopy = first;
        GraphVertexPhantom currentCopy = null;
        while(currentOriginal instanceof final GraphVertexPhantom currentOriginalPhantom) {
            currentCopy = new GraphVertexPhantom();
            previousCopy.to = currentCopy;
            currentCopy.fromReal = newOrigin;
            currentCopy.toReal = teleport.realTo();
            currentCopy.from = previousCopy;
            previousCopy = currentCopy;
            currentOriginal = currentOriginalPhantom.to;
        }
        final GraphVertexPhantom last = currentCopy;
        last.to = teleport.realTo();

        return last;
    }

    private WildernessExits findWildernessExits(GraphVertexReal start, GraphVertexReal end, HashSet<String> blacklist) {
        if (start.wildernessLevel() == WildernessLevels.BELOW20) {
            return new WildernessExits(false, null, 0, true, start, start, 0, 0);
        }

        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueueUnweighted openList = new PathfindingQueueUnweighted();
        openList.enqueue(start, null, PathfinderResult.MOVEMENT_START_TITLE, false);

        GraphVertexReal exitBelow30 = null;
        GraphVertexReal exitBelow20 = null;

        while (openList.hasNext()) {
            final PathfindingQueueUnweighted.Entry currentEntry = openList.dequeue();

            if (closedList.contains(currentEntry.vertex())) {
                continue;
            }
            closedList.add(currentEntry.vertex());

            if ((currentEntry.vertex() instanceof final GraphVertexPhantom currentVertex)) {
                // Add neighbour to openList
                if(!closedList.contains(currentVertex.toReal)) {
                    openList.enqueue(currentVertex.to, currentEntry, currentEntry.edgeTitle(), false);
                }
            } else {
                final GraphVertexReal currentVertex = (GraphVertexReal) currentEntry.vertex();

                // Goal found?
                if (currentEntry.vertex().equals(end)) {
                    return new WildernessExits(
                            true,
                            makePathForwards(currentEntry),
                            currentEntry.totalCostX2(),
                            false,
                            null,
                            null,
                            closedList.size(),
                            openList.size());
                }

                // Lvl 20 exit found?
                if (currentVertex.wildernessLevel() == WildernessLevels.BELOW20) {
                    exitBelow20 = currentVertex;

                    // If no exit to below lvl 30 has been found yet, use the lvl 20 exit as it is faster
                    if (exitBelow30 == null) {
                        exitBelow30 = exitBelow20;
                    }
                    return new WildernessExits(false, null, 0, true, exitBelow30, exitBelow20, closedList.size(), openList.size());

                } else if (currentVertex.wildernessLevel() == WildernessLevels.BETWEEN20AND30) {
                    // Lvl 30 exit found?
                    if (exitBelow30 == null) {
                        exitBelow30 = currentVertex;

                        // Add lvl 30 wildy teleports
                        for (List<Teleport> teleportsTo30 : this.teleportsTo30Map.values()) {
                            for (Teleport teleportTo30 : teleportsTo30) {
                                if (!blacklist.contains(teleportTo30.title()) && !closedList.contains(teleportTo30.realTo())) {
                                    openList.enqueue(teleportTo30.to(), currentEntry, teleportTo30.title(), false);
                                }
                            }
                        }
                    }
                }

                // Add neighbours to openList
                for (GraphEdge edgeOut : currentVertex.edgesOut()) {
                    if(!blacklist.contains(edgeOut.title()) && !closedList.contains(edgeOut.realTo())) {
                        openList.enqueue(edgeOut.to(), currentEntry, edgeOut.title(), edgeOut.isWalking());
                    }
                }
            }
        }

        // No exit found
        return new WildernessExits(false, null, 0, false, null, null, closedList.size(), openList.size());
    }

    private List<PathfinderResult.Movement> makePathForwards(final PathfindingQueueUnweighted.Entry end) {
        LinkedList<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueueUnweighted.Entry current = end;
        while(current != null) {
            if (current.vertex() instanceof final GraphVertexReal currentVertex) {
                path.addFirst(new PathfinderResult.Movement(currentVertex.coordinate(), current.edgeTitle()));
            }
            current = current.previous();
        }

        return path;
    }


    private record WildernessExits(
            boolean pathFound,
            List<PathfinderResult.Movement> path,
            int pathTotalCostX2,
            boolean exitsFound,
            GraphVertexReal exit30,
            GraphVertexReal exit20,
            int amountExpandedVertices,
            int amountVerticesLeftInQueue
    ) {
    }
}
