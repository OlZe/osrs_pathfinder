package wiki.runescape.oldschool.logic;

import java.util.Map;
import java.util.Set;

public record Graph(Map<Coordinate, GraphVertex> vertices, Set<Teleport> teleports) {

    /**
     * Empty for faster debugging because IntelliJ would try to represent this huge object as a string and slow down
     * @return ""
     */
    @Override
    public String toString() {
        return "";
    }

    public boolean isWalkable(final Coordinate coordinate) {
        return this.vertices.get(coordinate) != null;
    }

}