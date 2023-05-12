package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.graph.Graph;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphVertex;
import wiki.runescape.oldschool.pathfinder.logic.pathfinder.PathfinderResult.Movement;

import java.util.HashSet;
import java.util.List;

public abstract class Pathfinder {

    public static final String MOVEMENT_START_TITLE = "start";

    final protected Graph graph;

    protected Pathfinder(final Graph graph) {
        this.graph = graph;
    }

    public PathfinderResult findPath(Coordinate start, Coordinate end, HashSet<String> blacklist) {
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

        // Remove the 2x factor out of the total cost to return the real total cost
        // If the total cost is not evenly divisible by 2 then the path ended on a walking step (cost: half) and needs to be rounded up
        final int totalCost = (result.totalCostX2() % 2 != 0) ? ((result.totalCostX2() + 1) / 2)  : (result.totalCostX2() / 2);

        return new PathfinderResult(
                result.pathFound(),
                result.path(),
                totalCost,
                System.currentTimeMillis() - startTime,
                result.amountExpandedVertices(),
                result.amountVerticesLeftInQueue(),
                result.pathFound() ? null : ("Could not find a path from start " + start + " to end " + end + ".")
        );
    }


    protected abstract PartialPathfinderResult findPath(GraphVertex start, GraphVertex end, HashSet<String> blacklist);

    protected record PartialPathfinderResult(boolean pathFound,
                                             List<Movement> path,
                                             int totalCostX2,
                                             int amountExpandedVertices,
                                             int amountVerticesLeftInQueue) {
    }

}
