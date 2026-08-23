# 🍌 Bananasball Roadmap & Future Enhancements

This document outlines upcoming feature initiatives, testing improvements, and architectural milestones for future development cycles.

---

## 🎯 High-Priority Initiatives

### 1. 📜 Interactive "Banana Ball Rules" In-App Guide
- **Concept**: A dedicated UI view (or interactive drawer modal) presenting all 9 official Banana Ball rules cataloged in [BANANABALL_RULES.md](docs/BANANABALL_RULES.md).
- **Features**:
  - High-contrast expandable rule cards with custom iconography.
  - Plain-English breakdown of unique concepts: *Trick Plays, Fan Catches, the 2-Hour Clock, Golden Batter Rule, and Showdown Tiebreakers*.
  - Deep-linkable from the Gameday View ("Why do all runs count in the final inning?" tooltip).

### 2. ⚡ Live Simulation Harness & Compose UI Testing Suite
- **Concept**: A developer debug harness and automated Compose UI test suite to verify real-time state transitions without waiting for live game broadcasts.
- **Features**:
  - **Live Game Simulation Harness**: A debug toggle that streams simulated inning progressions, scoring runs, B4S occurrences, Golden Batter calls, and Showdown rounds into `ScheduleViewModel`.
  - **Compose UI Tests (`androidx.compose.ui.test`)**: Semantic automated interaction tests for DateRibbon scrolling, Gameday modal expansion, theme toggling, and pull-to-refresh.

### 3. ⚾ Showdown & Golden Batter Visualization in Gameday Sheet
- **Concept**: Elevate the [GameDetailModalSheet.kt](shared/src/commonMain/kotlin/com/example/bananasball/ui/schedule/GameDetailModalSheet.kt) to handle tiebreaker edge cases with custom Banana Ball intelligence.
- **Features**:
  - **Showdown Round Tracker**: Visual cards for Round 1 (1 Pitcher, 1 Catcher, 1 Fielder), Round 2 (Pitcher + Catcher only), and Round 3 (Bases Loaded).
  - **Golden Batter Badge**: Visual indicator highlighting when a manager exercises their one-time Golden Batter rule.

### 4. 👤 Interactive Player Profiles & Career Ledgers
- **Concept**: Rich player detail bottom sheet accessible by tapping any player name in the Batting/Pitching leaderboards or in-game box scores.
- **Features**:
  - Player headshot/photo loaded dynamically via Coil.
  - Bio, jersey number, positions, hitting roles, and season stat splits (AVG, OPS, HR, RBI, B4S, Strikeouts).
  - Pitching splits (ERA, Wins, Saves, Strikeouts, Sprints Allowed).

### 5. 🔔 Game-Day & Broadcast Notifications
- **Concept**: Local Android notifications to keep fans alerted before first pitch and during critical game moments.
- **Features**:
  - **15-Minute First Pitch Reminder**: Scheduled via Android `AlarmManager` / Kotlin Multiplatform notification scheduler.
  - **Live Broadcast Alert**: Triggered when YouTube channel scraping detects active streaming with `isLive = true`.
  - **Showdown Alert**: Instant notification when a game ends in a tie at the 2-hour mark and enters the Showdown tiebreaker.

### 6. 🍏 iOS & iPad Tablet Device Validation
- **Concept**: Validate and polish the Compose Multiplatform iOS client (`iosApp`) on connected physical iPad and iPhone devices.
- **Features**:
  - **iPad Tablet Adaptive Layouts**: Optimize DateRibbon, multi-column standings/stats tables, and modal sheet sizing for larger tablet screen real estate and landscape orientation.
  - **Physical Device Provisioning**: Wire Xcode signing configuration to deploy builds directly to physical test devices via USB.
  - **iOS Platform Parity**: Verify Ktor Darwin engine networking, Coil image caching, Room SQLite persistence, and external URL scheme launch behavior on iOS.

---

## 📦 Distribution & App Store Publishing Roadmap

### 1. 🛡️ F-Droid Inclusion (Top Priority)
- **Goal**: Publish Bananasball on F-Droid as a 100% Free and Open Source Software (FOSS) application.
- **Requirements**:
  - Ensure zero proprietary binary blobs or non-FOSS tracking dependencies in build outputs.
  - Maintain dynamic Coil image loading for brand assets without bundled proprietary media.
  - Author reproducible `fdroiddata` build metadata recipe.

### 2. 🏪 Google Play Store & Apple App Store
- **Google Play**: Configure signed Android App Bundle (`.aab`) generation and release track automation.
- **Apple App Store / TestFlight**: Configure Xcode archive, export compliance, and TestFlight beta distribution pipeline.

---

## 🏗️ Architecture & Infrastructure Backlog

- **Directus Live WebSockets**: Explore WebSocket subscription endpoints if Directus exposes real-time state streams for zero-delay score updates.
- **Offline Cache Pre-Warming**: Pre-cache static team rosters and venue metadata on first app launch for instant zero-latency navigation.
