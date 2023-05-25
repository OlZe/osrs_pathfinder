package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;
import wiki.runescape.oldschool.pathfinder.logic.WildernessLevels;

import java.util.List;
import java.util.Objects;

public record GraphVertexReal(
        Coordinate coordinate,
        List<GraphEdge> edgesOut,
        List<GraphEdge> edgesIn,
        WildernessLevels wildernessLevel
) implements GraphVertex {

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this);
    }
}