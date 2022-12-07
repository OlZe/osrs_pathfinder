package wiki.runescape.oldschool.logic;

import java.util.List;

public record PathFinderResult(boolean pathFound, List<Movement> path, long computeTime) {

    public record Movement(Coordinate destination, String methodOfMovement) {

        @Override
        public String toString() {
            return this.destination.toString() + " " + methodOfMovement;
        }
    }
}
