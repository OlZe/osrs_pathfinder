package myImplementation;

import java.util.Arrays;
import java.util.List;

public class GraphNode {
    private final List<GraphNode> neighbors;
    private final Point coordinate;

    public GraphNode(Point coordinate, GraphNode... neighbors) {
        this.coordinate = coordinate;
        this.neighbors = Arrays.stream(neighbors).toList();
    }
}
