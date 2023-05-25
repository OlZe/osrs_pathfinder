package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

import java.util.List;

public record GraphVertexReal(
        Coordinate coordinate,
        List<GraphEdge> edgesOut,
        List<GraphEdge> edgesIn,
        WildernessLevels wildernessLevel
) implements GraphVertex {

    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}