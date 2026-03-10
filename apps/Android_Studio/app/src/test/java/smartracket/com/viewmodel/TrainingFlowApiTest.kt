package smartracket.com.viewmodel

import org.junit.Assert.assertTrue
import org.junit.Test
import smartracket.com.model.SessionState

class TrainingFlowApiTest {

    @Test
    fun `training view model exposes sport warm up and rest reminder flow state`() {
        val methodNames = TrainingViewModel::class.java.declaredMethods.map { it.name }
        val fieldNames = TrainingViewModel::class.java.declaredFields.map { it.name }

        assertTrue(methodNames.contains("selectSport"))
        assertTrue(methodNames.contains("confirmSportSelection"))
        assertTrue(methodNames.contains("beginWarmUp"))
        assertTrue(methodNames.contains("skipWarmUp"))
        assertTrue(methodNames.contains("dismissRestReminder"))

        assertTrue(fieldNames.contains("_selectedSport"))
        assertTrue(fieldNames.contains("_warmUpPlan"))
        assertTrue(fieldNames.contains("_warmUpElapsedTime"))
        assertTrue(fieldNames.contains("_showRestReminder"))
    }

    @Test
    fun `session state includes warming up`() {
        val names = SessionState.entries.map { it.name }

        assertTrue(names.contains("WARMING_UP"))
    }
}