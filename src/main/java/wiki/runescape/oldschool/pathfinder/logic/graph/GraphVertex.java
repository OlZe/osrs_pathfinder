package wiki.runescape.oldschool.pathfinder.logic.graph;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

import java.util.List;
import java.util.stream.Collectors;

public final class GraphVertex {
    private final Coordinate coordinate;
    private final List<GraphEdge> edgesOut;
    private final List<GraphEdge> edgesIn;
    private final WildernessLevels wildernessLevel;
    private final int hashCode;

    public GraphVertex(
            Coordinate coordinate,
            List<GraphEdge> edgesOut,
            List<GraphEdge> edgesIn,
            WildernessLevels wildernessLevel) {
        this.coordinate = coordinate;
        this.edgesOut = edgesOut;
        this.edgesIn = edgesIn;
        this.wildernessLevel = wildernessLevel;
        this.hashCode = System.identityHashCode(this);
    }


    /**
     * Adds a unidirectional edge from this Vertex to the newNeighbour
     *
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

    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public Coordinate coordinate() {
        return coordinate;
    }

    public List<GraphEdge> edgesOut() {
        return edgesOut;
    }

    public List<GraphEdge> edgesIn() {
        return edgesIn;
    }

    public WildernessLevels wildernessLevel() {
        return wildernessLevel;
    }

}
