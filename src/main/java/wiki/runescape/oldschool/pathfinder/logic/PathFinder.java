package wiki.runescape.oldschool.pathfinder.logic;

import java.util.List;
import java.util.Set;

public interface PathFinder {

    Result findPath(Graph graph, Coordinate start, Coordinate end, Set<String> blacklist);


    record Result(boolean pathFound,
                  List<Movement> path,
                  int totalCost,
                  long computeTimeMs,
                  int amountExpandedVertices,
                  int amountVerticesLeftInQueue) {

        public record Movement(Coordinate destination, String methodOfMovement) {

            @Override
            public String toString() {
                return this.destination.toString() + " " + methodOfMovement;
            }
        }
    }
}
