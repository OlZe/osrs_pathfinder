package wiki.runescape.oldschool.pathfinder.logic.graph;

public record GraphEdgeImpl(GraphVertex from,
                            GraphVertex to,
                            float cost,
                            String title,
                            boolean isWalking)
        implements GraphEdge {

}
