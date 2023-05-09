package wiki.runescape.oldschool.pathfinder.logic;

public record GraphEdgeImpl(GraphVertex from,
                            GraphVertex to,
                            float cost,
                            String title,
                            boolean isWalking)
        implements GraphEdge {

}
