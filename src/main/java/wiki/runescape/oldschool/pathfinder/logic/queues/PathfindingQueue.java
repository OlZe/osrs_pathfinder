package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge;

public interface PathfindingQueue {

    void enqueue(GraphEdge edge, Entry previous);

    Entry dequeue();

    boolean hasNext();

    int size();

    record Entry(GraphEdge edge,
                 Entry previous,
                 float totalCost)
            implements Comparable<Entry> {

        @Override
        public int compareTo(final Entry o) {
            return Float.compare(this.totalCost, o.totalCost);
        }
    }
}
