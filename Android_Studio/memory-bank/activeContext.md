# Active Context

## Current Focus
Plan implementation for centralized localization, app-wide CJK font, BLE-only highlights, and UI layout fixes.

## Open Questions / Blockers
- Locate exact BLE MCU button event parsing in BluetoothManager to emit highlight trigger.
- Confirm TrainingScreen file path/structure for manual save button gating.

## Notes
- Use centralized `AppStrings` map keyed by `Language` and provide via CompositionLocal.
- Remove auto-save highlights; trigger only via BLE button and manual save when connected.
- Tighten labels (e.g., "3M") and enforce single-line/ellipsis to fix S25 layout.

---
*Last Updated: 2026-02-08*
