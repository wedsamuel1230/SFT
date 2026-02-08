# SmartRacket 技術白皮書

**文件版本:** 1.0.0  
**日期:** 2026-02-08  
**分類:** 技術盡職調查用文件 (Confidential)  
**作者:** SmartRacket Engineering Team

---

## 目錄

1. [系統摘要](#system-summary)
2. [功能規格詳解](#functional-specs)
3. [系統架構](#system-architecture)
4. [數據與隱私](#data-privacy)
5. [技術權衡與決策紀錄](#technical-tradeoffs)
6. [附錄：數據結構定義](#appendix-data-structures)

---

## 1. 系統摘要 {#system-summary}

SmartRacket 是一套基於慣性測量單元 (IMU) 的乒乓球揮拍分析系統。系統由三個層級組成：嵌入式邊緣設備 (Seeed Studio XIAO nRF52840 Sense)、Android 原生應用程式 (Kotlin/Jetpack Compose)、以及雲端同步層 (Firebase Firestore)。

系統的核心技術差異化在於：放棄了傳統的電腦視覺方案 (Computer Vision)，轉而採用 6 軸 IMU 感測器 (加速度計 + 陀螺儀) 結合 TinyML 推論的技術路線。此決策帶來三項工程優勢：

1. **環境無關性:** 不依賴光線、背景、攝影機角度等外部條件，室內外場地均可運作。
2. **成本壓縮:** 單套硬體物料成本約 100 港幣，相較於高速攝影機方案降低兩個數量級。
3. **隱私安全:** 不採集任何影像數據，從根本上消除視覺隱私風險。

系統目前支援 3 種動作分類 (idle / forehand / backhand)，推論完全在 MCU 端完成 (Edge Impulse TinyML)，延遲低於 20 毫秒，附帶信心分數 (Confidence Score, 0.0-1.0) 用於品質門檻控制。手機端負責接收分類結果、評分計算、數據儲存與 UI 呈現，不執行 ML 推論。未來版本計畫擴展至 14 種擊球動作精細分類。

---

## 2. 功能規格詳解 {#functional-specs}

### 2.1 功能：即時動作分類與評分

- **Trigger:** BLE Notify 特徵值更新。nRF52840 MCU 完成一次揮拍偵測後，透過 GATT Notify 將 `McuModelOutput` JSON 封包推送至 Android 端。
- **Backend Logic:**
  1. nRF52840 MCU 上的 Edge Impulse TinyML 模型完成 6 軸 IMU 數據推論，分類為 idle / forehand / backhand 三類之一。
  2. MCU 將推論結果封裝為 `McuModelOutput` JSON，透過 BLE Notify 推送至 Android 端。
  3. `BluetoothManager` 接收 BLE Notify 回調，解析 JSON 為 `McuModelOutput` 物件 (欄位: `ts`, `stroke`, `score`, `conf`, `peak`)。
  4. Android 端根據 MCU 分類結果與信心分數，執行評分邏輯與回饋文字生成 (規則引擎，非 ML 推論)。
  5. 結果寫入 Room 資料庫 `strokes` 表，關聯至當前 `TrainingSession`。
  6. UI 層透過 `StateFlow` 即時更新畫面。
- **Edge Cases:**
  - 信心分數 < 0.5 時，分類結果標記為 `UNKNOWN`，不納入平均分計算，UI 提示信號品質不足。
  - BLE 連線中斷 (GATT Status != 0) 時，`BleOperationQueue` 自動重試並回退至裝置快取重連。
  - 快速連續揮拍（間隔 < 200ms）時，MCU 端進行 debounce 過濾，避免重複觸發。
  - MTU 協商失敗時，回退至預設 23 bytes MTU，分片傳輸 IMU 數據。
- **Data Structure:**
  ```
  McuModelOutput {
    ts: Long (Unix ms),
    stroke: String,
    score: Int (1-10),
    conf: Float (0.0-1.0),
    peak: Float (m/s^2)
  }
  
  Stroke (Room Entity) {
    strokeId: Long (PK, auto),
    sessionId: Long (FK → training_sessions),
    timestamp: Long,
    strokeType: String,
    score: Int,
    motionData: MotionData,
    feedback: String,
    confidence: Float,
    peakAcceleration: Float?,
    strokeDuration: Long?
  }
  ```

### 2.2 功能：訓練歷史追蹤與進步曲線

- **Trigger:** 使用者結束訓練 (`SessionState.ACTIVE` → `SessionState.COMPLETED`)，或導航至分析頁面 (AnalyticsScreen)。
- **Backend Logic:**
  1. `TrainingRepository` 查詢 Room `training_sessions` 表，按時間降序返回歷史記錄。
  2. 對每個 session 聚合 strokes 數據：平均分、總擊球數、擊球類型分佈。
  3. `AnalyticsScreen` 使用 MPAndroidChart 繪製趨勢圖（折線圖 + 柱狀圖）。
  4. 支援時間軸篩選：3 個月 / 6 個月 / 1 年 / 全部。
  5. 擊球類型篩選透過 `FilterChip` 實現 (Material3)。
- **Edge Cases:**
  - 訓練時長 < 30 秒的 session 標記為無效，不納入趨勢計算。
  - CJK 字型下 Tab 標籤設定 `maxLines=1 + TextOverflow.Ellipsis` 防止換行溢位。
  - 無歷史數據時顯示空狀態提示，引導使用者開始首次訓練。
- **Data Structure:**
  ```
  TrainingSession (Room Entity) {
    sessionId: Long (PK, auto),
    startTime: Long,
    endTime: Long?,
    totalDuration: Long,
    avgScore: Float,
    totalStrokes: Int,
    heartRateData: List<HeartRateReading>,
    avgHeartRate: Int?,
    maxHeartRate: Int?,
    caloriesBurned: Float?,
    notes: String?,
    isSynced: Boolean
  }
  
  SessionSummary {
    sessionId: Long,
    date: Long,
    duration: Long,
    ...
  }
  ```

### 2.3 功能：傷害預警系統

- **Trigger:** 單次揮拍的 `peakAcceleration` 超過安全閾值 (可配置，預設 25 m/s^2)，或連續 N 次揮拍的手腕角度偏差值持續高於警示線。
- **Backend Logic:**
  1. 從 `MotionData` 中提取加速度峰值與角速度變化率。
  2. 與訓練模型中的標準姿勢模板比對，計算關節力矩偏差 (Variance)。
  3. 短期警告：單次偏差 > 閾值，觸發震動回饋 + UI Toast 提示。
  4. 長期警告：分析近 7 天 session 中的姿勢偏差趨勢，若連續 3 次以上 session 平均偏差上升，推送「運動傷害風險提醒」通知。
- **Edge Cases:**
  - IMU 數據中出現離群值 (Outlier) 時（如球拍掉落），使用 Z-Score 濾波排除。
  - 使用者關閉預警後，不再推送同類型通知至當前 session 結束。
  - 裝置電量低 (< 15%) 時，降低採樣頻率以延長使用時間，同時提示數據精度可能降低。
- **Data Structure:**
  ```
  InjuryAlert {
    alertType: Enum (ACUTE | CHRONIC),
    bodyPart: String ("wrist" | "elbow" | "waist"),
    severity: Int (1-5),
    triggerStrokeId: Long,
    suggestion: String
  }
  ```

### 2.4 功能：高光一刻 (Highlight Capture)

- **Trigger:** 兩種觸發路徑：
  1. BLE 按鈕事件：MCU 端物理按鈕觸發，透過 Control Characteristic 發送指令。
  2. 手動儲存：使用者在訓練畫面點擊保存按鈕 (僅 BLE 連線狀態下可用)。
- **Backend Logic:**
  1. 系統維護最近 10 分鐘的 `Stroke` 環形緩衝區 (Ring Buffer)。
  2. 觸發時，截取觸發點前後 N 秒的 strokes 數據，打包為 `HighlightClip`。
  3. 生成元資料 (`HighlightMetadata`)：最佳擊球分數、動作類型、統計摘要。
  4. 寫入 Room `highlight_clips` 表。
  5. 雲端同步時推送至 Firestore `highlights` 子集合。
- **Edge Cases:**
  - BLE 未連線時，手動保存按鈕為 disabled 狀態 (UI 灰顯)。
  - 緩衝區中擊球數 < 3 時，提示「數據不足，無法生成精彩片段」。
  - 同一個 session 中的重複觸發需間隔至少 5 秒，防止重複儲存。
- **Data Structure:**
  ```
  HighlightClip (Room Entity) {
    clipId: Long (PK, auto),
    sessionId: Long (FK),
    clipStartTime: Long,
    clipEndTime: Long,
    thumbnailUri: String?,
    metadata: HighlightMetadata,
    isAutoSaved: Boolean,
    isSynced: Boolean
  }
  
  HighlightMetadata {
    bestScore: Int,
    strokeCount: Int,
    dominantStrokeType: String,
    avgConfidence: Float
  }
  ```

### 2.5 功能：雲端同步與 Galaxy Watch 支援

- **Trigger:** 三種同步時機：
  1. 定時同步：WorkManager 排程每 15 分鐘執行一次 (`SyncWorker`)。
  2. 即時同步：使用者在設定頁面點擊「立即同步」按鈕。
  3. Watch 觸發：`WearableListenerService` 收到 `/smartracket/sync` 路徑訊息時觸發。
- **Backend Logic:**
  1. `SyncManager` 檢查網路可用性 (NetworkConstraint)。
  2. `FirebaseSyncRepository` 查詢 Room 中 `isSynced = false` 的已完成 sessions。
  3. 匿名 Firebase Auth 取得 UID。
  4. 按 session 為單位推送：session 文件 → strokes 子集合 (每批 400 筆) → highlights 子集合。
  5. 成功後更新 Room 中 `isSynced = true`。
  6. 同步狀態透過 `StateFlow<SyncState>` 暴露給 UI (Idle / Syncing / Success / Error / FirebaseUnavailable)。
- **Edge Cases:**
  - `google-services.json` 為佔位檔時，`isFirebaseAvailable()` 返回 false，不嘗試同步，UI 顯示「Firebase 未設定」狀態。
  - Strokes 批次寫入超過 Firestore 500 筆限制時，自動分批 (chunk size = 400)。
  - 網路中斷時 WorkManager 自動延遲重試，指數退避。
  - 匿名帳號遷移：未來支援帳號綁定時，需使用 Firebase Auth linking API。
- **Data Structure:**
  ```
  Firestore Schema:
  users/{uid}/
    sessions/{sessionId}/        ← session document
      strokes/                   ← subcollection
      highlights/                ← subcollection
  
  SyncState: Idle | Syncing | Success(n) | Error(msg) | FirebaseUnavailable
  ```

### 2.6 功能：Samsung Health 整合

- **Trigger:** 訓練開始時自動請求 Health Connect 權限，開始讀取心率感測器數據。
- **Backend Logic:**
  1. `HealthRepository` 透過 Health Connect API 查詢 Galaxy Watch / 手機心率感測器。
  2. 定期讀取 (每 5 秒) 心率數據，打包為 `HeartRateReading(timestamp, bpm)`。
  3. 訓練結束時聚合：平均心率、最大心率、估算卡路里消耗。
  4. 寫入 `TrainingSession.heartRateData` 列表。
- **Edge Cases:**
  - Health Connect 未安裝或權限被拒時，心率相關欄位為 null，UI 隱藏心率區塊。
  - Galaxy Watch 未配對時，回退至手機端感測器 (若有)。
  - 異常心率值 (< 30 或 > 220 BPM) 過濾不寫入。

---

## 3. 系統架構 {#system-architecture}

### 3.1 三層架構概覽

```
┌─────────────────────────────────────────────────────────────┐
│                    Layer 3: Cloud                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Firestore   │  │  Firebase    │  │  Cloud       │      │
│  │  Database    │  │  Auth        │  │  Functions   │      │
│  │  (NoSQL)     │  │  (Anonymous) │  │  (Future)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────┬──────────────────────────────────────────────────┘
           │ HTTPS/gRPC (TLS 1.3)
┌──────────┴──────────────────────────────────────────────────┐
│                 Layer 2: Mobile (Android)                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ Compose  │ │ ViewModel│ │ Score    │ │ Room DB  │      │
│  │ UI       │ │ (Hilt)   │ │ Engine   │ │ (SQLite) │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ BLE Mgr  │ │ Sync     │ │ Health   │ │ WorkMgr  │      │
│  │          │ │ Manager  │ │ Connect  │ │          │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
└──────────┬──────────────────────────────────────────────────┘
           │ BLE 5.0 (GATT Profile)
┌──────────┴──────────────────────────────────────────────────┐
│               Layer 1: Edge (nRF52840 Sense)                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ IMU      │ │ Edge     │ │ BLE      │ │ Button   │      │
│  │ 6-axis   │ │ Impulse  │ │ GATT     │ │ I/O      │      │
│  │ LSM6DS3  │ │ TinyML   │ │ Server   │ │          │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 BLE GATT Profile 規格

| Characteristic | UUID | 方向 | 用途 |
|---|---|---|---|
| IMU Data | `beb5483e-...-26a8` | MCU → Phone (Notify) | 6 軸 IMU 原始數據與模型推論結果 |
| Control | `beb5483e-...-26a9` | Phone → MCU (Write) | 控制指令 (校準、重置、模式切換) |
| Battery | `beb5483e-...-26aa` | MCU → Phone (Read) | 電池電量百分比 |
| Service UUID | `4fafc201-...-914b` | — | GATT Service 容器 |

**BLE 操作可靠性機制:**
- `BleOperationQueue`: FIFO 佇列序列化所有 GATT 操作，避免 Android BLE 並發問題。
- 裝置快取 (`BluetoothDevice` cache): 減少重新掃描開銷。
- MTU 協商: 請求最大 MTU 以減少分片次數。
- 連線優先級: 訓練模式設定為 `CONNECTION_PRIORITY_HIGH`。

### 3.3 ML 模型部署策略

系統目前採用 MCU 端單級推論架構 (Edge-Only Inference)。所有 ML 推論在 nRF52840 Sense 上完成，手機端僅負責數據接收、評分計算與 UI 呈現。

**MCU 端推論 (nRF52840 + Edge Impulse):**
- 平台: Edge Impulse Studio (雲端訓練 → 匯出 C++ 推論庫)
- 模型大小: < 50KB (量化 INT8)
- 分類類別: 3 類 (idle / forehand / backhand)
- 延遲: < 20ms
- 採樣頻率: 100Hz (6 軸 IMU)
- 用途: 揮拍偵測、動作分類、信心分數輸出

**Phone 端 (Android — 無 ML 推論):**
- 角色: 接收 MCU 分類結果 (BLE Notify)，執行評分邏輯 (規則引擎)，生成回饋文字，儲存至 Room，更新 UI。
- 依賴: TensorFlow Lite 2.14.0 已包含於 build.gradle 中，為未來 Phone 端推論預留，當前版本未啟用。

**決策理由:**
- MCU 端 Edge Impulse 推論延遲極低 (< 20ms)，滿足即時回饋需求。
- 當前 3 類分類模型體積小，nRF52840 (Cortex-M4F, 64MHz, 256KB RAM) 足以承載。
- 避免 BLE 傳輸原始 IMU 數據的頻寬瓶頸 (6 軸 x 100Hz = 2.4KB/s)。
- Phone 端 TFLite 依賴保留，為未來擴展至 14 類精細分類做技術儲備。

**未來演進路徑 (Roadmap):**
1. Phase 1 (當前): MCU 端 3 類分類 → 手機接收結果。
2. Phase 2: 擴展 MCU 模型至 6-8 類 (加入 chop/push/serve)。
3. Phase 3: 啟用 Phone 端 TFLite 進行精細 14 類二次分類 (混合推論)。

### 3.4 Android 應用架構

```
Presentation Layer (Compose UI)
    │
    ├── HomeScreen
    ├── TrainingScreen (One UI: Top 35% View / Bottom 65% Interaction)
    ├── AnalyticsScreen (MPAndroidChart)
    ├── HighlightsScreen
    └── SettingsScreen (Theme/Language/Sync/Devices)
    │
    ▼
ViewModel Layer (Hilt-injected)
    │
    ├── TrainingViewModel
    ├── AnalyticsViewModel
    ├── HighlightsViewModel
    └── SettingsViewModel
    │
    ▼
Repository Layer
    │
    ├── TrainingRepository      ← Room (sessions + strokes)
    ├── BluetoothRepository     ← BLE operations
    ├── HighlightRepository     ← Room (highlights)
    ├── HealthRepository        ← Health Connect API
    └── FirebaseSyncRepository  ← Firestore read/write
    │
    ▼
Data Layer
    │
    ├── Room Database (SmartRacketDatabase)
    │   ├── training_sessions (table)
    │   ├── strokes (table, FK → sessions)
    │   └── highlight_clips (table, FK → sessions)
    │
    ├── DataStore (preferences: theme, language, sync toggle)
    │
    └── Firebase Firestore (cloud mirror)
```

**依賴注入:** Hilt/Dagger2，全域 Singleton scope 管理 Repository、BluetoothManager、Database 實例。

**國際化 (i18n):** CompositionLocal + `AppStrings` data class，支援 EN / ZH-CN / ZH-TW。運行時切換，無需重啟 Activity。

**主題:** Samsung One UI 設計規範，Samsung Blue 色系 (#1428A0 / #A6ADDB)，支援 System / Light / Dark 三種模式。

### 3.5 資料庫 Schema

```sql
-- Room Database: smart_racket_db

CREATE TABLE training_sessions (
    sessionId     INTEGER PRIMARY KEY AUTOINCREMENT,
    startTime     INTEGER NOT NULL,
    endTime       INTEGER,
    totalDuration INTEGER DEFAULT 0,
    avgScore      REAL DEFAULT 0,
    totalStrokes  INTEGER DEFAULT 0,
    heartRateData TEXT,           -- JSON: List<HeartRateReading>
    avgHeartRate  INTEGER,
    maxHeartRate  INTEGER,
    caloriesBurned REAL,
    notes         TEXT,
    isSynced      INTEGER DEFAULT 0
);

CREATE TABLE strokes (
    strokeId         INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId        INTEGER NOT NULL REFERENCES training_sessions(sessionId) ON DELETE CASCADE,
    timestamp        INTEGER NOT NULL,
    strokeType       TEXT NOT NULL,
    score            INTEGER NOT NULL,
    motionData       TEXT NOT NULL,  -- JSON: MotionData
    feedback         TEXT NOT NULL,
    confidence       REAL DEFAULT 0,
    peakAcceleration REAL,
    strokeDuration   INTEGER
);
CREATE INDEX idx_strokes_session ON strokes(sessionId);

CREATE TABLE highlight_clips (
    clipId        INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId     INTEGER NOT NULL REFERENCES training_sessions(sessionId) ON DELETE CASCADE,
    clipStartTime INTEGER NOT NULL,
    clipEndTime   INTEGER NOT NULL,
    thumbnailUri  TEXT,
    metadata      TEXT NOT NULL,    -- JSON: HighlightMetadata
    isAutoSaved   INTEGER DEFAULT 0,
    isSynced      INTEGER DEFAULT 0
);
CREATE INDEX idx_highlights_session ON highlight_clips(sessionId);
```

---

## 4. 數據與隱私 {#data-privacy}

### 4.1 數據分類

| 數據類型 | 敏感等級 | 儲存位置 | 保留期間 |
|---|---|---|---|
| IMU 動作數據 (加速度/角速度) | 低 | Room (本地) + Firestore (雲端) | 使用者可刪除 |
| 訓練評分與統計 | 低 | Room + Firestore | 使用者可刪除 |
| 心率數據 | 中 | Room (嵌入 session) | 隨 session 刪除 |
| 匿名 UID | 低 | Firebase Auth | 帳號存續期間 |
| 裝置配對資訊 (BLE MAC) | 中 | DataStore (本地) | 使用者可清除 |

### 4.2 隱私合規策略

**GDPR 合規 (適用於歐盟市場拓展):**

1. **數據最小化原則 (Data Minimization):**
   - 系統僅採集 IMU 感測器數據 (6 軸浮點數列)。
   - 不採集任何影像、音訊、位置或個人身份資訊 (PII)。
   - 心率數據透過 Health Connect 標準 API 讀取，受 Android 權限系統保護。

2. **使用者同意與控制:**
   - 雲端同步為 opt-in (預設關閉)，使用者可在 SettingsScreen 啟用/停用。
   - BLE 配對需使用者主動操作。
   - Health Connect 權限需使用者顯式授權。

3. **數據可攜性 (Data Portability):**
   - Room 資料庫支援本地匯出 (CSV/JSON)。
   - Firestore 數據可透過 Firebase Admin SDK 匯出。

4. **刪除權 (Right to Erasure):**
   - 設定頁面提供「清除所有數據」功能。
   - 匿名帳號刪除時，Firestore 數據隨之清除 (透過 Cloud Functions 觸發)。

5. **兒童隱私 (COPPA 合規):**
   - 匿名認證不採集任何年齡相關資訊。
   - 未來帳號系統需新增年齡門檻驗證。

### 4.3 加密與傳輸安全

```
┌───────────┐     BLE 5.0 AES-CCM     ┌───────────┐
│  nRF52840 │ ◄──────────────────────► │  Android  │
│  (Edge)   │    Link Layer Encrypt    │  (Phone)  │
└───────────┘                          └─────┬─────┘
                                             │
                                      TLS 1.3 (gRPC)
                                             │
                                      ┌──────┴──────┐
                                      │  Firebase   │
                                      │  Firestore  │
                                      └─────────────┘
```

| 層級 | 加密方式 | 標準 |
|---|---|---|
| BLE 傳輸 | AES-CCM (BLE 5.0 LE Secure Connections) | Bluetooth Core Spec 5.0 |
| 本地儲存 | Android Full Disk Encryption (FDE/FBE) | Android 10+ 強制要求 |
| 雲端傳輸 | TLS 1.3 (Firebase SDK 內建) | RFC 8446 |
| 雲端靜態 | Google Cloud AES-256 (Firebase 預設) | FIPS 140-2 |

### 4.4 匿名認證架構

```
User Install App
      │
      ▼
  [First Launch]
      │
      ▼
  Firebase Anonymous Auth
      │
      ├── Success → UID generated (e.g., "abc123xyz")
      │              No email, no password, no PII
      │
      └── Failure → Offline mode (Room only, no sync)
```

設計決策：採用匿名認證而非帳號系統，根據產品階段 (MVP) 做出的權衡。優點是零摩擦的使用者體驗，缺點是裝置遺失時數據無法恢復。未來版本將支援帳號綁定 (Google Sign-In)，透過 Firebase Auth Linking API 將匿名帳號升級為永久帳號。

---

## 5. 技術權衡與決策紀錄 {#technical-tradeoffs}

### 5.1 即時性 vs. 準確度

此為系統最關鍵的工程權衡。

**當前架構：MCU 端單級推論 (Edge-Only)**

| 維度 | 當前狀態 (MCU-Only) | 未來目標 (Hybrid) |
|---|---|---|
| 推論位置 | nRF52840 MCU | MCU + Phone |
| 框架 | Edge Impulse | Edge Impulse + TFLite |
| 分類數 | 3 類 (idle/forehand/backhand) | 14 類精細分類 |
| 延遲 | < 20ms (MCU) + BLE 傳輸 | < 100ms (含二次推論) |
| 模型大小 | < 50KB (INT8) | MCU < 50KB + Phone 1-5MB |
| 離線能力 | 完全離線 | 完全離線 |

**當前數據流:**

```
揮拍發生 → [MCU: 20ms] Edge Impulse 推論 (3 類分類 + 信心分數)
         → BLE Notify (McuModelOutput JSON)
         → [Phone] 接收結果 → 規則引擎評分 → Room 儲存 → UI 更新
         總延遲: < 50ms (MCU 推論 + BLE 傳輸)
```

人類對觸覺/視覺回饋的感知閾值約為 150-200ms。系統總延遲控制在 50ms 以內，遠低於人類感知閾值。

**權衡分析：為何選擇 Edge-Only 而非 Hybrid:**
1. 3 類分類模型體積足夠小，MCU 端可完整承載，無需手機輔助。
2. 避免傳輸原始 IMU 數據的 BLE 頻寬壓力，僅傳輸分類結果 (< 100 bytes/拍)。
3. Edge Impulse 平台提供端到端的數據採集 → 訓練 → 部署流程，降低 ML 工程複雜度。
4. 隨著訓練數據累積與模型擴展，未來可無縫遷移至 Hybrid 架構。

### 5.2 IMU vs. Computer Vision

| 維度 | IMU 方案 (選定) | Computer Vision 方案 (棄用) |
|---|---|---|
| 硬體成本 | ~100 HKD | > 5,000 HKD (高速攝影機) |
| 環境依賴 | 無 | 光線、角度、背景 |
| 隱私風險 | 無影像 | 影像數據需 GDPR 處理 |
| 部署複雜度 | 嵌入球拍手柄 | 需固定架設攝影機 |
| 分析維度 | 力度/速度/角度/角速度 | 全身姿勢/軌跡 |
| 可擴展性 | 高 (同軸 IMU 可套用至其他球拍) | 低 (需重新訓練視覺模型) |

### 5.3 Room + Firebase vs. 純雲端

選擇 Room 作為 Single Source of Truth，Firebase 作為同步鏡像，而非純雲端儲存。

**理由:**
1. 乒乓球訓練場地網路環境不穩定（體育館、室外）。
2. 訓練過程中每秒可能產生 3-5 筆 Stroke 記錄，直接寫入雲端會產生大量 Firestore 寫入操作 (計費)。
3. Room 本地寫入延遲 < 5ms，Firestore 寫入延遲 > 200ms。
4. 離線優先架構確保核心功能在斷網環境下完全可用。

---

## 6. 附錄：數據結構定義 {#appendix-data-structures}

### 6.1 完整 StrokeType 列舉

| 名稱 | 顯示名 | 說明 |
|---|---|---|
| FOREHAND_LOOP | Forehand Loop | 正手弧圈球 (上旋攻擊) |
| FOREHAND_DRIVE | Forehand Drive | 正手快攻 (平擊) |
| FOREHAND_FLICK | Forehand Flick | 正手挑打 (台內) |
| BACKHAND_LOOP | Backhand Loop | 反手弧圈球 |
| BACKHAND_DRIVE | Backhand Drive | 反手快攻 |
| BACKHAND_FLICK | Backhand Flick | 反手挑打 |
| FOREHAND_BLOCK | Forehand Block | 正手擋球 (防守) |
| BACKHAND_BLOCK | Backhand Block | 反手擋球 |
| FOREHAND_CHOP | Forehand Chop | 正手削球 (下旋防守) |
| BACKHAND_CHOP | Backhand Chop | 反手削球 |
| FOREHAND_PUSH | Forehand Push | 正手搓球 (台內短下旋) |
| BACKHAND_PUSH | Backhand Push | 反手搓球 |
| SERVE | Serve | 發球 |
| UNKNOWN | Unknown | 未分類 (信心不足) |

### 6.2 系統需求

| 項目 | 規格 |
|---|---|
| Android 最低版本 | API 26 (Android 8.0 Oreo) |
| Android 目標版本 | API 35 (Android 15) |
| Kotlin 版本 | 2.0.21 |
| JVM Target | 17 |
| Compose BOM | 2024.11.00 |
| TensorFlow Lite | 2.14.0 (Phone 端預留，當前未啟用推論) |
| Edge Impulse SDK | 最新版 (MCU 端推論) |
| Firebase BOM | 33.7.0 |
| BLE 規格 | Bluetooth 5.0+ |
| MCU | Seeed Studio XIAO nRF52840 Sense |
| IMU | LSM6DS3 6-axis (on-board) |

---

*文件結束。本文件供技術盡職調查使用，包含的技術細節均基於當前代碼庫實際實作。*
