package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

public record GraphEdge(
        GraphVertex from,
        GraphVertex to,
        String title,
        boolean isWalking,

        GraphVertexReal realFrom,
        GraphVertexReal realTo
        ) {
}
