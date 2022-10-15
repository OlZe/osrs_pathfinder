package myImplementation;

import java.util.Map;
import java.util.Set;

public record Graph(Map<Point, GraphNode> nodes, Set<Starter> starters) {

    /**
     * Represents a teleport that can be used anywhere in the game.
     * Eg: Varrock Teleport
     */
    public record Starter(Point coordinate, String title) {

    }

}
