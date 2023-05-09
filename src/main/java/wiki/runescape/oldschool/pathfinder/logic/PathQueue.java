package wiki.runescape.oldschool.pathfinder.logic;

public interface PathQueue {

    void enqueue(GraphEdge edge, Entry previous);

    Entry dequeue();


    interface Entry {
        GraphEdge edge();
        Entry previous();
        float totalCost();
    }
}
