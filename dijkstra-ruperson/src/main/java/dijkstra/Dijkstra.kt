package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }

private class MultiPriorityQueue(val workers: Int) {
    private val queue: Array<PriorityQueue<Node>> = Array(workers) { PriorityQueue<Node>(NODE_DISTANCE_COMPARATOR) }
    private val random = Random()

    fun add(node: Node) {
        val ind = random.nextInt(workers)
        synchronized(queue[ind]) {
            queue[ind].add(node)
        }
    }

    fun poll() : Node? {
        val a = random.nextInt(workers)
        val b = random.nextInt(workers)
        val i = kotlin.math.min(a, b)
        val j = kotlin.math.max(a, b)


        synchronized(queue[i]) {
            synchronized(queue[j]) {
                val firstIsEmpty = queue[i].isEmpty()
                val secondIsEmpty = queue[j].isEmpty()
                return if (!firstIsEmpty && !secondIsEmpty) {
                    if (queue[i].peek().distance < queue[j].peek().distance) {
                        queue[i].poll()
                    } else {
                        queue[j].poll()
                    }
                } else {
                    if (!firstIsEmpty) {
                        queue[i].poll()
                    } else if (!secondIsEmpty) {
                        queue[j].poll()
                    } else {
                        null
                    }
                }
            }
        }
    }
}

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val activeNodes = AtomicInteger(1)
    val q = MultiPriorityQueue(workers)
    q.add(start)
    val onFinish = Phaser(workers + 1)
    repeat(workers) {
        thread {
            while (!activeNodes.compareAndSet(0, 0)) {
                val u: Node = q.poll() ?: continue
                val d = u.distance
                for (v in u.outgoingEdges) {
                    while (true) {
                        val curDistance = v.to.distance
                        val newDistance = d + v.weight
                        if (curDistance > newDistance) {
                            if (!v.to.casDistance(curDistance, newDistance)) {
                                continue
                            }
                            q.add(v.to)
                            activeNodes.incrementAndGet()
                        }
                        break
                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}