# Master Plan

## Milestones
1. [x] Centralize localization and app-wide strings
2. [x] Apply app-wide CJK font
3. [x] Restrict highlights to BLE/manual (connected only)
4. [x] Fix Analytics chip + bottom nav label layout
5. [x] Hybrid Firebase sync (Room cache + cloud) for Galaxy Watch

## Upcoming Work
- [ ] Replace google-services.json placeholder with real Firebase project config
- [ ] Build Galaxy Watch companion app (Wear OS) to consume Firestore data
- [ ] Port training session display to watch face complications

## Completed
- [x] Centralize localization: CompositionLocal + AppStrings data class (EN/ZH-CN/ZH-TW)
- [x] All screens use LocalAppStrings.current (Home, Training, Analytics, Highlights, Settings)
- [x] CJK font: Samsung One + platform CJK fallback in Type.kt
- [x] Highlights restricted to BLE trigger + manual save (connected only)
- [x] Auto-save threshold UI removed from SettingsScreen
- [x] Analytics TabRow + FilterChip: maxLines=1 + TextOverflow.Ellipsis
- [x] Bottom nav labels: maxLines=1 + Ellipsis + labelSmall
- [x] Samsung One UI color alignment (v0.2.0)
- [x] Dark/Light/System theme toggle (v0.3.0)
- [x] BLE operation queue + reliability (v0.4.0)
- [x] Multi-device BLE support + BleDeviceProfile (v0.3.0)

---
*Last Updated: 2026-02-08*
