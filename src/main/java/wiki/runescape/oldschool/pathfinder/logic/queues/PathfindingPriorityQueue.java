package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdgeImpl;
import wiki.runescape.oldschool.pathfinder.logic.graph.GraphVertex;

public class PathfindingPriorityQueue implements PathfindingQueue {

    private final PriorityQueueTieByTime<Entry> queue = new PriorityQueueTieByTime<>();

    public PathfindingPriorityQueue(GraphVertex startVertex) {
        this.queue.add(new Entry(new GraphEdgeImpl(null, startVertex, 0, "start", false), null, 0));
    }

    @Override
    public void enqueue(final GraphEdge edge, final Entry previous) {
        float newTotalCost = previous.totalCost() + edge.cost();

        if (!edge.isWalking() && previous.edge().isWalking()) {
            // Walking stops, round up in case path stops on a half tick
            newTotalCost = (float) Math.ceil(newTotalCost);
        }

        this.queue.add(new Entry(edge, previous, newTotalCost));
    }

    @Override
    public Entry dequeue() {
        return this.queue.remove();
    }

    @Override
    public boolean hasNext() {
        return this.queue.peek() != null;
    }

    @Override
    public int size() {
        return this.queue.size();
    }
}
