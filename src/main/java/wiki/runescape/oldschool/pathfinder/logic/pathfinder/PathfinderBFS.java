package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueueUnweighted;

import java.util.*;

public class PathfinderBFS extends PathfinderUnweighted {

    private final Collection<Teleport> teleportsTo20Wildy;
    private final Collection<Teleport> teleports20To30Wildy;

    public PathfinderBFS(final Graph unweightedGraph) {
        super(unweightedGraph);
        this.teleportsTo20Wildy = new ArrayList<>();
        this.teleports20To30Wildy = new ArrayList<>();
        unweightedGraph.teleports().forEach(tp -> {
            if(tp.canTeleportUpTo30Wildy()) {
                this.teleports20To30Wildy.add(tp);
            } else {
                this.teleportsTo20Wildy.add(tp);
            }
        });
    }

    @Override
    protected PartialPathfinderResult findPath(final GraphVertexReal start, final GraphVertexReal end, final HashSet<String> blacklist) {
        final Set<GraphVertex> closedList = new HashSet<>();
        final PathfindingQueueUnweighted openList = new PathfindingQueueUnweighted();
        openList.enqueue(start, null, PathfinderResult.MOVEMENT_START_TITLE, false);

        boolean addedTeleports20To30Wildy = false;
        boolean addedTeleportsTo20Wildy = false;

        while(openList.hasNext()) {
            final PathfindingQueueUnweighted.Entry currentEntry = openList.dequeue();
            final GraphVertex currentVertex = currentEntry.vertex();

            if(closedList.contains(currentVertex)) {
                continue;
            }
            closedList.add(currentVertex);

            // Goal found?
            if (currentVertex.equals(end)) {
                return new PartialPathfinderResult(
                        true,
                        this.backtrack(currentEntry),
                        currentEntry.totalCostX2(),
                        closedList.size(),
                        openList.size());
            }

            if(currentVertex instanceof final GraphVertexReal currentVertexReal) {

                // Add neighbours of vertex to openList
                for (GraphEdge edgeOut : currentVertexReal.edgesOut()) {
                    if(!blacklist.contains(edgeOut.title()) && !closedList.contains(edgeOut.to())) {
                        openList.enqueue(edgeOut.to(), currentEntry, edgeOut.title(), edgeOut.isWalking());
                    }
                }

                // If teleports haven't been added, add them to openList, depending on wildy level
                final boolean addTeleports20To30Wildy = !addedTeleports20To30Wildy && !(currentVertexReal.wildernessLevel().equals(WildernessLevels.ABOVE30));
                if (addTeleports20To30Wildy) {
                    for (Teleport teleport : this.teleports20To30Wildy) {
                        if (!blacklist.contains(teleport.title()) && !closedList.contains(teleport.to())) {
                            openList.enqueue(teleport.to(), currentEntry, teleport.title(), false);
                        }
                    }
                    addedTeleports20To30Wildy = true;
                }
                final boolean addTeleportsTo20Wildy = !addedTeleportsTo20Wildy && currentVertexReal.wildernessLevel().equals(WildernessLevels.BELOW20);
                if (addTeleportsTo20Wildy) {
                    for (Teleport teleport : this.teleportsTo20Wildy) {
                        if (!blacklist.contains(teleport.title()) && !closedList.contains(teleport.to())) {
                            openList.enqueue(teleport.to(), currentEntry, teleport.title(), false);
                        }
                    }
                    addedTeleportsTo20Wildy = true;
                }
            }
            else {
                // currentVertex is instance of GraphVertexPhantom
                // Add successor to openList
                final GraphVertexPhantom currentVertexPhantom = (GraphVertexPhantom) currentVertex;
                if(!closedList.contains(currentVertexPhantom.next)) {
                    openList.enqueue(currentVertexPhantom.next, currentEntry, currentEntry.edgeTitle(), currentEntry.edgeIsWalking());
                }
            }
        }

        // No path found
        return new PartialPathfinderResult(false, null, 0, closedList.size(), openList.size());
    }

    private List<PathfinderResult.Movement> backtrack(final PathfindingQueueUnweighted.Entry end) {
        LinkedList<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueueUnweighted.Entry current = end;
        while(current !=  null) {
            if(current.vertex() instanceof final GraphVertexReal currentVertexReal) {
                path.addFirst(new PathfinderResult.Movement(currentVertexReal.coordinate(), current.edgeTitle()));
            }
            current = current.previous();
        }

        return path;
    }

}
