package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge;

public class PathfindingPriorityQueue implements PathfindingQueue {

    private final PriorityQueueTieByTime<Entry> queue = new PriorityQueueTieByTime<>();

    @Override
    public void enqueue(final GraphEdge edge, final Entry previous) {
        float newTotalCost = edge.cost();
        if(previous != null) {
            newTotalCost += previous.totalCost();

            if (!edge.isWalking() && previous.edge().isWalking()) {
                // Walking stops, round up in case path stops on a half tick
                newTotalCost = (float) Math.ceil(newTotalCost);
            }
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
