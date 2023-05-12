package wiki.runescape.oldschool.pathfinder.logic.graph;

public record GraphEdgeImpl(GraphVertex from,
                            GraphVertex to,
                            int costX2,
                            String title,
                            boolean isWalking)
        implements GraphEdge {

}
