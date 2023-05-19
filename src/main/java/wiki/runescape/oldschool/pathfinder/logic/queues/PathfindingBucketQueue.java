package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class PathfindingBucketQueue implements PathfindingQueue {
    final private HashMap<Integer, Queue<Entry>> buckets = new HashMap<>();
    final private PriorityQueue<Integer> bucketsCosts = new PriorityQueue<>();

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

        final Entry newEntry = new Entry(edge, previous, newTotalCostX2);

        final Queue<Entry> bucket = buckets.get(newTotalCostX2);
        if(bucket != null) {
            bucket.add(newEntry);
        }
        else {
            // Create new bucket
            final Queue<Entry> newBucket = new LinkedList<>();
            newBucket.add(newEntry);
            buckets.put(newTotalCostX2, newBucket);
            bucketsCosts.add(newTotalCostX2);
        }
    }

    @Override
    public Entry dequeue() {
        final Integer lowestCost = this.bucketsCosts.peek();
        if(lowestCost == null) {
            return null;
        }
        final Queue<Entry> bucket = this.buckets.get(lowestCost);
        final Entry nextEntry = bucket.remove();

        if(bucket.size() == 0) {
            // Bucket empty, delete
            buckets.remove(lowestCost);
            bucketsCosts.remove();
        }

        return nextEntry;
    }

    @Override
    public boolean hasNext() {
        return this.bucketsCosts.peek() != null;
    }

    @Override
    public int size() {
        int size = 0;
        for (Queue<Entry> bucket : buckets.values()) {
            size += bucket.size();
        }
        return size;
    }
}
