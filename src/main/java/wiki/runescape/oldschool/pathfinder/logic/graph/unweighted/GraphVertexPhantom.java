package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

public class GraphVertexPhantom implements GraphVertex {
    // Not record class because instantiation records with cyclic references is impossible
    public GraphVertex before;
    public GraphVertex next;
}
