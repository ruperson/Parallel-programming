package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node T = tail.getValue();
            if (T.next.compareAndSet(null, newTail) ) {
                // node has been added, move tail forward
                tail.compareAndSet(T, newTail);
                return;
            } else {
                // help other enqueue operations
                tail.compareAndSet(T, T.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while(true) {
            Node h = head.getValue();
            Node hN = h.next.getValue();
            if (hN == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(h, hN)) {
                return hN.x;
            }
        }
    }

    @Override
    public int peek() {
        Node h = head.getValue();
        Node hN = h.next.getValue();
        if (hN == null) return Integer.MIN_VALUE;
        //if (head.compareAndSet(h, h)) {
        return hN.x;
        //}
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x) {
            this.next = new AtomicRef<>(null);
            this.x = x;
        }
    }
}