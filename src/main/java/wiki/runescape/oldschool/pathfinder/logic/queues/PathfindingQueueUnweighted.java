package wiki.runescape.oldschool.pathfinder.logic.queues;

import wiki.runescape.oldschool.pathfinder.logic.graph.unweighted.GraphVertex;

public class PathfindingQueueUnweighted {

    private final PriorityQueueTieByTime<Entry> queue = new PriorityQueueTieByTime<>();

    public void enqueue(GraphVertex vertex, Entry previous, String edgeTitle, boolean edgeIsWalking) {
        int newTotalCostX2 = 0;

        if(previous != null) {
            newTotalCostX2 = previous.totalCostX2();
            if(!edgeIsWalking && previous.edgeIsWalking()) {
                // Walking stops, round up in case path stops on a half tick
                if(newTotalCostX2 % 2 != 0) {
                    newTotalCostX2++;
                }
            }
            newTotalCostX2++;
        }

        this.queue.add(new Entry(vertex, previous, edgeTitle, edgeIsWalking, newTotalCostX2));
    }

    public Entry dequeue() {
        return this.queue.remove();
    }

    public boolean hasNext() {
        return this.queue.peek() != null;
    }

    public int size() {
        return this.queue.size();
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
