package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.Collection;
import java.util.Map;

public record Graph(
        Map<Coordinate, GraphVertexReal> vertices,
        Collection<Teleport> teleports
) {

    @Override
    public String toString() {
        return "Unweighted Graph";
    }
}
