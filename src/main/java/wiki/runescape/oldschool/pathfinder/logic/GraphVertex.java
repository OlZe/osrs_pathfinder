package wiki.runescape.oldschool.pathfinder.logic;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GraphVertex {
    public final List<GraphEdge> neighbors;
    public final Coordinate coordinate;

    public GraphVertex(Coordinate coordinate) {
        this.coordinate = coordinate;
        this.neighbors = new LinkedList<>();
    }

    /**
     * Adds a unidirectional edge from this Vertex to the newNeighbour
     *
     * @param newNeighbour The new neighbour
     */
    public void addEdgeTo(GraphVertex newNeighbour, byte cost, String methodOfMovement) {
        this.neighbors.add(new GraphEdge(newNeighbour, cost, methodOfMovement));
    }

    /**
     * Slow, for debugging only
     *
     * @return Representation with coordinates and in which direction walking is possible and which transports are available
     */
    @Override
    public String toString() {
        final String neighbours = this.neighbors.stream()
                .map(GraphEdge::methodOfMovement)
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
}
