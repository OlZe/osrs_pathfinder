package myImplementation;

import java.util.List;

public record PathFinderResult(boolean pathFound, List<PathFinderResult.Movement> path, long computeTime) {

    public record Movement(Coordinate destination, String methodOfMovement) {

        @Override
        public String toString() {
            return this.destination.toString() + " " + methodOfMovement;
        }
    }
}
