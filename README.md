# AI-Powered Music Recommendation Middleware

[![Android](https://img.shields.io/badge/Platform-Android%20API%2026%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room%20(SQLite)-003B57?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini%20API-8E75B2?logo=google&logoColor=white)](https://ai.google.dev)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An **on-device, AI-powered music recommendation middleware** designed to recreate the legendary **Resso-style zero-skip listening experience** on top of existing music streaming platforms (**Apple Music**, **YouTube Music**, and **Spotify**).

---

## 🎯 The Core Problem & Vision

Streaming platforms provide lossless audio and massive catalogs, but their default algorithmic queues often produce jarring transitions—requiring users to repeatedly press **Skip** to find a song that matches their current listening flow.

**Our application does not replace your streaming apps or host audio files.**

Instead, it runs natively on Android as an intelligent **observation, learning, and recommendation middleware**:

```text
                    USER
                     │
                     ▼
             ┌───────────────┐
             │   OUR APP     │
             │               │
             │ AI Music      │
             │ Recommendation│
             │ Engine (Room) │
             └───────┬───────┘
                     │
       "playFromSearch / playFromUri"
                     │
                     ▼
             ┌───────────────┐
             │ IMusicProvider│
             │ (MediaSession)│
             └───────┬───────┘
                     │
                     ▼
             ┌───────────────┐
             │ TARGET PLAYER │
             │               │
             │ • Apple Music │
             │ • YT Music    │
             │ • Spotify     │
             └───────────────┘
```

The system continuously executes the core foundation loop:

$$\text{PLAY SONG} \longrightarrow \text{OBSERVE} \longrightarrow \text{LEARN} \longrightarrow \text{RECOMMEND} \longrightarrow \text{PLAY NEXT} \longrightarrow \text{OBSERVE} \longrightarrow \text{LEARN}$$

---

## 🏛️ Core Architectural Separation

The system maintains a strict separation across four layers:

1. **OBSERVATION (Android MediaSession)**: Observes what is currently playing, timestamps, live scrubber positions, and telemetry without requiring private cloud tokens.
2. **PERSONAL DATA (Room DB)**: Builds our own on-device **Personal Listening Library**, behavioral signals, and song-to-song Markov transition graph ($A \to B$).
3. **RECOMMENDATION (Multi-Tier Pipeline)**: Generates candidates across 4 tiers, ranks them deterministically with local heuristics, and contextually reasons via Gemini AI.
4. **PLAYBACK (MediaController.TransportControls)**: Dispatches `playFromSearch(title + " " + artist)` so the target music player performs the native catalog lookup and lossless audio stream.

---

## 📦 Multi-Tier Candidate Generation Pipeline

```text
                    CURRENT SONG
                         │
                         ▼
                    MediaSession
                         │
                         ▼
              ┌────────────────────┐
              │   Personal Music   │
              │   History (Room)   │
              └─────────┬──────────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     Personal        Catalog       Gemini
     History        Discovery     Discovery
     (Tier 1)       (Tier 2)      (Tier 3)
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                Candidate Pool
                 ~50–100 songs
                        │
                        ▼
              Local Ranking Engine
                        │
                        ▼
            Best Next Song Selected
                        │
                        ▼
         MediaController.playFromSearch()
                        │
                        ▼
             Apple Music / YT Music
```

- **Tier 1 — Personal Listening Library (Room DB)**: High-affinity tracks, frequent replays, and historically successful transitions ($A \to B$).
- **Tier 2 — Public Catalog & Metadata (Provider-Independent)**: Uses Apple's free public iTunes Search API and open sources (MusicBrainz/Last.fm) for artist discographies and album tracks.
- **Tier 3 — Gemini AI Discovery**: Generates creative candidate ideas matching acoustic vibe, tempo, and mood. Every Gemini suggestion is validated against real catalog data before playback.
- **Tier 4 — Future Modular Sources**: Extensible interface for official streaming APIs or local device audio files.

---

## ✨ Key Features

- **Universal Android MediaSession Integration ($0 Cost)**: Connects natively to active media players via `MediaSessionManager` and `NotificationListenerService` without requiring proprietary web SDKs or developer accounts.
- **Real-Time Telemetry State Machine**: Accurately classifies:
  - 🟢 **Natural Completion** ($\ge 90\%$ playback)
  - 🔴 **Early Skip** ($< 15$ seconds) $\to$ applies decaying temporary penalty
  - 🟡 **Late Skip** ($15\text{s} \le t < 85\%$) $\to$ subtle negative transition signal
  - 🔁 **Immediate Replay** $\to$ strong positive preference reinforcement
  - ❤️ **Like / 👎 Dislike** $\to$ persistent rating updates & remote custom action forwarding
- **Markov Transition Graph ($A \to B$)**: Learns which songs flow naturally after one another for your unique musical taste.
- **Exponential Half-Life Skip Penalty**: Single skips decay over time ($t_{1/2} = 4\text{h}$):
  $$P(t) = P_0 \cdot \left(\frac{1}{2}\right)^{\frac{t}{t_{1/2}}}$$
  A liked song that was skipped once is suppressed *temporarily*, not erased from long-term memory.
- **Deterministic Local Ranking Engine**: Multi-factor scoring weighting transitions, artist affinity, session mood, and decay penalties.
- **100% Privacy & Local Storage**: All listening data is persisted locally in an on-device Android Room database.

---

## 📂 Project Structure

```text
AI-Powered-Music-Recommendation-Engine/
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml          # Service declarations & permissions
│   │   │   └── java/com/musicengine/mediapoc/
│   │   │       ├── MainActivity.kt          # Main Compose activity & permission guards
│   │   │       ├── service/
│   │   │       │   └── MediaNotificationListenerService.kt # MediaSession observer & telemetry
│   │   │       ├── ui/
│   │   │       │   ├── components/
│   │   │       │   │   ├── NowPlayingCard.kt        # Live artwork, scrubber, like/dislike
│   │   │       │   │   ├── PlaybackControlPanel.kt  # TransportControls & search tester
│   │   │       │   │   └── EventLogList.kt          # Color-coded real-time event feed
│   │   │       │   ├── model/
│   │   │       │   │   └── PlaybackModels.kt        # State models, Enums, formatters
│   │   │       │   └── viewmodel/
│   │   │       │       └── PlayerViewModel.kt       # MVVM StateFlow & session management
│   │   │       └── theme/                   # Material 3 dark/glassmorphic design system
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
├── .gitignore                               # Configured for Android & Gradle
└── README.md
```

---

## 🚀 Development Roadmap

| Phase | Description | Status |
|---|---|---|
| **Phase 1** | **Android MediaSession Controller & Telemetry** (Active observer, position extrapolator, event classifier, `TransportControls`, POC tester UI) | **Completed & Verified ✅** |
| **Phase 2** | **Personal Music Database (Room DB)** (`TrackEntity`, `PlayEventEntity`, `TransitionEntity`, `SkipPenaltyEntity`, Half-life decay math) | **In Progress 🔄** |
| **Phase 3** | **Candidate Generator & Heuristic Engine** (Markov transitions $A \to B$, Multi-tier candidate generation, Multi-factor scoring equation) | **Pending ⏳** |
| **Phase 4** | **Gemini AI Contextual Re-Ranking** (Google GenAI Kotlin SDK, compact context serializer, structured JSON reasoning) | **Pending ⏳** |
| **Phase 5** | **Autonomous Background Queue** (Lifecycle-safe continuous autoplay service for zero-touch transitions) | **Pending ⏳** |

---

## 🛠️ Getting Started & Local Setup

### Prerequisites
- **Android Studio** (Koala / Ladybug or newer)
- **JDK 17** or higher
- **Android Device or Emulator** running **Android 8.0 (API 26)** or higher
- **Apple Music** or **YouTube Music** installed on your testing device

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/omkarpawar201/AI-Powered-Music-Recommendation-Engine.git
   cd AI-Powered-Music-Recommendation-Engine/android
   ```
2. Build the debug APK via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install and run on your connected device:
   ```bash
   ./gradlew installDebug
   ```
4. **Grant Required Permissions**:
   - **Notification Access**: Required by Android to attach to active media sessions via `NotificationListenerService`.
   - **Battery Optimization Exemption**: Prevents OS battery savers from killing the background telemetry service.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
