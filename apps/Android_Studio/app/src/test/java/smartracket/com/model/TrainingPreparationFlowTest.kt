package smartracket.com.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import smartracket.com.R

class TrainingPreparationFlowTest {

    @Test
    fun `reset keeps connected player in warm up when sport is already confirmed`() {
        val step = TrainingPreparationFlowRules.stepAfterSessionReset(
            isConnected = true,
            selectedSport = Sport.PICKLEBALL,
            selectionRequired = false
        )

        assertEquals(TrainingPreparationStep.WARM_UP, step)
    }

    @Test
    fun `new connection still asks for sport even when a default exists`() {
        val step = TrainingPreparationFlowRules.stepAfterConnection(
            isNewConnection = true,
            selectedSport = Sport.TENNIS
        )

        assertEquals(TrainingPreparationStep.SPORT_SELECTION, step)
    }

    @Test
    fun `warm up demo advances with elapsed progress`() {
        val plan = WarmUpPlans.forSport(Sport.TABLE_TENNIS)

        val firstDemo = WarmUpMediaLibrary.currentDemo(plan, elapsedTimeMs = 0L)
        val secondDemo = WarmUpMediaLibrary.currentDemo(plan, elapsedTimeMs = 35_000L)

        assertNotNull(firstDemo)
        assertNotNull(secondDemo)
        assertEquals("Wrist circles", firstDemo?.title)
        assertEquals("Shadow forehands", secondDemo?.title)
    }

    @Test
    fun `every sport exposes a dedicated selection icon`() {
        val icons = Sport.entries.associateWith { sport ->
            SportSelectionIconLibrary.iconFor(sport)
        }

        assertEquals(R.drawable.badminton, icons[Sport.BADMINTON])
        assertEquals(R.drawable.other_racket, icons[Sport.OTHER_RACKET_SPORT])
        assertEquals(R.drawable.padel, icons[Sport.PADEL])
        assertEquals(R.drawable.pickleball, icons[Sport.PICKLEBALL])
        assertEquals(R.drawable.squash, icons[Sport.SQUASH])
        assertEquals(R.drawable.table_tennis, icons[Sport.TABLE_TENNIS])
        assertEquals(R.drawable.tennis, icons[Sport.TENNIS])
        assertFalse(icons.values.contains(0))
    }

    @Test
    fun `selector layout spec keeps icons prominent with tighter spacing`() {
        assertEquals(72, SportSelectionLayoutSpec.iconSizeDp)
        assertEquals(16, SportSelectionLayoutSpec.cardPaddingDp)
        assertEquals(12, SportSelectionLayoutSpec.cardContentSpacingDp)
        assertEquals(12, SportSelectionLayoutSpec.gridSpacingDp)
        assertEquals(8, SportSelectionLayoutSpec.sectionTopSpacingDp)
    }

    @Test
    fun `warm up layout spec removes guiding image and enables scrolling`() {
        assertFalse(WarmUpLayoutSpec.showGuidingImage)
        assertEquals(true, WarmUpLayoutSpec.isScrollable)
    }

    @Test
    fun `same connection reset resumes warm up instead of idling`() {
        val sessionState = TrainingPreparationFlowRules.sessionStateAfterSessionReset(
            nextPreparationStep = TrainingPreparationStep.WARM_UP,
            warmUpRequiredForConnection = true
        )

        assertEquals(SessionState.WARMING_UP, sessionState)
    }

    @Test
    fun `same connection reset stays idle when warm up was skipped for the connection`() {
        val sessionState = TrainingPreparationFlowRules.sessionStateAfterSessionReset(
            nextPreparationStep = TrainingPreparationStep.WARM_UP,
            warmUpRequiredForConnection = false
        )

        assertEquals(SessionState.IDLE, sessionState)
    }

    @Test
    fun `warm up plan exposes step descriptions and advances into second step`() {
        val plan = WarmUpPlans.forSport(Sport.TABLE_TENNIS)

        assertTrue(plan.steps.all { it.description.isNotBlank() })
        assertEquals(1, WarmUpProgress.currentStepIndex(plan, elapsedTimeMs = 30_000L))
    }

    @Test
    fun `start training stays disabled until warm up fully completes unless connection skip is active`() {
        val plan = WarmUpPlans.forSport(Sport.TABLE_TENNIS)
        val almostDoneElapsed = (plan.totalDurationSeconds * 1000L) - 1L

        assertFalse(
            WarmUpActionRules.canStartTraining(
                plan = plan,
                elapsedTimeMs = almostDoneElapsed,
                warmUpRequiredForConnection = true
            )
        )
        assertTrue(
            WarmUpActionRules.canStartTraining(
                plan = plan,
                elapsedTimeMs = plan.totalDurationSeconds * 1000L,
                warmUpRequiredForConnection = true
            )
        )
        assertTrue(
            WarmUpActionRules.canStartTraining(
                plan = plan,
                elapsedTimeMs = 0L,
                warmUpRequiredForConnection = false
            )
        )
    }
}