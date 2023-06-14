package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

public class GraphVertexPhantom implements GraphVertex {
    // final fields because instantiation this class with cyclic references is impossible
    public GraphVertex from;
    public GraphVertex to;
    public GraphVertexReal fromReal;
    public GraphVertexReal toReal;

    @Override
    public String toString() {
        return "Phantom Vertex";
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
