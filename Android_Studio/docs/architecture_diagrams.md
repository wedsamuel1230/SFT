# SmartRacket Architecture Diagrams

This document contains Mermaid diagrams for the SmartRacket Coach app architecture.

## MVVM Architecture Overview

```mermaid
graph TB
    subgraph "UI Layer (Compose)"
        HS[HomeScreen]
        TS[TrainingScreen]
        AS[AnalyticsScreen]
        HLS[HighlightsScreen]
        SS[SettingsScreen]
    end

    subgraph "ViewModel Layer"
        HVM[HomeViewModel]
        TVM[TrainingViewModel]
        AVM[AnalyticsViewModel]
        HLVM[HighlightsViewModel]
        SVM[SettingsViewModel]
    end

    subgraph "Repository Layer"
        BR[BluetoothRepository]
        TR[TrainingRepository]
        HR[HighlightRepository]
        HLR[HealthRepository]
    end

    subgraph "Data Layer"
        BM[BluetoothManager]
        SC[StrokeClassifier]
        DB[(Room Database)]
        HC[Health Connect]
    end

    HS --> HVM
    TS --> TVM
    AS --> AVM
    HLS --> HLVM
    SS --> SVM

    HVM --> BR
    HVM --> TR
    HVM --> HLR
    
    TVM --> BR
    TVM --> TR
    TVM --> HR
    TVM --> HLR
    
    AVM --> TR
    
    HLVM --> HR
    
    SVM --> BR
    SVM --> HLR

    BR --> BM
    TR --> DB
    TR --> SC
    HR --> DB
    HLR --> HC

    style HS fill:#e1f5fe
    style TS fill:#e1f5fe
    style AS fill:#e1f5fe
    style HLS fill:#e1f5fe
    style SS fill:#e1f5fe
    
    style HVM fill:#fff3e0
    style TVM fill:#fff3e0
    style AVM fill:#fff3e0
    style HLVM fill:#fff3e0
    style SVM fill:#fff3e0
    
    style BR fill:#e8f5e9
    style TR fill:#e8f5e9
    style HR fill:#e8f5e9
    style HLR fill:#e8f5e9
    
    style BM fill:#fce4ec
    style SC fill:#fce4ec
    style DB fill:#fce4ec
    style HC fill:#fce4ec
```

## Bluetooth State Machine

```mermaid
stateDiagram-v2
    [*] --> Disconnected
    
    Disconnected --> Scanning: startScan()
    Scanning --> Disconnected: stopScan() / timeout
    Scanning --> Connecting: device found
    
    Connecting --> Connected: GATT connected
    Connecting --> Disconnected: connection failed
    Connecting --> Error: timeout / error
    
    Connected --> Disconnected: disconnect()
    Connected --> Connecting: connection lost (auto-reconnect)
    Connected --> Error: communication error
    
    Error --> Disconnected: clearError()
    Error --> Scanning: retry()
    
    note right of Scanning
        Scans for SmartRacket
        devices using BLE
        Service UUID filter
    end note
    
    note right of Connected
        Enables notifications
        for IMU characteristic
        Receives data at 20Hz
    end note
    
    note right of Connecting
        Up to 5 reconnect
        attempts with 2s delay
    end note
```

## Real-Time Data Flow Pipeline

```mermaid
flowchart LR
    subgraph ESP32["ESP32 Paddle"]
        IMU[IMU Sensor]
        BLE_TX[BLE Transmitter]
    end

    subgraph Android["Android App"]
        subgraph BLE["Bluetooth Layer"]
            BM[BluetoothManager]
            PARSE[Packet Parser]
        end
        
        subgraph Processing["Processing Layer"]
            BUFFER[IMU Buffer]
            DETECT[Stroke Detector]
            TFL[TensorFlow Lite]
        end
        
        subgraph Storage["Storage Layer"]
            ROOM[(Room DB)]
            HIGHLIGHT[Highlight Buffer]
        end
        
        subgraph UI["UI Layer"]
            SCORE[Score Display]
            FEEDBACK[Feedback Tips]
            TIMER[Timer/Stats]
        end
    end

    IMU -->|50-100ms| BLE_TX
    BLE_TX -->|BLE Notify| BM
    BM --> PARSE
    PARSE --> BUFFER
    BUFFER --> DETECT
    DETECT -->|Motion Data| TFL
    TFL -->|Classification| ROOM
    TFL -->|Score + Type| SCORE
    TFL -->|Feedback| FEEDBACK
    BUFFER --> HIGHLIGHT
    DETECT --> TIMER

    style IMU fill:#ffcdd2
    style BLE_TX fill:#ffcdd2
    style BM fill:#c8e6c9
    style PARSE fill:#c8e6c9
    style TFL fill:#fff9c4
    style SCORE fill:#bbdefb
    style FEEDBACK fill:#bbdefb
```

## Training Session Flow

```mermaid
sequenceDiagram
    participant User
    participant UI as TrainingScreen
    participant VM as TrainingViewModel
    participant BR as BluetoothRepo
    participant TR as TrainingRepo
    participant SC as StrokeClassifier
    participant DB as Room Database

    User->>UI: Tap "Start Training"
    UI->>VM: startSession()
    VM->>TR: startSession()
    TR->>DB: Insert TrainingSession
    DB-->>TR: sessionId
    TR-->>VM: TrainingSession
    VM->>UI: Update state to ACTIVE

    loop Every detected stroke
        BR->>VM: detectedStrokes.emit(motionData)
        VM->>TR: recordStroke(sessionId, motionData)
        TR->>SC: classify(motionData)
        SC-->>TR: StrokeClassificationResult
        TR->>DB: Insert Stroke
        TR-->>VM: Stroke
        VM->>UI: Update score, feedback, count
    end

    User->>UI: Tap "Stop"
    UI->>VM: stopSession()
    VM->>TR: endSession(sessionId)
    TR->>DB: Update TrainingSession
    TR-->>VM: Final stats
    VM->>UI: Show summary
```

## Highlight Capture Flow

```mermaid
flowchart TD
    START[Stroke Detected]
    BUFFER[Add to Circular Buffer<br/>3 min capacity]
    CHECK{Score >= 8?}
    MANUAL{User tapped<br/>Save Highlight?}
    CREATE[Create HighlightClip]
    EXTRACT[Extract motion data<br/>around timestamp]
    METADATA[Generate metadata<br/>score, type, time]
    SAVE[Save to Database]
    NOTIFY[Show save confirmation]
    END[Continue training]

    START --> BUFFER
    BUFFER --> CHECK
    CHECK -->|Yes| CREATE
    CHECK -->|No| MANUAL
    MANUAL -->|Yes| CREATE
    MANUAL -->|No| END
    CREATE --> EXTRACT
    EXTRACT --> METADATA
    METADATA --> SAVE
    SAVE --> NOTIFY
    NOTIFY --> END

    style CHECK fill:#fff9c4
    style MANUAL fill:#fff9c4
    style CREATE fill:#c8e6c9
    style SAVE fill:#c8e6c9
```

## Health Connect Integration

```mermaid
flowchart LR
    subgraph Watch["Galaxy Watch"]
        HR_SENSOR[Heart Rate Sensor]
        WEAR_APP[Wear OS App]
    end

    subgraph Phone["Android Phone"]
        subgraph HC["Health Connect"]
            HR_DATA[(Heart Rate Records)]
            EX_DATA[(Exercise Records)]
        end
        
        subgraph App["SmartRacket Coach"]
            HEALTH_REPO[HealthRepository]
            TRAINING_VM[TrainingViewModel]
            ANALYTICS[Analytics Dashboard]
        end
    end

    HR_SENSOR --> WEAR_APP
    WEAR_APP --> HR_DATA
    HR_DATA --> HEALTH_REPO
    HEALTH_REPO --> TRAINING_VM
    TRAINING_VM --> EX_DATA
    HEALTH_REPO --> ANALYTICS

    style HR_SENSOR fill:#ffcdd2
    style HC fill:#e1f5fe
    style App fill:#e8f5e9
```

---

To render these diagrams:
1. Use a Mermaid-compatible Markdown viewer
2. Or paste into https://mermaid.live
3. Or use VS Code with Mermaid extension

