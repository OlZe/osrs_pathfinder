package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueue;

import java.util.*;

public class PathfinderDijkstraForwardReverse extends Pathfinder {
    private final Collection<Teleport> teleports20To30Wildy;
    private final Collection<Teleport> teleportsTo20Wildy;
    private final Class<? extends PathfindingQueue> queueClass;

    public PathfinderDijkstraForwardReverse(final Graph graph, final Class<? extends PathfindingQueue> queueClass) {
        super(graph);
        this.queueClass = queueClass;
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
    protected PartialPathfinderResult findPath(final GraphVertex start, final GraphVertex end, final HashSet<String> blacklist) {
        // Init forward data structures
        final HashMap<GraphVertex, PathfindingQueue.Entry> closedListForward = new HashMap<>();
        final PathfindingQueue openListForward = this.instantiatePathfindingQueue();
        openListForward.enqueue(new GraphEdgeImpl(null, start, 0, Pathfinder.MOVEMENT_START_TITLE, false), null);

        // Init backward data structures
        final HashMap<GraphVertex, PathfindingQueue.Entry> closedListReverse = new HashMap<>();
        final PathfindingQueue openListReverse = this.instantiatePathfindingQueue();
        openListReverse.enqueue(new GraphEdgeImpl(end, null, 0, "end", false), null);

        boolean addedTeleports20To30Wildy = false;
        boolean addedTeleportsTo20Wildy = false;
        boolean nextIterationForward = true;

        while (openListForward.hasNext() || openListReverse.hasNext()) {
            final boolean processForward = (nextIterationForward && openListForward.hasNext()) || !openListReverse.hasNext();
            if (processForward) {
                // Process forward dijkstra step
                final PathfindingQueue.Entry currentEntry = openListForward.dequeue();
                final GraphVertex currentVertex = currentEntry.edge().to();
                if (closedListForward.containsKey(currentVertex)) {
                    continue;
                }
                closedListForward.put(currentVertex, currentEntry);

                // Meet in the middle?
                final PathfindingQueue.Entry meetWithReverse = closedListReverse.get(currentVertex);
                if (meetWithReverse != null) {
                    return new PartialPathfinderResult(
                            true,
                            this.makePath(currentEntry, meetWithReverse),
                            currentEntry.totalCostX2() + meetWithReverse.totalCostX2(),
                            closedListForward.size() + closedListReverse.size(),
                            openListForward.size() + openListReverse.size());
                }

                // Add forward neighbours of currentVertex to openList
                for (GraphEdge edge : currentVertex.edgesOut()) {
                    if (!blacklist.contains(edge.title()) && !closedListForward.containsKey(edge.to())) {
                        openListForward.enqueue(edge, currentEntry);
                    }
                }

                // If teleports haven't been added, add them to openListForward, depending on wildy level
                final boolean addTeleports20To30Wildy = !addedTeleports20To30Wildy && !(currentVertex.wildernessLevel().equals(WildernessLevels.ABOVE30));
                if (addTeleports20To30Wildy) {
                    for (Teleport teleport : this.teleports20To30Wildy) {
                        if (!blacklist.contains(teleport.title()) && !closedListForward.containsKey(teleport.to())) {
                            openListForward.enqueue(teleport, currentEntry);
                        }
                    }
                    addedTeleports20To30Wildy = true;
                }
                final boolean addTeleportsTo20Wildy = !addedTeleportsTo20Wildy && currentVertex.wildernessLevel().equals(WildernessLevels.BELOW20);
                if (addTeleportsTo20Wildy) {
                    for (Teleport teleport : this.teleportsTo20Wildy) {
                        if (!blacklist.contains(teleport.title()) && !closedListForward.containsKey(teleport.to())) {
                            openListForward.enqueue(teleport, currentEntry);
                        }
                    }
                    addedTeleportsTo20Wildy = true;
                }

                // Mark next iteration to process backwards
                nextIterationForward = false;
            } else {
                // Process reverse dijkstra step
                final PathfindingQueue.Entry currentEntry = openListReverse.dequeue();
                final GraphVertex currentVertex = currentEntry.edge().from();

                if (closedListReverse.containsKey(currentVertex)) {
                    continue;
                }
                closedListReverse.put(currentVertex, currentEntry);

                // Meet in the middle?
                final PathfindingQueue.Entry meetWithForward = closedListForward.get(currentVertex);
                if (meetWithForward != null) {
                    return new PartialPathfinderResult(
                            true,
                            this.makePath(meetWithForward, currentEntry),
                            currentEntry.totalCostX2() + meetWithForward.totalCostX2(),
                            closedListForward.size() + closedListReverse.size(),
                            openListForward.size() + openListReverse.size());
                }

                // Add predecessors of currentVertex to openListReverse
                for (GraphEdge edge : currentVertex.edgesIn()) {
                    if (!blacklist.contains(edge.title()) && !closedListReverse.containsKey(edge.from())) {
                        openListReverse.enqueue(edge, currentEntry);
                    }
                }

                // Mark next iteration to process forwards
                nextIterationForward = true;
            }
        }

        // No path found
        return new PartialPathfinderResult(
                false,
                null,
                0,
                closedListForward.size() + closedListReverse.size(),
                openListForward.size() + openListReverse.size());
    }

    private List<PathfinderResult.Movement> makePath(PathfindingQueue.Entry forward, PathfindingQueue.Entry reverse) {
        LinkedList<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueue.Entry current = forward;
        while (current != null) {
            path.addFirst(new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        current = reverse;
        while (current.previous() != null) {
            path.addLast(new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
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
}
