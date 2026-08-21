# Bananasball 🍌⚾️

A professional-ish fan app for the **Banana Ball** league, allowing fans to quickly watch backflip catches and all the other yellow ball antics.

<p align="center">
  <img src="docs/screenshots/schedule_live_view.png" width="300" alt="Banana Ball Live Schedule and Scorecard">
</p>

---

## 🌟 Key Features

### 📡 Real-Time Stream Discovery & Live Viewers
- **Deep YouTube Broadcast Discovery**: Automatically scans official team channels (Savannah Bananas, Party Animals, Firefighters, Texas Tailgaters, and more) to extract direct stream watch links and broadcast thumbnails.
- **Dynamic Viewership Badges**: Displays pre-game waiting counters (`🔥 X waiting`) and live audience counters (`🔴 5.7K watching`) directly on match cards.
- **One-Click Launch**: Opens live broadcasts directly in the native YouTube app or browser.

### ⚾️ Live Game Scorecard & Banana Ball Scorebug
- **Banana Ball Scoring**: Tracks both inning-by-inning points (`PTS`) and cumulative game action (`Runs` & `Hits`).
- **2-Hour Game Clock**: Live, second-by-second countdown clock counting down from the official Banana Ball 2-hour hard time limit.
- **Dual-Time Display**: Separates Game Start (First Pitch) from Stream Broadcast start times, converted accurately from venue timezones (MST, EDT, CDT) to local device time.
- **Pulsing Live Status**: Dynamic pulsing indicators for games currently in progress.

### 📅 Interactive Schedule & Calendar Navigation
- **Dynamic Date Ribbon**: Quick horizontal date picker ribbon to browse adjacent days.
- **Full Season Calendar**: Material 3 DatePickerDialog to jump to any date across the entire world tour.
- **Pull-to-Refresh & Adaptive Polling**: Automatically refreshes live games every 45 seconds while viewing active game days.

### 📊 League Standings & Leaderboards
- **Official Directus API Integration**: Live standings tracking Wins, Losses, Win Percentage, Run Differential, Points, and Trick Plays.
- **Batting & Pitching Leaders**: Comprehensive leaderboards for Batting Average, Home Runs, RBIs, Stolen Bases, ERA, Strikeouts, and Wins.

### 🧢 Teams & Stadium Hub
- Explore all Banana Ball teams, official colors, home venues, and quick links to team channels.

### 🌓 Midnight Dark Theme
- Tailored Material 3 dark theme with deep midnight surfaces (`#0B1120`), high-contrast slate cards (`#1E293B`), and Savannah Banana yellow accents (`#FFE000`).

---

## 🏗 Architecture & Tech Stack

Built following the **Tube / Socket / Grid** architecture for Kotlin Multiplatform:

| Layer | Technologies |
|---|---|
| **UI (Tube)** | Compose Multiplatform, Material 3, Coil 3 (Async Image Loading), Jetpack Lifecycle / ViewModel |
| **Domain (Socket)** | Pure Kotlin Domain Entities, Repository Contracts, Coroutines & Flow |
| **Data Engine (Grid)** | Room KMP (SQLite persistence, migrations), Ktor Client (Content Negotiation, Timeout, Headers), Ksoup (HTML Scraping), Directus REST API |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or IntelliJ IDEA with KMP plugin
- JDK 17+
- Android SDK 34+

### Build & Run
```bash
# Run unit test suite
./gradlew :shared:testAndroidHostTest

# Build and deploy Android App
./gradlew :androidApp:installDebug
```

---

## 📜 Open Source & Disclaimer

This project is licensed under the **Apache License 2.0**.

> [!NOTE]
> This is an unofficial, fan-made open-source project and is not affiliated with, endorsed by, or sponsored by Fans First Entertainment, the Savannah Bananas, or the Banana Ball league. Official team logos are resolved and loaded from public URLs at runtime to respect copyright and trademarks.
