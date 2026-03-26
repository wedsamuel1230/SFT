package smartracket.com.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McuModelOutputTest {

    @Test
    fun `event warning marks output as warning and returns message`() {
        val output = McuModelOutput(
            ts = 123L,
            stroke = "forehand",
            event = "warning",
            conf = 0.99f,
            peak = 1946.3f
        )

        assertTrue(output.isWarning)
        val message = output.warningMessageOrNull()
        assertNotNull(message)
        assertTrue(message!!.contains("Overload warning"))
    }

    @Test
    fun `overload stroke marks output as warning even without event field`() {
        val output = McuModelOutput(
            ts = 123L,
            stroke = "overload_gyro",
            conf = 0.99f,
            peak = 2272.5f
        )

        assertTrue(output.isWarning)
        assertTrue(output.isTooHeavy)
        assertNotNull(output.warningMessageOrNull())
    }

    @Test
    fun `warning below threshold is not too heavy`() {
        val output = McuModelOutput(
            ts = 123L,
            stroke = "overload_accel",
            event = "warning",
            conf = 0.95f,
            peak = 299.9f
        )

        assertTrue(output.isWarning)
        assertFalse(output.isTooHeavy)
    }

    @Test
    fun `normal stroke is not warning`() {
        val output = McuModelOutput(
            ts = 123L,
            stroke = "backhand",
            conf = 0.92f,
            peak = 12.2f
        )

        assertFalse(output.isWarning)
        assertNull(output.warningMessageOrNull())
    }
}
