package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

public record Teleport(
        GraphVertexPhantom to,
        String title,
        boolean canTeleportUpTo30Wildy
) {
}