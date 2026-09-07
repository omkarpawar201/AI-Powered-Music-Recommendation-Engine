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

The system continuously answers:
> **"Given the song I am listening to right now, what is the best next song to play for me?"**

---

## 🏗️ Architecture & Recommendation Pipeline

The recommendation engine combines local deterministic heuristics, a personalized Markov transition graph ($A \to B$), half-life decaying skip penalties, and contextual Google Gemini AI reasoning:

```text
Target Music Player (Apple Music / YouTube Music)
     │
     ▼ (Android MediaSession Callbacks)
Telemetry Observer (MediaNotificationListenerService)
     │
     ▼
Local Room Database (Listening History, Skips, Replays, Ratings)
     │
     ▼
Local Music Taste Profile & Markov Transition Graph (A → B)
     │
     ▼
Candidate Generator (Tier 1: History/Transitions, Tier 2: Open Catalog/Similar, Tier 3: Discovery)
     │
     ▼
Heuristic Ranking Engine (Rule-based weights, Decaying Skip Penalties, Transition Scores)
     │
     ▼ (Top 5–10 Candidates + Compact Profile Context)
Gemini AI Re-ranker (Contextual & Semantic Transition Analysis)
     │
     ▼
Winning Track Selection
     │
     ▼
IMusicProvider.playFromSearch(title, artist)
     │
     ▼
Target Music Player Playback
```

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
- **Gemini AI Re-Ranking**: Evaluates acoustic flow, tempo, and vibe for top candidate tracks.
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
| **Phase 3** | **Candidate Generator & Heuristic Engine** (Markov transitions $A \to B$, Multi-factor scoring equation, Catalog expansion) | **Pending ⏳** |
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

## 🔒 Permissions Explained

| Permission | Purpose |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Enables `MediaSessionManager` to discover active media sessions and read track metadata / playback states. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ensures continuous background telemetry tracking when the screen is locked or another app is focused. |
| `INTERNET` | Used for open catalog lookups and Gemini AI re-ranking requests. |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
