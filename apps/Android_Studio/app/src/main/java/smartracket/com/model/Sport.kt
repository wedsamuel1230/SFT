package smartracket.com.model

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
    val durationSeconds: Int
)

data class WarmUpPlan(
    val sport: Sport,
    val title: String,
    val totalDurationSeconds: Int,
    val steps: List<WarmUpStep>
)

data class RestReminderUiState(
    val reminderCount: Int,
    val elapsedTimeMs: Long
)

enum class TrainingPreparationStep {
    SPORT_SELECTION,
    WARM_UP
}

object WarmUpPlans {
    fun forSport(sport: Sport): WarmUpPlan {
        val steps = when (sport) {
            Sport.TABLE_TENNIS -> listOf(
                WarmUpStep("Wrist circles", 30),
                WarmUpStep("Shadow forehands", 45),
                WarmUpStep("Quick footwork taps", 45)
            )
            Sport.PICKLEBALL -> listOf(
                WarmUpStep("Shoulder rolls", 30),
                WarmUpStep("Shadow volleys", 45),
                WarmUpStep("Split-step rhythm", 45)
            )
            Sport.TENNIS -> listOf(
                WarmUpStep("Dynamic shoulder warm-up", 45),
                WarmUpStep("Shadow groundstrokes", 45),
                WarmUpStep("Lateral recovery steps", 60)
            )
            Sport.BADMINTON -> listOf(
                WarmUpStep("Ankle mobility", 30),
                WarmUpStep("Shadow clears", 45),
                WarmUpStep("Split-step hops", 45)
            )
            Sport.SQUASH -> listOf(
                WarmUpStep("Torso rotations", 30),
                WarmUpStep("Ghost swings", 45),
                WarmUpStep("Front-back movement", 45)
            )
            Sport.PADEL -> listOf(
                WarmUpStep("Band-free shoulder prep", 30),
                WarmUpStep("Wall-ready volleys", 45),
                WarmUpStep("Side shuffles", 45)
            )
            Sport.OTHER_RACKET_SPORT -> listOf(
                WarmUpStep("Joint mobility", 30),
                WarmUpStep("Shadow swings", 45),
                WarmUpStep("Light footwork", 45)
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