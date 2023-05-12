package wiki.runescape.oldschool.pathfinder.logic.graph;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record GraphVertex(
        Coordinate coordinate,
        List<GraphEdge> edgesOut,
        List<GraphEdge> edgesIn,
        WildernessLevels wildernessLevel) {

    /**
     * Adds a unidirectional edge from this Vertex to the newNeighbour
     * @param newNeighbour The new neighbour
     */
    public void addEdgeTo(GraphVertex newNeighbour, int costX2, String methodOfMovement, boolean isWalking) {
        assert !newNeighbour.equals(this);
        final GraphEdge newEdge = new GraphEdgeImpl(this, newNeighbour, costX2, methodOfMovement, isWalking);
        this.edgesOut.add(newEdge);
        newNeighbour.edgesIn.add(newEdge);
    }

    /**
     * Slow, for debugging only
     *
     * @return Representation with coordinates and in which direction walking is possible and which transports are available
     */
    @Override
    public String toString() {
        final String neighbours = this.edgesOut.stream()
                .map(GraphEdge::title)
                .map(s -> switch (s) { // Shorten walking strings for better debugging
                    case GraphBuilder.WALK_NORTH -> "N";
                    case GraphBuilder.WALK_EAST -> "E";
                    case GraphBuilder.WALK_SOUTH -> "S";
                    case GraphBuilder.WALK_WEST -> "W";
                    case GraphBuilder.WALK_NORTH_EAST -> "NE";
                    case GraphBuilder.WALK_SOUTH_EAST -> "SE";
                    case GraphBuilder.WALK_SOUTH_WEST -> "SW";
                    case GraphBuilder.WALK_NORTH_WEST -> "NW";
                    default -> s;
                }).collect(Collectors.joining(","));
        return "[" + this.coordinate.toString() + "->" + neighbours + "]";
    }

    /**
     * Compares using their coordinate
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final GraphVertex that = (GraphVertex) o;
        return Objects.equals(coordinate, that.coordinate);
    }

    /**
     * Hashes this coordinate
     */
    @Override
    public int hashCode() {
        return Objects.hash(coordinate);
    }
}
