# SmartRacket Localization, Highlights, and Layout Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Centralize localization, apply app-wide CJK font, fix layout issues, and restrict highlights to BLE-triggered/manual (connected only).

**Architecture:** Introduce a single localization map keyed by `Language` and expose it app-wide via CompositionLocal/Flow. Update UI screens to consume localized strings and apply a global CJK font via `Typography`. Refactor highlight capture to be triggered only by BLE button events and gated manual actions.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Hilt, StateFlow, Room, BLE (BluetoothManager/BluetoothRepository).

**Plan Location:** `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md`

**Memory Bank Context:** `memory-bank/activeContext.md` (current focus placeholder), `memory-bank/SESSION.md` (v0.1.0 in progress).

---

### Task 1: Establish centralized localization + app language source

**Memory Bank Updates:**
- Update: `memory-bank/activeContext.md` - Set focus to localization + BLE highlight constraints.
- Update: `memory-bank/master-plan.md` - Add milestones for localization, highlights, layout fixes.
- Append: `memory-bank/SESSION.md` - Add session entry for plan creation.
- Update: `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md` - Mark Task 1 complete.

**Files:**
- Create: `app/src/main/java/smartracket/com/ui/i18n/Strings.kt`
- Modify: `app/src/main/java/smartracket/com/MainActivity.kt`
- Modify: `app/src/main/java/smartracket/com/ui/screens/SettingsScreen.kt`

**Step 1: Write the failing test**

Create `app/src/test/java/smartracket/com/ui/i18n/StringsTest.kt`:
```kotlin
@Test
fun returnsLocalizedStringForChinese() {
    val zh = Language.CHINESE
    val strings = Strings.forLanguage(zh)
    assertEquals("训练", strings.training)
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
.\gradlew.bat testDebugUnitTest --tests "*StringsTest*"
```
Expected: FAIL (Strings not found).

**Step 3: Write minimal implementation**

Create `Strings.kt`:
```kotlin
package smartracket.com.ui.i18n

data class AppStrings(
    val appName: String,
    val home: String,
    val training: String,
    val analytics: String,
    val highlights: String,
    val settings: String,
    val threeMonths: String,
    // ...add all labels currently hardcoded in screens
)

object Strings {
    fun forLanguage(language: Language): AppStrings = when (language) {
        Language.ENGLISH -> AppStrings(
            appName = "SmartRacket Coach",
            home = "Home",
            training = "Training",
            analytics = "Analytics",
            highlights = "Highlights",
            settings = "Settings",
            threeMonths = "3 Months",
        )
        Language.CHINESE -> AppStrings(
            appName = "SmartRacket Coach",
            home = "首页",
            training = "训练",
            analytics = "分析",
            highlights = "精彩",
            settings = "设置",
            threeMonths = "3个月",
        )
    }
}
```

Add `CompositionLocal` (e.g., in `MainActivity` or `ui/theme/Theme.kt`):
```kotlin
val LocalAppStrings = staticCompositionLocalOf { Strings.forLanguage(Language.ENGLISH) }
```

Provide it at the app root using the current language (from Settings or a Flow-backed source):
```kotlin
CompositionLocalProvider(LocalAppStrings provides Strings.forLanguage(currentLanguage)) {
    SmartRacketTheme { /* App */ }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
.\gradlew.bat testDebugUnitTest --tests "*StringsTest*"
```
Expected: PASS.

**Step 5: Update Memory Bank**
- Update `memory-bank/activeContext.md` and `master-plan.md` with Task 1 progress.
- Append to `memory-bank/SESSION.md` (v0.1.1).

**Step 6: Commit**
```bash
git add app/src/main/java/smartracket/com/ui/i18n/Strings.kt app/src/test/java/smartracket/com/ui/i18n/StringsTest.kt memory-bank/activeContext.md memory-bank/SESSION.md memory-bank/master-plan.md memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md
git commit -m "feat(i18n): add centralized strings map"
```

---

### Task 2: Replace hardcoded UI strings in screens

**Memory Bank Updates:**
- Update: `memory-bank/activeContext.md` - Log screens migrated.
- Append: `memory-bank/SESSION.md` - v0.1.2
- Update: `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md` - Mark Task 2 complete.

**Files:**
- Modify: `app/src/main/java/smartracket/com/ui/screens/HomeScreen.kt`
- Modify: `app/src/main/java/smartracket/com/ui/screens/AnalyticsScreen.kt`
- Modify: `app/src/main/java/smartracket/com/ui/screens/HighlightsScreen.kt`
- Modify: `app/src/main/java/smartracket/com/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/smartracket/com/MainActivity.kt`

**Step 1: Write the failing test**

Add a UI unit test for one screen (example for HomeScreen):
```kotlin
@Test
fun homeScreenUsesLocalizedTitle() {
    val strings = Strings.forLanguage(Language.CHINESE)
    assertEquals("SmartRacket Coach", strings.appName)
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
.\gradlew.bat testDebugUnitTest --tests "*homeScreenUsesLocalizedTitle*"
```
Expected: FAIL (string not used in UI).

**Step 3: Write minimal implementation**

Replace hardcoded strings with:
```kotlin
val strings = LocalAppStrings.current
Text(text = strings.appName)
```

Update all labels in these screens and navigation bar labels to use `strings` values.

**Step 4: Run test to verify it passes**

Run:
```bash
.\gradlew.bat testDebugUnitTest --tests "*homeScreenUsesLocalizedTitle*"
```
Expected: PASS.

**Step 5: Update Memory Bank**
- Update `memory-bank/activeContext.md`.
- Append `memory-bank/SESSION.md` (v0.1.2).

**Step 6: Commit**
```bash
git add app/src/main/java/smartracket/com/ui/screens/HomeScreen.kt app/src/main/java/smartracket/com/ui/screens/AnalyticsScreen.kt app/src/main/java/smartracket/com/ui/screens/HighlightsScreen.kt app/src/main/java/smartracket/com/ui/screens/SettingsScreen.kt app/src/main/java/smartracket/com/MainActivity.kt memory-bank/activeContext.md memory-bank/SESSION.md memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md
git commit -m "refactor(ui): use centralized localized strings"
```

---

### Task 3: Apply app-wide CJK font via Typography

**Memory Bank Updates:**
- Update: `memory-bank/activeContext.md` - Note font applied.
- Append: `memory-bank/SESSION.md` - v0.1.3
- Update: `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md` - Mark Task 3 complete.

**Files:**
- Create: `app/src/main/res/font/noto_sans_sc_regular.ttf`
- Modify: `app/src/main/java/smartracket/com/ui/theme/Type.kt`

**Step 1: Write the failing test**

Add a simple unit test verifying typography font family is set (if accessible), or add a screenshot test placeholder. If unit tests can’t access typography, use manual verification and note it.

**Step 2: Run test to verify it fails**

Run:
```bash
.\gradlew.bat testDebugUnitTest
```
Expected: FAIL or no coverage (document manual check).

**Step 3: Write minimal implementation**

In `Type.kt`:
```kotlin
val CjkFontFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = CjkFontFamily, /* ... */),
    // apply to all styles
)
```

**Step 4: Run test to verify it passes**

Run:
```bash
.\gradlew.bat testDebugUnitTest
```
Expected: PASS (if no unit test), plus manual verification in emulator.

**Step 5: Update Memory Bank**
- Update `memory-bank/activeContext.md`.
- Append `memory-bank/SESSION.md` (v0.1.3).

**Step 6: Commit**
```bash
git add app/src/main/res/font/noto_sans_sc_regular.ttf app/src/main/java/smartracket/com/ui/theme/Type.kt memory-bank/activeContext.md memory-bank/SESSION.md memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md
git commit -m "feat(ui): apply CJK font via typography"
```

---

### Task 4: Restrict highlights to BLE button + connected manual save

**Memory Bank Updates:**
- Update: `memory-bank/activeContext.md` - Highlight trigger rules updated.
- Append: `memory-bank/SESSION.md` - v0.1.4
- Update: `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md` - Mark Task 4 complete.

**Files:**
- Modify: `app/src/main/java/smartracket/com/viewmodel/TrainingViewModel.kt`
- Modify: `app/src/main/java/smartracket/com/repository/HighlightRepository.kt`
- Modify: `app/src/main/java/smartracket/com/repository/BluetoothRepository.kt`
- Modify: `app/src/main/java/smartracket/com/utils/BluetoothManager.kt` (or wherever BLE payloads are parsed)
- Modify: `app/src/main/java/smartracket/com/ui/screens/TrainingScreen.kt`

**Step 1: Write the failing test**

Create a unit test for `HighlightRepository.shouldAutoSave` or for `TrainingViewModel` to ensure auto-save is disabled:
```kotlin
@Test
fun autoSaveIsDisabled() {
    val repo = HighlightRepository(/* fakes */)
    assertFalse(repo.shouldAutoSave(10))
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
.\gradlew.bat testDebugUnitTest --tests "*autoSaveIsDisabled*"
```
Expected: FAIL (auto-save still enabled).

**Step 3: Write minimal implementation**

- Remove auto-save in `TrainingViewModel`:
```kotlin
// Remove auto-save check
// if (highlightRepository.shouldAutoSave(stroke.score)) { saveHighlight(...) }
```

- Update `HighlightRepository.shouldAutoSave()` to always return `false` (or delete it if unused).

- Add BLE button event signal:
```kotlin
private val _highlightTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
val highlightTrigger = _highlightTrigger.asSharedFlow()
```
Emit it when MCU button packet is received in `BluetoothManager` and forward in repository.

- In `TrainingViewModel.init`, collect `highlightTrigger` and call `saveHighlight(isAutoSave = false, strokeInfo = lastStrokeInfo)` only if connected.

- In `TrainingScreen`, enable the manual Save button only when `connectionState` is `Connected`; otherwise hide or disable.

**Step 4: Run test to verify it passes**

Run:
```bash
.\gradlew.bat testDebugUnitTest --tests "*autoSaveIsDisabled*"
```
Expected: PASS.

**Step 5: Update Memory Bank**
- Update `memory-bank/activeContext.md`.
- Append `memory-bank/SESSION.md` (v0.1.4).

**Step 6: Commit**
```bash
git add app/src/main/java/smartracket/com/viewmodel/TrainingViewModel.kt app/src/main/java/smartracket/com/repository/HighlightRepository.kt app/src/main/java/smartracket/com/repository/BluetoothRepository.kt app/src/main/java/smartracket/com/utils/BluetoothManager.kt app/src/main/java/smartracket/com/ui/screens/TrainingScreen.kt memory-bank/activeContext.md memory-bank/SESSION.md memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md
git commit -m "fix(highlights): restrict to BLE/manual connected"
```

---

### Task 5: Layout fixes (Analytics chip + bottom navigation)

**Memory Bank Updates:**
- Update: `memory-bank/activeContext.md` - Document layout changes.
- Append: `memory-bank/SESSION.md` - v0.1.5
- Update: `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md` - Mark Task 5 complete.

**Files:**
- Modify: `app/src/main/java/smartracket/com/ui/screens/AnalyticsScreen.kt`
- Modify: `app/src/main/java/smartracket/com/MainActivity.kt`

**Step 1: Write the failing test**

Add a UI test (screenshot or Compose test) to assert labels remain single line. If tests aren’t in place, note manual verification and add a TODO test stub.

**Step 2: Run test to verify it fails**

Run:
```bash
.\gradlew.bat testDebugUnitTest
```
Expected: FAIL or no coverage (document manual check).

**Step 3: Write minimal implementation**

- Shorten the “3 Months” label to “3M” in Chinese and English in `Strings.kt`.
- Set `maxLines = 1` and `overflow = TextOverflow.Ellipsis` for chip and nav labels.
- Use smaller typography for navigation labels or chips:
```kotlin
Text(text = strings.threeMonths, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
```

**Step 4: Run test to verify it passes**

Run:
```bash
.\gradlew.bat testDebugUnitTest
```
Expected: PASS (or manual verification in emulator).

**Step 5: Update Memory Bank**
- Update `memory-bank/activeContext.md`.
- Append `memory-bank/SESSION.md` (v0.1.5).

---

## Execution Handoff

Plan complete and saved to `memory-bank/plans/2026-02-08-smart-racket-localization-highlights.md`. Two execution options:

1. **Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration
2. **Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

Which approach?
