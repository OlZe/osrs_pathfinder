package wiki.runescape.oldschool.pathfinder.logic.graph;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.*;

public record Graph(Map<Coordinate, GraphVertex> vertices,
                    Collection<Teleport> teleports) {

    /**
     * Empty for faster debugging because IntelliJ would try to represent this huge object as a string and slow down
     *
     * @return ""
     */
    @Override
    public String toString() {
        return "";
    }

    public boolean isWalkable(final Coordinate coordinate) {
        return this.vertices.get(coordinate) != null;
    }

    public Collection<String> getAllTeleportsTransports() {
        final Collection<String> allTeleportsTransports = new LinkedList<>();

        this.teleports.stream()
                .map(Teleport::title)
                .forEachOrdered(allTeleportsTransports::add);

        this.vertices.values().stream()
                .flatMap(vertex -> vertex.edgesOut().stream())
                .filter(edge -> !edge.isWalking())
                .map(GraphEdge::title)
                .distinct()
                .forEachOrdered(allTeleportsTransports::add);

        return allTeleportsTransports;
    }

}
