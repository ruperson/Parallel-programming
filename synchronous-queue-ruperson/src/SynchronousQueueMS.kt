import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


const val RETRY = "RetryObj"

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(AtomicReference())
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override suspend fun send(element: E) {
        while (true) {
            val curTail = tail.get()
            val curHead = head.get()
            if (curTail == curHead || curTail is Sender<*>) {
                val res = suspendCoroutine<Any> sc@{ cont ->
                    val newNode = Sender(element, cont)
                    if (!curTail.next.compareAndSet(null, newNode)) {
                        cont.resume(RETRY)
                        return@sc
                    }
                    tail.compareAndSet(curTail, newNode)
                }
                if (res != RETRY) return
            } else {
                val headNext = curHead.next.get() as? Receiver<*> ?: continue
                if (head.compareAndSet(curHead, headNext)) {
                    (headNext.action as Continuation<E>).resume(element)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val curTail = tail.get()
            val curHead = head.get()

            if (curTail == curHead || curTail is Receiver<*>) {
                val res = suspendCoroutine<E?> sc@{ cont ->
                    val newNode = Receiver(cont)
                    if (!curTail.next.compareAndSet(null, newNode)) {
                        cont.resume(null)
                        return@sc
                    }
                    tail.compareAndSet(curTail, newNode)
                }
                if (res != null) return res
            } else {
                val headNext = curHead.next.get() as? Sender<*> ?: continue
                if (head.compareAndSet(curHead, headNext)) {
                    headNext.action.resume(Unit)
                    return headNext.element as E
                }
            }
        }
    }

    private open class Node(val next: AtomicReference<Node>)

    private class Receiver<E>(
        val action: Continuation<E>,
        next: AtomicReference<Node> = AtomicReference()
    ) : Node(next)

    private class Sender<E>(
        val element: E,
        val action: Continuation<Unit>,
        next: AtomicReference<Node> = AtomicReference()
    ) : Node(next)
}