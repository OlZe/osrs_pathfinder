package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphVertex;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.PathfinderResult.Movement;

import java.util.List;
import java.util.Set;

public abstract class Pathfinder {

    public static final String MOVEMENT_START_TITLE = "start";

    final protected Graph graph;

    protected Pathfinder(final Graph graph) {
        this.graph = graph;
    }

    public PathfinderResult findPath(Coordinate start, Coordinate end, Set<String> blacklist) {
        final long startTime = System.currentTimeMillis();

        // if start == end, return
        if (start.equals(end)) {
            return new PathfinderResult(
                    true,
                    List.of(new Movement(start, MOVEMENT_START_TITLE)),
                    0,
                    System.currentTimeMillis() - startTime,
                    0,
                    0,
                    null
            );
        }


        final GraphVertex startVertex = this.graph.vertices().get(start);
        final GraphVertex endVertex = this.graph.vertices().get(end);

        // If start or end unwalkable, return
        if (startVertex == null || endVertex == null) {
            String errorMessage = "";
            if (startVertex == null && endVertex == null) {
                errorMessage = "Start coordinate " + start + " and end coordinate " + end + " are not walkable.";
            } else if (startVertex == null) {
                errorMessage = "Start coordinate " + start + " is not walkable.";
            } else {
                errorMessage += "End coordinate " + end + " is not walkable.";
            }
            return new PathfinderResult(
                    false,
                    null,
                    0,
                    System.currentTimeMillis() - startTime,
                    0,
                    0,
                    errorMessage);

        }

        // Find path
        final PartialPathfinderResult result = this.findPath(startVertex, endVertex, blacklist);
        return new PathfinderResult(
                result.pathFound(),
                result.path(),
                result.totalCost(),
                System.currentTimeMillis() - startTime,
                result.amountExpandedVertices(),
                result.amountVerticesLeftInQueue(),
                result.pathFound() ? null : ("Could not find a path from start " + start + " to end " + end + ".")
        );
    }


    protected abstract PartialPathfinderResult findPath(GraphVertex start, GraphVertex end, Set<String> blacklist);

    protected record PartialPathfinderResult(boolean pathFound,
                                             List<Movement> path,
                                             int totalCost,
                                             int amountExpandedVertices,
                                             int amountVerticesLeftInQueue) {
    }

}
