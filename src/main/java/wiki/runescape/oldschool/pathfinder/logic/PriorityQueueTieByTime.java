package wiki.runescape.oldschool.pathfinder.logic;

import java.util.PriorityQueue;

/**
 * Provides a PriorityQueue where ties are broken by the time in which they have been inserted
 * Example: Inputting Objects A,B,C,D (same weight) will always return A,B,C,D instead of eg A,C,D,B
 */
public class PriorityQueueTieByTime<E extends Comparable<E>> {
    private final PriorityQueue<QueueEntry<E>> queue = new PriorityQueue<>();
    private int elementsInserted = 0;

    public boolean add(E element) {
        try {
            this.elementsInserted = Math.addExact(this.elementsInserted, 1);
        } catch (Exception e) {
            throw new RuntimeException("PriorityQueueTieByTime had too many insertions. Consider using a bigger data type", e);
        }
        return this.queue.add(new QueueEntry<>(element, this.elementsInserted));
    }

    public E peek() {
        final QueueEntry<E> queueEntry = this.queue.peek();
        return queueEntry != null ? queueEntry.element : null;
    }

    public E remove() {
        return this.queue.remove().element;
    }

    public int size() {
        return this.queue.size();
    }


    private record QueueEntry<E extends Comparable<E>>(E element,
                                                       int insertionCount) implements Comparable<QueueEntry<E>> {

        @Override
        public int compareTo(final QueueEntry<E> o) {
            int elementOrder = this.element.compareTo(o.element);
            if (elementOrder != 0) {
                return elementOrder;
            } else {
                // Tie, prioritize earlier element
                return this.insertionCount - o.insertionCount;
            }
        }
    }

}
