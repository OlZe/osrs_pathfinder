package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge;

import java.util.PriorityQueue;

public class PathfindingArrayQueue implements PathfindingQueue {

    private static final int QUEUE_SIZE = 100000;

    private final PriorityQueue<Entry> otherCostsQueue = new PriorityQueue<>();
    private final Entry[] queueA = new Entry[QUEUE_SIZE];
    private final Entry[] queueB = new Entry[QUEUE_SIZE];
    private Entry[] currentQueue = queueA;
    private Entry[] nextQueue = queueB;
    private int currentQueueCost = 0;
    private int currentQueueIndexFirst = 0;
    private int currentQueueIndexLast = -1;
    private int nextQueueIndexLast = -1;


    @Override
    public void enqueue(final GraphEdge edge, final Entry previous) {
        int newTotalCostX2 = edge.costX2();
        if (previous != null) {
            newTotalCostX2 += previous.totalCostX2();

            if (!edge.isWalking() && previous.edge().isWalking()) {
                // Walking stops, round up in case path stops on a half tick
                if (newTotalCostX2 % 2 != 0) {
                    newTotalCostX2++;
                }
            }
        }

        this.enqueue(new Entry(edge, previous, newTotalCostX2));
    }

    @Override
    public Entry dequeue() {
        final Entry entry;

        if (currentQueueIndexFirst <= currentQueueIndexLast) {
            // Take from currentQueue
            entry = currentQueue[currentQueueIndexFirst];
            currentQueueIndexFirst++;
        } else if (otherCostsQueue.size() > 0 && otherCostsQueue.peek().totalCostX2() == currentQueueCost) {
            // Take from otherCostsQueue
            entry = otherCostsQueue.remove();
        } else {
            // Current queue is empty, otherCostsQueue does not have elements with currentQueueCost
            // which means we have to move up to the next queue, or we have no more elements left to dequeue
            final boolean noMoreElementsLeft = nextQueueIndexLast < 0 && otherCostsQueue.size() == 0;
            if (noMoreElementsLeft) {
                entry = null;
            } else {
                this.moveNextQueueToCurrentQueue();
                entry = dequeue();
            }
        }

        return entry;
    }

    @Override
    public boolean hasNext() {
        return currentQueueIndexFirst <= currentQueueIndexLast // Current queue populated
                || nextQueueIndexLast >= 0 // Next queue populated
                || otherCostsQueue.size() > 0; // otherCostsQueue populated
    }

    @Override
    public int size() {
        final int currentQueueSize = currentQueueIndexLast - currentQueueIndexFirst + 1;
        final int nextQueueSize = nextQueueIndexLast + 1;
        final int otherCostsQueueSize = otherCostsQueue.size();
        return currentQueueSize + nextQueueSize + otherCostsQueueSize;
    }

    private void enqueue(final Entry newEntry) {
        final int costDifference = newEntry.totalCostX2() - currentQueueCost;
        if (costDifference == 1) {
            nextQueueIndexLast++;
            nextQueue[nextQueueIndexLast] = newEntry;
        } else if (costDifference > 1) {
            otherCostsQueue.add(newEntry);
        } else {
            // costDifference == 0
            currentQueueIndexLast++;
            currentQueue[currentQueueIndexLast] = newEntry;
        }
    }

    private void moveNextQueueToCurrentQueue() {
        if (currentQueue == queueA) {
            currentQueue = queueB;
            nextQueue = queueA;
        } else {
            currentQueue = queueA;
            nextQueue = queueB;
        }
        currentQueueIndexFirst = 0;
        currentQueueIndexLast = nextQueueIndexLast;
        nextQueueIndexLast = -1;
        currentQueueCost++;
    }
}
