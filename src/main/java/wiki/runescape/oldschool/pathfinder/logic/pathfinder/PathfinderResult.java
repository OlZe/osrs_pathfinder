package wiki.runescape.oldschool.pathfinder.logic.pathfinder;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.List;

public record PathfinderResult(boolean pathFound,
                               List<Movement> path,
                               int totalCost,
                               long computeTimeMs,
                               int amountExpandedVertices,
                               int amountVerticesLeftInQueue,
                               String reasonForNoPathFound) {

    public static final String MOVEMENT_START_TITLE = "start";

    public record Movement(Coordinate destination,
                           String methodOfMovement) {
    }
}
