package smartracket.com.model

import androidx.annotation.DrawableRes
import smartracket.com.R
import smartracket.com.model.SessionState

enum class Sport(
    val displayName: String,
    val healthSessionTitle: String
) {
    TABLE_TENNIS("Table Tennis", "Table Tennis Training"),
    PICKLEBALL("Pickleball", "Pickleball Training"),
    TENNIS("Tennis", "Tennis Training"),
    BADMINTON("Badminton", "Badminton Training"),
    SQUASH("Squash", "Squash Training"),
    PADEL("Padel", "Padel Training"),
    OTHER_RACKET_SPORT("Other Racket Sport", "Racket Sport Training");

    companion object {
        fun fromName(name: String?): Sport {
            return entries.firstOrNull { it.name == name } ?: TABLE_TENNIS
        }
    }
}

enum class WarmUpState {
    NOT_STARTED,
    COMPLETED,
    SKIPPED
}

data class WarmUpStep(
    val title: String,
    val description: String,
    val durationSeconds: Int
)

data class WarmUpPlan(
    val sport: Sport,
    val title: String,
    val totalDurationSeconds: Int,
    val steps: List<WarmUpStep>
)

data class WarmUpDemoMedia(
    val title: String,
    val caption: String,
    @DrawableRes val illustrationRes: Int
)

data class RestReminderUiState(
    val reminderCount: Int,
    val elapsedTimeMs: Long
)

enum class TrainingPreparationStep {
    SPORT_SELECTION,
    WARM_UP
}

object TrainingPreparationFlowRules {
    fun stepAfterConnection(
        isNewConnection: Boolean,
        selectedSport: Sport?
    ): TrainingPreparationStep {
        return if (isNewConnection || selectedSport == null) {
            TrainingPreparationStep.SPORT_SELECTION
        } else {
            TrainingPreparationStep.WARM_UP
        }
    }

    fun stepAfterSessionReset(
        isConnected: Boolean,
        selectedSport: Sport?,
        selectionRequired: Boolean
    ): TrainingPreparationStep {
        return if (isConnected && selectedSport != null && !selectionRequired) {
            TrainingPreparationStep.WARM_UP
        } else {
            TrainingPreparationStep.SPORT_SELECTION
        }
    }

    fun sessionStateAfterSessionReset(
        nextPreparationStep: TrainingPreparationStep,
        warmUpRequiredForConnection: Boolean
    ): SessionState {
        return if (nextPreparationStep == TrainingPreparationStep.WARM_UP && warmUpRequiredForConnection) {
            SessionState.WARMING_UP
        } else {
            SessionState.IDLE
        }
    }
}

object WarmUpActionRules {
    fun canStartTraining(
        plan: WarmUpPlan?,
        elapsedTimeMs: Long,
        warmUpRequiredForConnection: Boolean
    ): Boolean {
        if (!warmUpRequiredForConnection) return true
        val totalDurationMs = (plan?.totalDurationSeconds ?: 0) * 1000L
        if (totalDurationMs <= 0L) return true
        return elapsedTimeMs >= totalDurationMs
    }
}

object SportSelectionIconLibrary {
    @DrawableRes
    fun iconFor(sport: Sport): Int {
        return when (sport) {
            Sport.BADMINTON -> R.drawable.badminton
            Sport.OTHER_RACKET_SPORT -> R.drawable.other_racket
            Sport.PADEL -> R.drawable.padel
            Sport.PICKLEBALL -> R.drawable.pickleball
            Sport.SQUASH -> R.drawable.squash
            Sport.TABLE_TENNIS -> R.drawable.table_tennis
            Sport.TENNIS -> R.drawable.tennis
        }
    }
}

object SportSelectionLayoutSpec {
    const val iconSizeDp = 72
    const val cardPaddingDp = 16
    const val cardContentSpacingDp = 12
    const val gridSpacingDp = 12
    const val sectionTopSpacingDp = 8
}

object WarmUpLayoutSpec {
    const val showGuidingImage = false
    const val isScrollable = true
}

object WarmUpMediaLibrary {
    fun currentDemo(plan: WarmUpPlan?, elapsedTimeMs: Long): WarmUpDemoMedia? {
        if (plan == null) return null

        val currentStep = currentStep(plan, elapsedTimeMs) ?: return null
        val illustrationRes = illustrationForStep(currentStep.title)

        return WarmUpDemoMedia(
            title = currentStep.title,
            caption = captionForIllustration(illustrationRes),
            illustrationRes = illustrationRes
        )
    }

    private fun currentStep(plan: WarmUpPlan, elapsedTimeMs: Long): WarmUpStep? {
        val elapsedSeconds = (elapsedTimeMs / 1000L).toInt().coerceAtLeast(0)
        var runningDuration = 0

        for (step in plan.steps) {
            runningDuration += step.durationSeconds
            if (elapsedSeconds < runningDuration) {
                return step
            }
        }

        return plan.steps.lastOrNull()
    }

    @DrawableRes
    private fun illustrationForStep(stepTitle: String): Int {
        val normalized = stepTitle.lowercase()
        return when {
            normalized.contains("wrist") ||
                normalized.contains("shoulder") ||
                normalized.contains("ankle") ||
                normalized.contains("torso") ||
                normalized.contains("joint") -> R.drawable.illustration_warmup_mobility

            normalized.contains("shadow") ||
                normalized.contains("ghost") ||
                normalized.contains("volleys") -> R.drawable.illustration_warmup_shadow

            else -> R.drawable.illustration_warmup_footwork
        }
    }

    private fun captionForIllustration(@DrawableRes illustrationRes: Int): String {
        return when (illustrationRes) {
            R.drawable.illustration_warmup_mobility -> "Open the joints first so your swing feels loose from the first rally."
            R.drawable.illustration_warmup_shadow -> "Rehearse clean racket paths before you add ball speed or pressure."
            else -> "Wake up your base with quick directional steps before live movement begins."
        }
    }
}

object WarmUpProgress {
    fun currentStepIndex(plan: WarmUpPlan?, elapsedTimeMs: Long): Int {
        if (plan == null || plan.steps.isEmpty()) return -1

        val elapsedSeconds = (elapsedTimeMs / 1000L).toInt().coerceAtLeast(0)
        var runningDuration = 0

        plan.steps.forEachIndexed { index, step ->
            runningDuration += step.durationSeconds
            if (elapsedSeconds < runningDuration) {
                return index
            }
        }

        return plan.steps.lastIndex
    }
}

object WarmUpPlans {
    fun forSport(sport: Sport): WarmUpPlan {
        val steps = when (sport) {
            Sport.TABLE_TENNIS -> listOf(
                WarmUpStep("Wrist circles", "Hold the racket loosely and draw slow circles both clockwise and counterclockwise to loosen the wrist.", 30),
                WarmUpStep("Shadow forehands", "Without a ball, rehearse compact forehand swings and finish balanced over your front foot.", 45),
                WarmUpStep("Quick footwork taps", "Stay light on your toes and make quick side-to-side taps as if recovering for the next shot.", 45)
            )
            Sport.PICKLEBALL -> listOf(
                WarmUpStep("Shoulder rolls", "Roll both shoulders forward and backward in smooth circles to open the upper body.", 30),
                WarmUpStep("Shadow volleys", "Practice short punch volleys in front of your body with the paddle face stable and quiet.", 45),
                WarmUpStep("Split-step rhythm", "Bounce into a small split step, land softly, and reset your stance for the next exchange.", 45)
            )
            Sport.TENNIS -> listOf(
                WarmUpStep("Dynamic shoulder warm-up", "Sweep your arms through controlled circles and cross-body reaches to loosen the shoulder line.", 45),
                WarmUpStep("Shadow groundstrokes", "Rehearse forehand and backhand drives with smooth unit turns and a relaxed follow-through.", 45),
                WarmUpStep("Lateral recovery steps", "Push out wide, recover to center, and keep your chest tall through each shuffle.", 60)
            )
            Sport.BADMINTON -> listOf(
                WarmUpStep("Ankle mobility", "Rock forward and back over each ankle, then circle the joint to prepare for quick directional changes.", 30),
                WarmUpStep("Shadow clears", "Practice overhead clear motions with full extension while keeping your grip relaxed.", 45),
                WarmUpStep("Split-step hops", "Make short reactive hops, land on the balls of your feet, and stay ready to explode in any direction.", 45)
            )
            Sport.SQUASH -> listOf(
                WarmUpStep("Torso rotations", "Rotate through the ribs and hips with the racket held across your chest to wake up the core.", 30),
                WarmUpStep("Ghost swings", "Move into imaginary corners and rehearse clean compact swings without a ball.", 45),
                WarmUpStep("Front-back movement", "Step forward into a lunge, push back out, and repeat with quick balanced recoveries.", 45)
            )
            Sport.PADEL -> listOf(
                WarmUpStep("Band-free shoulder prep", "Use controlled arm raises and shoulder circles to prep for repeated overhead contacts.", 30),
                WarmUpStep("Wall-ready volleys", "Rehearse compact volley blocks in front of your body like you are controlling rebounds off the glass.", 45),
                WarmUpStep("Side shuffles", "Shuffle laterally with small quick steps and keep the racket up in front throughout.", 45)
            )
            Sport.OTHER_RACKET_SPORT -> listOf(
                WarmUpStep("Joint mobility", "Loosen wrists, shoulders, hips, and ankles with smooth circles before you raise the intensity.", 30),
                WarmUpStep("Shadow swings", "Practice your main swing pattern without a ball, focusing on rhythm and balance.", 45),
                WarmUpStep("Light footwork", "Stay springy through your feet and make short directional steps to prepare for live movement.", 45)
            )
        }

        return WarmUpPlan(
            sport = sport,
            title = "${sport.displayName} warm-up",
            totalDurationSeconds = steps.sumOf { it.durationSeconds },
            steps = steps
        )
    }
}

object RestReminderPolicy {
    const val DEFAULT_INTERVAL_MS: Long = 30 * 60 * 1000L

    fun reminderCountForElapsed(
        elapsedTimeMs: Long,
        intervalMs: Long = DEFAULT_INTERVAL_MS
    ): Int {
        if (elapsedTimeMs < intervalMs || intervalMs <= 0L) return 0
        return (elapsedTimeMs / intervalMs).toInt()
    }

    fun shouldTriggerReminder(
        elapsedTimeMs: Long,
        remindersAlreadyShown: Int,
        intervalMs: Long = DEFAULT_INTERVAL_MS
    ): Boolean {
        return reminderCountForElapsed(elapsedTimeMs, intervalMs) > remindersAlreadyShown
    }
}