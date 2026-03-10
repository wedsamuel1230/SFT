package smartracket.com.model

import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingMetadataModelTest {

    @Test
    fun `device pairing exposes a persisted default sport`() {
        val fieldNames = DevicePairing::class.java.declaredFields.map { it.name }

        assertTrue(
            "DevicePairing should store a default sport for each racket",
            fieldNames.contains("defaultSport")
        )
    }

    @Test
    fun `training session exposes sport warm up and reminder metadata`() {
        val fieldNames = TrainingSession::class.java.declaredFields.map { it.name }

        assertTrue(
            "TrainingSession should persist the selected sport",
            fieldNames.contains("sport")
        )
        assertTrue(
            "TrainingSession should persist warm-up completion state",
            fieldNames.contains("warmUpState")
        )
        assertTrue(
            "TrainingSession should persist warm-up duration",
            fieldNames.contains("warmUpDurationMs")
        )
        assertTrue(
            "TrainingSession should persist recurring reminder cadence",
            fieldNames.contains("restReminderIntervalMs")
        )
        assertTrue(
            "TrainingSession should persist how many rest reminders fired",
            fieldNames.contains("restReminderCount")
        )
    }
}