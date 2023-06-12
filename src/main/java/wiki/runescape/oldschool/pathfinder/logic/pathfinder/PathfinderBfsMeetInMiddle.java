package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueueUnweighted;

import java.util.*;
import java.util.stream.Collectors;

public class PathfinderBfsMeetInMiddle extends PathfinderUnweighted {
    private final Map<GraphVertexReal, List<Teleport>> teleportsTo30Map;
    private final Map<GraphVertexReal, List<Teleport>> teleportsTo20Map;

    public PathfinderBfsMeetInMiddle(final Graph unweightedGraph) {
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
        int searchRadiusForwards = 0;

        // Init backward data structures
        final HashMap<GraphVertex, PathfindingQueueUnweighted.Entry> closedListBackwards = new HashMap<>();
        final PathfindingQueueUnweighted openListBackwards = new PathfindingQueueUnweighted();
        openListBackwards.enqueue(end, null, "end", false);
        int searchRadiusBackwards = 0;


        GraphVertexReal closestVertexBelow20 = null;
        GraphVertexReal closestVertexBelow30 = null;

        while (openListForwards.hasNext() || openListBackwards.hasNext()) {
            final boolean searchForward = (closestVertexBelow20 == null) // Search forward until wilderness exit
                    || (searchRadiusForwards <= searchRadiusBackwards) // Search forward until circles are equal size
                    || !openListBackwards.hasNext(); // Search forward if backwards is impossible

            if (closestVertexBelow20 == null && !openListForwards.hasNext()) {
                // Stuck in wilderness
                return new PartialPathfinderResult(false, null, 0, closedListForwards.size() + closedListBackwards.size(), openListForwards.size() + openListBackwards.size());
            }

            if (searchForward) {
                // Advance searchRadiusForwards by 1 step
                while (openListForwards.hasNext() && openListForwards.peek().totalCostX2() == searchRadiusForwards) {
                    final PathfindingQueueUnweighted.Entry currentEntry = openListForwards.dequeue();
                    if (closedListForwards.containsKey(currentEntry.vertex())) {
                        continue;
                    }
                    closedListForwards.put(currentEntry.vertex(), currentEntry);

                    // Meet in the middle?
                    final PathfindingQueueUnweighted.Entry meetWithBackwards = closedListBackwards.get(currentEntry.vertex());
                    if (meetWithBackwards != null) {
                        return new PartialPathfinderResult(
                                true,
                                this.makePath(currentEntry, meetWithBackwards),
                                currentEntry.totalCostX2() + meetWithBackwards.totalCostX2(),
                                closedListForwards.size() + closedListBackwards.size(),
                                openListForwards.size() + openListBackwards.size());
                    }

                    if (currentEntry.vertex() instanceof final GraphVertexReal currentVertexReal) {
                        // Add forward neighbours to openList
                        for (GraphEdge edgeOut : currentVertexReal.edgesOut()) {
                            if (!blacklist.contains(edgeOut.title()) && !closedListForwards.containsKey(edgeOut.realTo())) {
                                openListForwards.enqueue(edgeOut.to(), currentEntry, edgeOut.title(), edgeOut.isWalking());
                            }
                        }

                        // Lvl 20 exit found?
                        if (closestVertexBelow20 == null && currentVertexReal.wildernessLevel() == WildernessLevels.BELOW20) {
                            closestVertexBelow20 = currentVertexReal;

                            // Set teleports to originate from closestVertexBelow20
                            for (List<Teleport> teleports : teleportsTo20Map.values()) {
                                for (Teleport teleport : teleports) {
                                    this.setTeleportOrigin(teleport, closestVertexBelow20);
                                }
                            }

                            // Enqueue lvl 20 teleports
                            for (List<Teleport> teleports : teleportsTo20Map.values()) {
                                for (Teleport teleport : teleports) {
                                    if(!blacklist.contains(teleport.title()) && !closedListForwards.containsKey(teleport.realTo())) {
                                        openListForwards.enqueue(teleport.to(), currentEntry, teleport.title(), false);
                                    }
                                }
                            }
                        }
                        // Lvl 30 exit found?
                        if (closestVertexBelow30 == null && currentVertexReal.wildernessLevel() != WildernessLevels.ABOVE30) {
                            closestVertexBelow30 = currentVertexReal;

                            // Set teleports to originate from lvl 30 exit
                            for (List<Teleport> teleports : this.teleportsTo30Map.values()) {
                                for (Teleport teleport : teleports) {
                                    this.setTeleportOrigin(teleport, closestVertexBelow30);
                                }
                            }

                            // Enqueue lvl 30 teleports
                            for (List<Teleport> teleports : teleportsTo30Map.values()) {
                                for (Teleport teleport : teleports) {
                                    if(!blacklist.contains(teleport.title()) && !closedListForwards.containsKey(teleport.realTo())) {
                                        openListForwards.enqueue(teleport.to(), currentEntry, teleport.title(), false);
                                    }
                                }
                            }
                        }
                    } else {
                        // currentVertex is instance of GraphVertexPhantom
                        // Add successor to openList
                        final GraphVertexPhantom currentVertexPhantom = (GraphVertexPhantom) currentEntry.vertex();
                        if (!closedListForwards.containsKey(currentVertexPhantom.toReal)) {
                            openListForwards.enqueue(currentVertexPhantom.to, currentEntry, currentEntry.edgeTitle(), false);
                        }
                    }
                }
                searchRadiusForwards++;
            } else {
                // Advance searchRadiusBackwards by 1 step
                while (openListBackwards.hasNext() && openListBackwards.peek().totalCostX2() == searchRadiusBackwards) {
                    final PathfindingQueueUnweighted.Entry currentEntry = openListBackwards.dequeue();

                    if (closedListBackwards.containsKey(currentEntry.vertex())) {
                        continue;
                    }
                    closedListBackwards.put(currentEntry.vertex(), currentEntry);

                    // Meet in the middle?
                    final PathfindingQueueUnweighted.Entry meetWithForward = closedListForwards.get(currentEntry.vertex());
                    if (meetWithForward != null) {
                        return new PartialPathfinderResult(
                                true,
                                this.makePath(meetWithForward, currentEntry),
                                currentEntry.totalCostX2() + meetWithForward.totalCostX2(),
                                closedListForwards.size() + closedListBackwards.size(),
                                openListForwards.size() + openListBackwards.size());
                    }

                    if (currentEntry.vertex() instanceof final GraphVertexReal currentVertexReal) {
                        // Add predecessors to openListBackwards
                        for (GraphEdge edgeIn : currentVertexReal.edgesIn()) {
                            if (!blacklist.contains(edgeIn.title()) && !closedListBackwards.containsKey(edgeIn.realFrom())) {
                                openListBackwards.enqueue(edgeIn.from(), currentEntry, edgeIn.title(), edgeIn.isWalking());
                            }
                        }

                        // If a teleports go here, enqueue them
                        final List<Teleport> teleportsUpTo30Here = this.teleportsTo30Map.get(currentVertexReal);
                        if(teleportsUpTo30Here != null && !closedListBackwards.containsKey(closestVertexBelow30)) {
                            for (Teleport teleportUpTo30Here : teleportsUpTo30Here) {
                                if(!blacklist.contains(teleportUpTo30Here.title())) {
                                    final GraphVertexPhantom lastVertex = this.getLast(teleportUpTo30Here);
                                    openListBackwards.enqueue(lastVertex, currentEntry, teleportUpTo30Here.title(), false);
                                }
                            }
                        }
                        final List<Teleport> teleportsUpTo20Here = this.teleportsTo20Map.get(currentVertexReal);
                        if(teleportsUpTo20Here != null && !closedListBackwards.containsKey(closestVertexBelow20)) {
                            for (Teleport teleportUpTo20Here : teleportsUpTo20Here) {
                                if(!blacklist.contains(teleportUpTo20Here.title())) {
                                    final GraphVertexPhantom lastVertex = this.getLast(teleportUpTo20Here);
                                    openListBackwards.enqueue(lastVertex, currentEntry, teleportUpTo20Here.title(), false);
                                }
                            }
                        }
                    } else {
                        // vertex is instance of GraphVertexPhantom
                        final GraphVertexPhantom currentVertexPhantom = (GraphVertexPhantom) currentEntry.vertex();
                        if (!closedListBackwards.containsKey(currentVertexPhantom.fromReal)) {
                            openListBackwards.enqueue(currentVertexPhantom.from, currentEntry, currentEntry.edgeTitle(), false);
                        }
                    }
                }
                searchRadiusBackwards++;
            }
        }

        // No path found
        return new PartialPathfinderResult(
                false,
                null,
                0,
                closedListForwards.size() + closedListBackwards.size(),
                openListForwards.size() + openListBackwards.size());
    }

    private List<PathfinderResult.Movement> makePath(PathfindingQueueUnweighted.Entry forward, PathfindingQueueUnweighted.Entry backwards) {
        LinkedList<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueueUnweighted.Entry current = forward;
        while (current != null) {
            if (current.vertex() instanceof final GraphVertexReal currentVertex) {
                path.addFirst(new PathfinderResult.Movement(currentVertex.coordinate(), current.edgeTitle()));
            }
            current = current.previous();
        }

        if (backwards.previous() == null) {
            return path;
        }

        PathfindingQueueUnweighted.Entry before = backwards;
        current = backwards.previous();
        while (current.vertex() instanceof GraphVertexPhantom) {
            current = current.previous();
        }

        while (current != null) {
            if (current.vertex() instanceof final GraphVertexReal currentVertex) {
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
            while (currentOriginal instanceof final GraphVertexPhantom currentOriginalPhantom) {
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

    private void setTeleportOrigin(Teleport teleport, GraphVertexReal newOrigin) {
        GraphVertex current = teleport.to();
        ((GraphVertexPhantom) current).from = newOrigin;
        while (current instanceof final GraphVertexPhantom currentPhantom) {
            currentPhantom.fromReal = newOrigin;
            current = currentPhantom.to;
        }
    }

    private GraphVertexPhantom getLast(final Teleport teleport) {
        GraphVertexPhantom current = teleport.to();
        while (current.to instanceof final GraphVertexPhantom currentTo) {
            current = currentTo;
        }
        return current;
    }
}
