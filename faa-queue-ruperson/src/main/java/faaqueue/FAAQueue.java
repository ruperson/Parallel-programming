package faaqueue;

import kotlinx.atomicfu.*;

import static faaqueue.FAAQueue.Node.NODE_SIZE;


public class FAAQueue<T> implements Queue<T> {
    private static final Object DONE = new Object(); // Marker for the "DONE" slot state; to avoid memory leaks

    private AtomicRef<Node> head; // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private AtomicRef<Node> tail; // Tail pointer, similarly to the Michael-Scott queue

    public FAAQueue() {
        Node firstNode = new Node();
        head = new AtomicRef<>(firstNode);
        tail = new AtomicRef<>(firstNode);
    }

    @Override
    public void enqueue(T x) {
        while (true) {
            Node curTail = tail.getValue();
            int enqIdx = curTail.enqIdx.getAndIncrement();
            if (enqIdx >= NODE_SIZE) {
                if (curTail.next.compareAndSet(null, new Node(x))) {
                    tail.compareAndSet(curTail, curTail.next.getValue());
                    return;
                }
            } else if (curTail.data.get(enqIdx).compareAndSet(null, x)) {
                return;
            }
        }
    }

    @Override
    public T dequeue() {
        while (true) {
            Node h = head.getValue();
            if (h.isEmpty()) {
                Node hN = h.next.getValue();
                if (hN == null) {
                    return null;
                }
                head.compareAndSet(h, hN);
                continue;
            }
            int deqIdx = h.deqIdx.getAndIncrement();
            if (deqIdx >= NODE_SIZE) continue;
            Object res = h.data.get(deqIdx).getAndSet(DONE);
            if (res == null) continue;
            return (T) res;
        }
    }


    static class Node {
        static final int NODE_SIZE = 2; // CHANGE ME FOR BENCHMARKING ONLY
        private AtomicRef<Node> next = new AtomicRef<>(null);
        private final AtomicInt enqIdx = new AtomicInt(0); // index for the next enqueue operation
        private final AtomicInt deqIdx = new AtomicInt(0); // index for the next dequeue operation
        private final AtomicArray<Object> data = new AtomicArray<>(NODE_SIZE);

        Node() {}

        Node(Object x) {
            enqIdx.getAndIncrement();
            data.get(0).setValue(x);
        }

        private boolean isEmpty() {
            return deqIdx.getValue() >= enqIdx.getValue() || deqIdx.getValue() >= NODE_SIZE;
        }
    }
}