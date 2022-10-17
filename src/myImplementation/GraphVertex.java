package myImplementation;

import java.util.LinkedList;
import java.util.List;

public class GraphVertex {
    public final List<GraphEdge> neighbors;
    public final Coordinate coordinate;

    public GraphVertex(Coordinate coordinate) {
        this.coordinate = coordinate;
        this.neighbors = new LinkedList<>();
    }

    /**
     * Adds a unidirectional edge from this Vertex to the newNeighbour
     * @param newNeighbour The new neighbour
     */
    public void addEdgeTo(GraphVertex newNeighbour, String methodOfMovement) {
        this.neighbors.add(new GraphEdge(newNeighbour, methodOfMovement));
    }

    /**
     * Slow, for debugging only
     * @return Representation with coordinates and in which direction walking is possible
     */
    @Override
    public String toString() {
        final boolean N = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveNorth().equals(graphVertex.to().coordinate));
        final boolean E = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveEast().equals(graphVertex.to().coordinate));
        final boolean S = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveSouth().equals(graphVertex.to().coordinate));
        final boolean W = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveWest().equals(graphVertex.to().coordinate));
        final boolean NE = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveNorth().moveEast().equals(graphVertex.to().coordinate));
        final boolean NW = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveNorth().moveWest().equals(graphVertex.to().coordinate));
        final boolean SE = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveSouth().moveEast().equals(graphVertex.to().coordinate));
        final boolean SW = this.neighbors.stream().anyMatch(graphVertex -> coordinate.moveSouth().moveWest().equals(graphVertex.to().coordinate));

        return "[" + this.coordinate.toString() + "->" +
                (N ? "N," : "") +
                (NE ? "NE," : "") +
                (E ? "E," : "") +
                (SE ? "SE," : "") +
                (S ? "S," : "") +
                (SW ? "SW," : "") +
                (W ? "W," : "") +
                (NW ? "NW," : "") +
                "]";
    }
}
