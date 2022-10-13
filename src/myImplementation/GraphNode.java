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

    public void linkBidirectional(GraphNode newNeighbour) {
        this.neighbors.add(newNeighbour);
        newNeighbour.neighbors.add(this);
    }
}
