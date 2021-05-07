public class Solution implements AtomicCounter {

    private final Node root = new Node(0);
    private final ThreadLocal<Node> last = ThreadLocal.withInitial(() -> root);

    @Override
    public int getAndAdd(int x) {
        Node node;
        int res = 0;
        do {
            res = last.get().value;
            node = new Node(res + x);
            last.set(last.get().next.decide(node));
        } while (node != last.get());
        return res;
    }

    private static class Node {
        private final int value;
        private final Consensus<Node> next;

        private Node(int x) {
            this.value = x;
            this.next = new Consensus<>();
        }
    }
}
