package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

import java.util.List;

public final class GraphVertexReal implements GraphVertex {
    private final Coordinate coordinate;
    private final List<GraphEdge> edgesOut;
    private final List<GraphEdge> edgesIn;
    private final WildernessLevels wildernessLevel;

    private final int hashCode;

    public GraphVertexReal(
            Coordinate coordinate,
            List<GraphEdge> edgesOut,
            List<GraphEdge> edgesIn,
            WildernessLevels wildernessLevel
    ) {
        this.coordinate = coordinate;
        this.edgesOut = edgesOut;
        this.edgesIn = edgesIn;
        this.wildernessLevel = wildernessLevel;
        this.hashCode = System.identityHashCode(this);
    }

    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return coordinate().toString();
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