package stack;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicRef;

import java.util.concurrent.ThreadLocalRandom;

interface Base {}

class DONE implements Base{
    private DONE() {}
    static final DONE INSTANCE = new DONE();
}

class Number implements Base {
    public int num;
    public Number(int num) {
        this.num = num;
    }
}

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private AtomicArray<Base> rendezVous = new AtomicArray<> (10);

    @Override
    public void push(int x) {
        int index = 0;
        for (int i = 0; i < 10; i++) {
            index = ThreadLocalRandom.current().nextInt(1, 10);
            if (rendezVous.get(index).compareAndSet(null, new Number(x))) {
                for (int j = 0; j < 10; j++) {
                    if (rendezVous.get(index).compareAndSet(DONE.INSTANCE, null)) {
                        return;
                    }
                }

                Base cur = rendezVous.get(index).getValue();
                if (cur instanceof Number
                        && rendezVous.get(index).compareAndSet(cur, null)) {
                    break;
                } else if (rendezVous.get(index).compareAndSet(DONE.INSTANCE, null)) {
                    return;
                }
                break;
            }
        }

        while (true) {
            Node h = head.getValue();
            Node newHead = new Node(x, h);
            if (head.compareAndSet(h, newHead)) {
                return;
            }
        }
    }
    @Override
    public int pop() {
        int index;
        for (int i = 0; i < 10 ; i++) {
            index = ThreadLocalRandom.current().nextInt(1, 10);
            Base expectedNumber = rendezVous.get(index).getValue();
            if (expectedNumber instanceof Number && rendezVous.get(index).compareAndSet(expectedNumber, DONE.INSTANCE)) {
                return ((Number)expectedNumber).num;
            }
        }

        while(true) {
            Node h = head.getValue();
            if (h == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(h, h.next.getValue())) {
                return h.x;
            }
        }
    }
}
