package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge;

public class PathfindingPriorityQueue implements PathfindingQueue {

    private final PriorityQueueTieByTime<Entry> queue = new PriorityQueueTieByTime<>();

    @Override
    public void enqueue(final GraphEdge edge, final Entry previous) {
        int newTotalCostX2 = edge.costX2();
        if(previous != null) {
            newTotalCostX2 += previous.totalCostX2();

            if (!edge.isWalking() && previous.edge().isWalking()) {
                // Walking stops, round up in case path stops on a half tick
                if(newTotalCostX2 % 2 != 0) {
                    newTotalCostX2++;
                }
            }
        }

        this.queue.add(new Entry(edge, previous, newTotalCostX2));
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
