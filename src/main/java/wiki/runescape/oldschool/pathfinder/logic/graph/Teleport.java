package wiki.runescape.oldschool.pathfinder.logic.graph;

public record Teleport(GraphVertex to,
                       String title,
                       int costX2,
                       boolean canTeleportUpTo30Wildy)
        implements GraphEdge {

    @Override
    public GraphVertex from() {
        return null;
    }

    @Override
    public boolean isWalking() {
        return false;
    }
}
