package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

public class GraphVertexPhantom implements GraphVertex {
    // Not record class because instantiation records with cyclic references is impossible
    public GraphVertex from;
    public GraphVertex to;
    public GraphVertexReal fromReal;
    public GraphVertexReal toReal;

    @Override
    public String toString() {
        return "Phantom Vertex";
    }
}
