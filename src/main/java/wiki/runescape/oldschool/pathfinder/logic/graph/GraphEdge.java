package wiki.runescape.oldschool.pathfinder.logic.graph;

public interface GraphEdge {
    GraphVertex from();

    GraphVertex to();

    int costX2();

    String title();

    boolean isWalking();
}

