package myImplementation;

import java.util.LinkedList;
import java.util.List;

public class GraphNode {
    public final List<GraphNode> neighbors;
    public final Point coordinate;

    public GraphNode(Point coordinate) {
        this.coordinate = coordinate;
        this.neighbors = new LinkedList<>();
    }

    /**
     * Adds newNeighbour as a neighbour. Bidirectional
     * @param newNeighbour The new neighbour
     */
    public void linkBidirectional(GraphNode newNeighbour) {
        this.neighbors.add(newNeighbour);
        newNeighbour.neighbors.add(this);
    }

    /**
     * Adds newNeighbour as a neighbour. Unidirectional
     * @param newNeighbour The new neighbour
     */
    public void linkTo(GraphNode newNeighbour) {
        this.neighbors.add(newNeighbour);
    }

    /**
     * Slow, for debugging only
     * @return Representation with coordinates and in which direction walking is possible
     */
    @Override
    public String toString() {
        final boolean N = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveNorth().equals(graphNode.coordinate));
        final boolean E = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveEast().equals(graphNode.coordinate));
        final boolean S = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveSouth().equals(graphNode.coordinate));
        final boolean W = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveWest().equals(graphNode.coordinate));
        final boolean NE = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveNorth().moveEast().equals(graphNode.coordinate));
        final boolean NW = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveNorth().moveWest().equals(graphNode.coordinate));
        final boolean SE = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveSouth().moveEast().equals(graphNode.coordinate));
        final boolean SW = this.neighbors.stream().anyMatch(graphNode -> coordinate.moveSouth().moveWest().equals(graphNode.coordinate));

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
