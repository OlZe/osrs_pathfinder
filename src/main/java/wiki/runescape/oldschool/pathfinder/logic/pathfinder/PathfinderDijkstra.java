package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;
import wiki.runescape.oldschool.pathfinder.logic.graph.*;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingPriorityQueue;
import wiki.runescape.oldschool.pathfinder.logic.queues.PathfindingQueue;

import java.util.*;

public class PathfinderDijkstra extends Pathfinder {
    private final Collection<Teleport> teleports20To30Wildy;
    private final Collection<Teleport> teleportsTo20Wildy;

    public PathfinderDijkstra(Graph graph) {
        super(graph);
        this.teleports20To30Wildy = new ArrayList<>();
        this.teleportsTo20Wildy = new ArrayList<>();
        graph.teleports().forEach(teleport -> {
            if(teleport.canTeleportUpTo30Wildy()) {
                this.teleports20To30Wildy.add(teleport);
            }
            else {
                this.teleportsTo20Wildy.add(teleport);
            }
        });
    }

    @Override
    public PathfinderResult findPath(Coordinate start, Coordinate end, Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        final GraphVertex startVertex = this.graph.vertices().get(start);
        assert (startVertex != null);
        assert (this.graph.vertices().get(end) != null);

        // if start == end, return
        if (start.equals(end)) {
            return new PathfinderResult(
                    true,
                    List.of(new PathfinderResult.Movement(start, "start")),
                    0,
                    System.currentTimeMillis() - startTime,
                    0,
                    0);
        }

        // Init openList and closedList
        final Set<Coordinate> closedList = new HashSet<>();
        final PathfindingQueue openList = new PathfindingPriorityQueue(startVertex);

        boolean addedTeleports20To30Wildy = false;
        boolean addedTeleportsTo20Wildy = false;
        while (openList.hasNext()) {
            final PathfindingQueue.Entry currentEntry = openList.dequeue();
            final GraphVertex currentVertex = currentEntry.edge().to();

            if (closedList.contains(currentVertex.coordinate())) {
                continue;
            }
            closedList.add(currentVertex.coordinate());

            // Goal found?
            if (currentVertex.coordinate().equals(end)) {
                return new PathfinderResult(
                        true,
                        this.backtrack(currentEntry),
                        (int) currentEntry.totalCost(),
                        System.currentTimeMillis() - startTime,
                        closedList.size(),
                        openList.size());
            }

            // Add neighbours of vertex to openList
            for (GraphEdge edge : currentVertex.edgesOut()) {
                if (!blacklist.contains(edge.title())) {
                    openList.enqueue(edge, currentEntry);
                }
            }

            // If teleports haven't been added, add them to openList, depending on wildy level
            final boolean addTeleports20To30Wildy = !addedTeleports20To30Wildy && !(currentVertex.wildernessLevel().equals(WildernessLevels.ABOVE30));
            if(addTeleports20To30Wildy) {
                for(Teleport teleport : this.teleports20To30Wildy) {
                    if(!blacklist.contains(teleport.title())) {
                        openList.enqueue(teleport, currentEntry);
                    }
                }
                addedTeleports20To30Wildy = true;
            }
            final boolean addTeleportsTo20Wildy = !addedTeleportsTo20Wildy && currentVertex.wildernessLevel().equals(WildernessLevels.BELOW20);
            if(addTeleportsTo20Wildy) {
                for (Teleport teleport : this.teleportsTo20Wildy) {
                    if(!blacklist.contains(teleport.title())) {
                        openList.enqueue(teleport, currentEntry);
                    }
                }
                addedTeleportsTo20Wildy = true;
            }
        }

        // No Path found
        return new PathfinderResult(false, null, 0, System.currentTimeMillis() - startTime, closedList.size(), openList.size());
    }

    private List<PathfinderResult.Movement> backtrack(PathfindingQueue.Entry end) {
        List<PathfinderResult.Movement> path = new LinkedList<>();

        PathfindingQueue.Entry current = end;
        while (current != null) {
            path.add(0, new PathfinderResult.Movement(current.edge().to().coordinate(), current.edge().title()));
            current = current.previous();
        }

        return path;
    }
}
