package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueueUnweighted;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderBfsMeetAtTeleport extends PathfinderUnweighted {

    private final Map<GraphVertexReal, List<Teleport>> teleportsTo30Map;
    private final Map<GraphVertexReal, List<Teleport>> teleportsTo20Map;

    public PathfinderBfsMeetAtTeleport(final Graph unweightedGraph) {
        super(unweightedGraph);
        // Create copies as they will be modified by the search algorithm
        final Collection<Teleport> teleportCopies = this.copyTeleports(unweightedGraph.teleports());
        teleportsTo20Map = teleportCopies.stream().filter(tp -> !tp.canTeleportUpTo30Wildy()).collect(Collectors.groupingBy(Teleport::realTo));
        teleportsTo30Map = teleportCopies.stream().filter(Teleport::canTeleportUpTo30Wildy).collect(Collectors.groupingBy(Teleport::realTo));
    }

    @Override
    protected PartialPathfinderResult findPath(final GraphVertexReal start, final GraphVertexReal end, final HashSet<String> blacklist) {
        // Init forward data structures
        final HashMap<GraphVertex, PathfindingQueueUnweighted.Entry> closedListForwards = new HashMap<>();
        final PathfindingQueueUnweighted openListForwards = new PathfindingQueueUnweighted();
        openListForwards.enqueue(start, null, PathfinderResult.MOVEMENT_START_TITLE, false);

        GraphVertexReal closestVertexBelow20 = null;
        GraphVertexReal closestVertexBelow30 = null;
        int closestVertexBelow20Distance = -1;

        // Search forward until wilderness exit is found, then finish off all vertices with their distance <= (distance of exit)
        while (openListForwards.hasNext() && (closestVertexBelow20 == null || openListForwards.peek().totalCostX2() <= closestVertexBelow20Distance)) {
            final PathfindingQueueUnweighted.Entry currentEntry = openListForwards.dequeue();
            if (closedListForwards.containsKey(currentEntry.vertex())) {
                continue;
            }
            closedListForwards.put(currentEntry.vertex(), currentEntry);

            if (currentEntry.vertex() instanceof final GraphVertexPhantom currentVertex) {
                // Add successor to openListForwards
                if (!closedListForwards.containsKey(currentVertex.toReal)) {
                    openListForwards.enqueue(currentVertex.to, currentEntry, currentEntry.edgeTitle(), false);
                }
            } else {
                final GraphVertexReal currentVertex = (GraphVertexReal) currentEntry.vertex();

                // Goal found?
                if (currentVertex.equals(end)) {
                    return new PartialPathfinderResult(
                            true,
                            this.makePath(currentEntry, null),
                            currentEntry.totalCostX2(),
                            closedListForwards.size(),
                            openListForwards.size());
                }

                // Lvl 20 exit found?
                if (currentVertex.wildernessLevel() == WildernessLevels.BELOW20) {
                    if(closestVertexBelow20 == null) {
                        closestVertexBelow20 = currentVertex;
                        closestVertexBelow20Distance = currentEntry.totalCostX2();

                        // Set teleports to originate from lvl 20 exit
                        for (List<Teleport> teleports : teleportsTo20Map.values()) {
                            for (Teleport teleport : teleports) {
                                this.setTeleportOrigin(teleport, closestVertexBelow20);
                            }
                        }

                        // If no closestVertexBelow30 has been found yet, use closestVertexBelow20 as it is faster
                        if (closestVertexBelow30 == null) {
                            closestVertexBelow30 = closestVertexBelow20;

                            // Set teleports to originate from lvl 30 exit
                            for (List<Teleport> teleports : this.teleportsTo30Map.values()) {
                                for (Teleport teleport : teleports) {
                                    this.setTeleportOrigin(teleport, closestVertexBelow30);
                                }
                            }
                        }
                    }
                }
                else if (currentVertex.wildernessLevel() == WildernessLevels.BETWEEN20AND30) {
                    // Lvl 30 exit found?
                    if(closestVertexBelow30 == null) {
                        closestVertexBelow30 = currentVertex;


                        // Set teleports to originate from lvl 30 exit
                        for (List<Teleport> teleports : this.teleportsTo30Map.values()) {
                            for (Teleport teleport : teleports) {
                                this.setTeleportOrigin(teleport, closestVertexBelow30);
                            }
                        }

                        // Enqueue lvl 30 teleports
                        for (List<Teleport> teleports30 : this.teleportsTo30Map.values()) {
                            for (Teleport teleport30 : teleports30) {
                                if(!blacklist.contains(teleport30.title()) && !closedListForwards.containsKey(teleport30.realTo())) {
                                    openListForwards.enqueue(teleport30.to(), currentEntry, teleport30.title(), false);
                                }
                            }
                        }
                    }
                }

                // Enqueue neighbours
                for (GraphEdge edgeOut : currentVertex.edgesOut()) {
                    if(!blacklist.contains(edgeOut.title()) && !closedListForwards.containsKey(edgeOut.realTo())) {
                        openListForwards.enqueue(edgeOut.to(), currentEntry, edgeOut.title(), edgeOut.isWalking());
                    }
                }
            }
        }

        if(closestVertexBelow20 == null) {
            // Stuck in wilderness, return no path found
            return new PartialPathfinderResult(false, null, 0, closedListForwards.size(), openListForwards.size());
        }
        assert (closestVertexBelow30 != null);

        // Init backwards data structures
        final Set<GraphVertex> closedListBackwards = new HashSet<>();
        final PathfindingQueueUnweighted openListBackwards = new PathfindingQueueUnweighted();
        openListBackwards.enqueue(end, null, "end", false);

        while(openListBackwards.hasNext()) {
            final PathfindingQueueUnweighted.Entry currentEntry = openListBackwards.dequeue();
            if(closedListBackwards.contains(currentEntry.vertex())) {
                continue;
            }
            closedListBackwards.add(currentEntry.vertex());

            // Meet with forward search?
            final PathfindingQueueUnweighted.Entry meetWithForward = closedListForwards.get(currentEntry.vertex());
            if (meetWithForward != null) {
                return new PartialPathfinderResult(
                        true,
                        this.makePath(meetWithForward, currentEntry),
                        currentEntry.totalCostX2() + meetWithForward.totalCostX2(),
                        closedListForwards.size() + closedListBackwards.size(),
                        openListBackwards.size());
            }

            if(currentEntry.vertex() instanceof final GraphVertexPhantom currentVertex) {
                // Enqueue predecessor of currentVertex
                if(!closedListBackwards.contains(currentVertex.fromReal)) {
                    openListBackwards.enqueue(currentVertex.from, currentEntry, currentEntry.edgeTitle(), false);
                }
            } else {
                final GraphVertexReal currentVertex = (GraphVertexReal) currentEntry.vertex();

                // Enqueue predecessors of currentVertex
                for (GraphEdge edgeIn : currentVertex.edgesIn()) {
                    if(!blacklist.contains(edgeIn.title()) && !closedListBackwards.contains(edgeIn.realFrom())) {
                        openListBackwards.enqueue(edgeIn.from(), currentEntry, edgeIn.title(), edgeIn.isWalking());
                    }
                }

                // If a teleport goes here, enqueue a copy originating from its corresponding wilderness exit
                final List<Teleport> teleportsUpTo30Here = this.teleportsTo30Map.get(currentVertex);
                if(teleportsUpTo30Here != null && !closedListBackwards.contains(closestVertexBelow30)) {
                    for (Teleport teleportUpTo30Here : teleportsUpTo30Here) {
                        if(!blacklist.contains(teleportUpTo30Here.title())) {
                            final GraphVertexPhantom lastVertex = this.getLast(teleportUpTo30Here);
                            openListBackwards.enqueue(lastVertex, currentEntry, teleportUpTo30Here.title(), false);
                        }
                    }
                }
                final List<Teleport> teleportsUpTo20Here = this.teleportsTo20Map.get(currentVertex);
                if(teleportsUpTo20Here != null && !closedListBackwards.contains(closestVertexBelow20)) {
                    for (Teleport teleportUpTo20Here : teleportsUpTo20Here) {
                        if(!blacklist.contains(teleportUpTo20Here.title())) {
                            final GraphVertexPhantom lastVertex = this.getLast(teleportUpTo20Here);
                            openListBackwards.enqueue(lastVertex, currentEntry, teleportUpTo20Here.title(), false);
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
                closedListForwards.size() + closedListBackwards.size(),
                openListBackwards.size());
    }

    private GraphVertexPhantom getLast(final Teleport teleport) {
        GraphVertexPhantom current = teleport.to();
        while(current.to instanceof final GraphVertexPhantom currentTo) {
            current = currentTo;
        }
        return current;
    }

    private void setTeleportOrigin(Teleport teleport, GraphVertexReal newOrigin) {
        GraphVertex current = teleport.to();
        ((GraphVertexPhantom) current).from = newOrigin;
        while(current instanceof final GraphVertexPhantom currentPhantom) {
            currentPhantom.fromReal = newOrigin;
            current = currentPhantom.to;
        }
    }

    private List<PathfinderResult.Movement> makePath(PathfindingQueueUnweighted.Entry forward, PathfindingQueueUnweighted.Entry backwards) {
        LinkedList<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueueUnweighted.Entry current = forward;
        while (current != null) {
            if(current.vertex() instanceof final GraphVertexReal currentVertex) {
                path.addFirst(new PathfinderResult.Movement(currentVertex.coordinate(), current.edgeTitle()));
            }
            current = current.previous();
        }

        if(backwards == null || backwards.previous() == null) {
            return path;
        }

        PathfindingQueueUnweighted.Entry before = backwards;
        current = backwards.previous();
        while(current.vertex() instanceof GraphVertexPhantom) {
            current = current.previous();
        }

        while (current != null) {
            if(current.vertex() instanceof final GraphVertexReal currentVertex) {
                path.addLast(new PathfinderResult.Movement(currentVertex.coordinate(), before.edgeTitle()));
                before = current;
            }
            current = current.previous();
        }

        return path;
    }

    private Collection<Teleport> copyTeleports(Collection<Teleport> teleports) {
        final List<Teleport> copies = new LinkedList<>();
        for (Teleport teleport : teleports) {
            final GraphVertexPhantom firstCopy = new GraphVertexPhantom();
            firstCopy.toReal = teleport.realTo();

            GraphVertex currentOriginal = teleport.to().to;
            GraphVertexPhantom currentCopy = firstCopy;
            GraphVertexPhantom previousCopy = null;
            while(currentOriginal instanceof final GraphVertexPhantom currentOriginalPhantom) {
                previousCopy = currentCopy;
                currentCopy = new GraphVertexPhantom();

                currentCopy.from = previousCopy;
                previousCopy.to = currentCopy;
                currentCopy.toReal = teleport.realTo();

                currentOriginal = currentOriginalPhantom.to;
            }
            currentCopy.to = teleport.realTo();

            final Teleport teleportCopy = new Teleport(firstCopy, teleport.title(), teleport.canTeleportUpTo30Wildy(), teleport.realTo());
            copies.add(teleportCopy);
        }
        return copies;
    }
}
