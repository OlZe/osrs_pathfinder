package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.GraphVertex;

public class PathfindingQueueUnweighted {

    private static final int QUEUE_SIZE = 100000;

    private final Entry[] arrayA = new Entry[QUEUE_SIZE];
    private final Entry[] arrayB = new Entry[QUEUE_SIZE];
    private final Entry[] arrayC = new Entry[QUEUE_SIZE];
    private Entry[] currentQueue = arrayA;
    private Entry[] nextQueue = arrayB;
    private Entry[] nextNextQueue = arrayC;
    private int currentQueueCost = 0;
    private int currentQueueIndexFirst = 0;
    private int currentQueueIndexLast = -1;
    private int nextQueueIndexLast = -1;
    private int nextNextQueueIndexLast = -1;

    public void enqueue(GraphVertex vertex, Entry previous, String edgeTitle, boolean edgeIsWalking) {
        int newTotalCostX2 = 0;

        if (previous != null) {
            newTotalCostX2 = previous.totalCostX2();
            if (!edgeIsWalking && previous.edgeIsWalking()) {
                // Walking stops, round up in case path stops on a half tick
                if (newTotalCostX2 % 2 != 0) {
                    newTotalCostX2++;
                }
            }
            newTotalCostX2++;
        }

        this.enqueue(new Entry(vertex, previous, edgeTitle, edgeIsWalking, newTotalCostX2));
    }

    public Entry dequeue() {
        final Entry entry;

        if (currentQueueIndexFirst <= currentQueueIndexLast) {
            // Take from currentQueue
            entry = currentQueue[currentQueueIndexFirst];
            currentQueueIndexFirst++;
        } else {
            // currentQueue is empty which means we have to move up to the next queue,
            // or we have no more elements left to dequeue
            final boolean noMoreElementsLeft = nextQueueIndexLast < 0 && nextNextQueueIndexLast < 0;
            if (noMoreElementsLeft) {
                entry = null;
            } else {
                this.moveToNextQueue();
                entry = dequeue();
            }
        }
        return entry;
    }

    public Entry peek() {
        final Entry entry;

        if (currentQueueIndexFirst <= currentQueueIndexLast) {
            // Peek at currentQueue
            entry = currentQueue[currentQueueIndexFirst];
        } else {
            // currentQueue is empty which means we have to move up to the next queue,
            // or we have no more elements left to dequeue
            final boolean noMoreElementsLeft = nextQueueIndexLast < 0 && nextNextQueueIndexLast < 0;
            if (noMoreElementsLeft) {
                entry = null;
            } else {
                this.moveToNextQueue();
                entry = peek();
            }
        }
        return entry;
    }

    public boolean hasNext() {
        return currentQueueIndexFirst <= currentQueueIndexLast // currentQueue populated
                || nextQueueIndexLast >= 0 // nextQueue populated
                || nextNextQueueIndexLast >= 0; // nextNextQueue populated
    }

    public int size() {
        return this.currentQueueIndexLast - this.currentQueueIndexFirst + 1 // size currentQueue
                + this.nextQueueIndexLast + 1 // size nextQueue
                + this.nextNextQueueIndexLast + 1; // size nextNextQueue
    }

    private void enqueue(final Entry newEntry) {
        final int costDifference = newEntry.totalCostX2() - currentQueueCost;
        switch (costDifference) {
            case 1 -> {
                nextQueueIndexLast++;
                nextQueue[nextQueueIndexLast] = newEntry;
            }
            case 2 -> {
                nextNextQueueIndexLast++;
                nextNextQueue[nextNextQueueIndexLast] = newEntry;
            }
            case 0 -> {
                currentQueueIndexLast++;
                currentQueue[currentQueueIndexLast] = newEntry;
            }
            default ->
                    throw new IllegalArgumentException("Tried enqueueing an edge of cost " + costDifference + " into unweighted queue.");
        }
    }

    private void moveToNextQueue() {
        final Entry[] oldCurrentQueue = currentQueue;

        currentQueue = nextQueue;
        currentQueueIndexFirst = 0;
        currentQueueIndexLast = nextQueueIndexLast;

        nextQueue = nextNextQueue;
        nextQueueIndexLast = nextNextQueueIndexLast;

        nextNextQueue = oldCurrentQueue;
        nextNextQueueIndexLast = -1;

        currentQueueCost++;
    }


    public record Entry(
            GraphVertex vertex,
            Entry previous,
            String edgeTitle,
            boolean edgeIsWalking,
            int totalCostX2) implements Comparable<Entry> {

        @Override
        public int compareTo(final Entry o) {
            return this.totalCostX2 - o.totalCostX2;
        }
    }
}
