package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import java.util.Objects;

public class GraphVertexPhantom implements GraphVertex {
    // Not record class because instantiation records with cyclic references is impossible
    public GraphVertex before;
    public GraphVertex next;

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this);
    }
}
