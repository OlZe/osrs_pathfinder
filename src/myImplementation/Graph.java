package myImplementation;

import java.util.Map;
import java.util.Set;

public record Graph(Map<Coordinate, GraphVertex> vertices, Set<Starter> starters) {

    /**
     * Represents a teleport that can be used anywhere in the game.
     * Eg: Varrock Teleport
     */
    public record Starter(Coordinate coordinate, String title) {

    }

    /**
     * Empty for faster debugging because IntelliJ would try to represent this huge object as a string and slow down
     * @return ""
     */
    @Override
    public String toString() {
        return "";
    }
}
