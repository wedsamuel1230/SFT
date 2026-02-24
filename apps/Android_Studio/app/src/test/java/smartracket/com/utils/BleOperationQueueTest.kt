package smartracket.com.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BleOperationQueueTest {
    @Test
    fun `executes operations in FIFO order and waits for completion`() {
        val queue = BleOperationQueue()
        val executed = mutableListOf<String>()

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-1"
            override fun execute(): Boolean {
                executed.add(name)
                return true
            }
        })

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-2"
            override fun execute(): Boolean {
                executed.add(name)
                return true
            }
        })

        assertEquals(listOf("op-1"), executed)

        queue.onOperationComplete()
        assertEquals(listOf("op-1", "op-2"), executed)
    }

    @Test
    fun `skips failed operations and continues`() {
        val queue = BleOperationQueue()
        val executed = mutableListOf<String>()

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-fail"
            override fun execute(): Boolean {
                executed.add(name)
                return false
            }
        })

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-next"
            override fun execute(): Boolean {
                executed.add(name)
                return true
            }
        })

        assertEquals(listOf("op-fail", "op-next"), executed)
    }

    @Test
    fun `completing advances to next operation`() {
        val queue = BleOperationQueue()
        val executed = mutableListOf<String>()

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-1"
            override fun execute(): Boolean {
                executed.add(name)
                return true
            }
        })

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-2"
            override fun execute(): Boolean {
                executed.add(name)
                return true
            }
        })

        queue.enqueue(object : BleOperationQueue.BleOperation {
            override val name = "op-3"
            override fun execute(): Boolean {
                executed.add(name)
                return true
            }
        })

        assertEquals(listOf("op-1"), executed)

        queue.onOperationComplete()
        assertEquals(listOf("op-1", "op-2"), executed)

        queue.onOperationComplete()
        assertEquals(listOf("op-1", "op-2", "op-3"), executed)
    }
}
