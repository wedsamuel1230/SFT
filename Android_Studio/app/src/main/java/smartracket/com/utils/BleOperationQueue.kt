package smartracket.com.utils

import java.util.ArrayDeque

/**
 * Simple FIFO queue to serialize BLE GATT operations.
 *
 * BLE APIs allow only one in-flight operation at a time.
 * This queue ensures operations execute sequentially.
 */
class BleOperationQueue {
    interface BleOperation {
        val name: String
        fun execute(): Boolean
    }

    private val lock = Any()
    private val queue: ArrayDeque<BleOperation> = ArrayDeque()
    private var current: BleOperation? = null

    fun enqueue(operation: BleOperation) {
        synchronized(lock) {
            queue.add(operation)
            if (current == null) {
                startNextLocked()
            }
        }
    }

    fun onOperationComplete() {
        synchronized(lock) {
            current = null
            startNextLocked()
        }
    }

    fun clear() {
        synchronized(lock) {
            queue.clear()
            current = null
        }
    }

    private fun startNextLocked() {
        if (queue.isEmpty()) {
            return
        }
        val next = queue.removeFirst()
        current = next
        val started = next.execute()
        if (!started) {
            current = null
            startNextLocked()
        }
    }
}
