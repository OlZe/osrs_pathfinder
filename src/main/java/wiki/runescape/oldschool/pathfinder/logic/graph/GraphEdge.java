package wiki.runescape.oldschool.pathfinder.logic.graph;

public interface GraphEdge {
    GraphVertex from();

    GraphVertex to();

    float cost();

    String title();

    boolean isWalking();
}

