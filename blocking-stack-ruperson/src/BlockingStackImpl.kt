import java.util.concurrent.atomic.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlockingStackImpl<E> : BlockingStack<E> {

    // ==========================
    // Segment Queue Synchronizer
    // ==========================

    private val dummy = Receiver<E>(null)
    private val enqIdx = AtomicReference<Receiver<E>>(dummy)
    private val deqIdx = AtomicReference<Receiver<E>>(dummy)

    private class Receiver<E>(
        val action: Continuation<E>?,
        val next: AtomicReference<Receiver<E>> = AtomicReference()
    )

    private suspend fun suspend(): E {
        return suspendCoroutine { cont ->
            val node = Receiver(cont)
            while (true) {
                val curTail = deqIdx.get()
                if (curTail.next.compareAndSet(null, node)) {
                    if (deqIdx.compareAndSet(curTail, node)) {
                        break
                    }
                }
            }
        }
    }

    private fun resume(element: E) {
        while (true) {
            val curHead = enqIdx.get()
            if (curHead == deqIdx.get()) continue
            val node = curHead.next.get()
            if (enqIdx.compareAndSet(curHead, node)) {
                node.action!!.resume(element)
                return
            }
        }
    }

    // ==============
    // Blocking Stack
    // ==============


    private val head = AtomicReference<Node<E>?>()
    private val elements = AtomicInteger()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            // push the element to the top of the stack
            while (true) {
                val curHead = head.get()
                if (curHead?.element != SUSPENDED) {
                    if (head.compareAndSet(curHead, Node(element, curHead))) break
                } else {
                    val node = curHead.next
                    if (head.compareAndSet(curHead, node)) {
                        resume(element)
                        return
                    }
                }
            }
        } else {
            // resume the next waiting receiver
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            // remove the top element from the stack
            while (true) {
                val curHead = head.get()
                if (curHead == null) {
                    if (head.compareAndSet(null, Node(SUSPENDED, null))) return suspend()
                } else {
                    val node = curHead.next
                    if (head.compareAndSet(curHead, node)) return curHead.element as E
                }
            }
        } else {
            return suspend()
        }
    }
}

private class Node<E>(val element: Any?, val next: Node<E>?)

private val SUSPENDED = Any()