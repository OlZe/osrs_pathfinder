package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueueUnweighted;

import java.util.*;

/**
 * WARNING: not optimal if the backwards search area includes multiple teleport destinations
 * by the time the forward search can process the teleports.
 */
public class PathfinderBfsMeetInMiddle extends PathfinderUnweighted {
    private final Collection<Teleport> teleports20To30Wildy;
    private final Collection<Teleport> teleportsTo20Wildy;

    public PathfinderBfsMeetInMiddle(final Graph graph) {
        super(graph);
        this.teleports20To30Wildy = new ArrayList<>();
        this.teleportsTo20Wildy = new ArrayList<>();
        graph.teleports().forEach(teleport -> {
            if (teleport.canTeleportUpTo30Wildy()) {
                this.teleports20To30Wildy.add(teleport);
            } else {
                this.teleportsTo20Wildy.add(teleport);
            }
        });
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


        boolean addedTeleports20To30Wildy = false;
        boolean addedTeleportsTo20Wildy = false;

        while (openListForwards.hasNext() || openListBackwards.hasNext()) {
            final boolean searchForward = (searchRadiusForwards <= searchRadiusBackwards) || !openListBackwards.hasNext();
            if (searchForward) {
                // Advance searchRadiusForwards by 1 step
                while(openListForwards.hasNext() && openListForwards.peek().totalCostX2() == searchRadiusForwards) {
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

                    if(currentEntry.vertex() instanceof final GraphVertexReal currentVertexReal) {
                        // Add forward neighbours to openList
                        for (GraphEdge edgeOut : currentVertexReal.edgesOut()) {
                            if (!blacklist.contains(edgeOut.title()) && !closedListForwards.containsKey(edgeOut.realTo())) {
                                openListForwards.enqueue(edgeOut.to(), currentEntry, edgeOut.title(), edgeOut.isWalking());
                            }
                        }

                        // If teleports haven't been added, add them to openListForwards, depending on wildy level
                        final boolean addTeleports20To30Wildy = !addedTeleports20To30Wildy && !(currentVertexReal.wildernessLevel().equals(WildernessLevels.ABOVE30));
                        if (addTeleports20To30Wildy) {
                            for (Teleport teleport : this.teleports20To30Wildy) {
                                if (!blacklist.contains(teleport.title()) && !closedListForwards.containsKey(teleport.realTo())) {
                                    openListForwards.enqueue(teleport.to(), currentEntry, teleport.title(), false);
                                }
                            }
                            addedTeleports20To30Wildy = true;
                        }
                        final boolean addTeleportsTo20Wildy = !addedTeleportsTo20Wildy && currentVertexReal.wildernessLevel().equals(WildernessLevels.BELOW20);
                        if (addTeleportsTo20Wildy) {
                            for (Teleport teleport : this.teleportsTo20Wildy) {
                                if (!blacklist.contains(teleport.title()) && !closedListForwards.containsKey(teleport.realTo())) {
                                    openListForwards.enqueue(teleport.to(), currentEntry, teleport.title(), false);
                                }
                            }
                            addedTeleportsTo20Wildy = true;
                        }
                    } else {
                        // currentVertex is instance of GraphVertexPhantom
                        // Add successor to openList
                        final GraphVertexPhantom currentVertexPhantom = (GraphVertexPhantom) currentEntry.vertex();
                        if(!closedListForwards.containsKey(currentVertexPhantom.toReal)) {
                            openListForwards.enqueue(currentVertexPhantom.to, currentEntry, currentEntry.edgeTitle(), false);
                        }
                    }
                }
                searchRadiusForwards++;
            } else {
                // Advance searchRadiusBackwards by 1 step
                while(openListBackwards.hasNext() && openListBackwards.peek().totalCostX2() == searchRadiusBackwards) {
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

                    if(currentEntry.vertex() instanceof final GraphVertexReal currentVertexReal) {
                        // Add predecessors to openListBackwards
                        for (GraphEdge edgeIn : currentVertexReal.edgesIn()) {
                            if (!blacklist.contains(edgeIn.title()) && !closedListBackwards.containsKey(edgeIn.realFrom())) {
                                openListBackwards.enqueue(edgeIn.from(), currentEntry, edgeIn.title(), edgeIn.isWalking());
                            }
                        }
                    }
                    else {
                        // vertex is instance of GraphVertexPhantom
                        final GraphVertexPhantom currentVertexPhantom = (GraphVertexPhantom) currentEntry.vertex();
                        if(!closedListBackwards.containsKey(currentVertexPhantom.fromReal)) {
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
            if(current.vertex() instanceof final GraphVertexReal currentVertex) {
                path.addFirst(new PathfinderResult.Movement(currentVertex.coordinate(), current.edgeTitle()));
            }
            current = current.previous();
        }

        if(backwards.previous() == null) {
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
}
