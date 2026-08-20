# Bananasball Multi-View Expansion & Metadata Architecture

Implement the **Teams Screen**, **Stats & Standings Screen**, **DateRibbon & Calendar Picker**, and top-level **Navigation Shell** for the Bananasball KMP application, powered by live scraping from `bananaball.com/teams/` and `bananaball.com/stats/`.

## User Review Required

> [!IMPORTANT]
> **Key Architecture Decisions:**
> 1. **Centralized `TeamProvider`**: All team metadata (Savannah Bananas `SB`, Party Animals `PA`, Firefighters `FF`, Texas Tailgaters `TG`, Loco Beach Coconuts `LBC`, Indianapolis Clowns `IC`) including logos, colors, YouTube channel URLs, official roster links, and aliases are centralized in a pure domain contract and data provider.
> 2. **Multi-Screen Navigation**: Introduce a sleek Material 3 Navigation Bar (or Tab Bar) in the main UI supporting 3 core destinations:
>    - 📅 **Schedule** (Game cards, live hype counts, direct YouTube stream launch, dynamic DateRibbon + Calendar DatePicker)
>    - 📊 **Stats & Standings** (League Standings table + Batting/Pitching Stat Leaders)
>    - ⚾ **Teams** (Team cards with brand colors, logos, roster links, and direct YouTube channel links)
> 3. **Scraping Strategy**:
>    - `https://bananaball.com/teams/` for team rosters and metadata.
>    - `https://bananaball.com/stats/` (and backing `banana-stats-pages-seven.vercel.app`) for standings and stat leaders with reliable offline fallback benchmarks.

## Proposed Changes

### Domain Layer (Socket)

#### [NEW] [TeamProvider.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/repository/TeamProvider.kt)
- Define `TeamProvider` interface for resolving teams by ID, name/alias matching, and querying all active teams.

#### [MODIFY] [Team.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/model/Team.kt)
- Add brand color hex codes (`primaryColor`, `secondaryColor`), `rosterUrl`, `websiteUrl`, and `youtubeChannelUrl`.

#### [MODIFY] [Standings.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/model/Standings.kt)
- Ensure fields support complete league standings (rank, team, wins, losses, win percentage, games behind, streak, run differential).

#### [MODIFY] [PlayerStats.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/model/PlayerStats.kt)
- Expand `StatLeader` to support comprehensive Banana Ball stat categories (Batting: AVG, HR, RBI, OPS, B4S [Ball Four Sprints], SB [Stolen Bases]; Pitching: ERA, Wins, SO, WHIP).

#### [NEW] [GetTeamsUseCase.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/usecase/GetTeamsUseCase.kt)
- Pure interactor to stream and refresh all teams.

#### [NEW] [GetStandingsUseCase.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/usecase/GetStandingsUseCase.kt)
- Interactor to stream league standings and trigger background refreshes.

#### [NEW] [GetSeasonStatsUseCase.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/domain/usecase/GetSeasonStatsUseCase.kt)
- Interactor to stream stat leaderboards.

---

### Data Layer (Grid)

#### [MODIFY] [StaticTeamProvider.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/repository/StaticTeamProvider.kt)
- Implement `TeamProvider` domain interface.
- Complete metadata for all 6 teams:
  1. `SB`: Savannah Bananas (Colors: #FFE000, #002D62, Roster PDF link, YouTube streams)
  2. `PA`: Party Animals (Colors: #FF007F, #000000, Roster PDF link, YouTube streams)
  3. `FF`: Firefighters (Colors: #E63946, #1D3557, Roster PDF link, YouTube streams)
  4. `TG`: Texas Tailgaters (Colors: #C1440E, #2B2D42, Roster PDF link, YouTube streams)
  5. `LBC`: Loco Beach Coconuts (Colors: #00A896, #F4A261, Roster PDF link, YouTube streams)
  6. `IC`: Indianapolis Clowns (Colors: #6A0572, #F7B267, Roster PDF link, YouTube streams)

#### [MODIFY] [GameMapper.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/mapper/GameMapper.kt)
- Refactor to use `TeamProvider` instead of private hardcoded name/logo mapping.

#### [MODIFY] [KtorScheduleScraper.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/remote/KtorScheduleScraper.kt)
- Refactor to leverage `TeamProvider` for team codes and YouTube streams.

#### [MODIFY] [KtorTeamScraper.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/remote/KtorTeamScraper.kt)
- Scrape team details, logos, and roster PDF URLs from `https://bananaball.com/teams/` to enrich team entities.

#### [MODIFY] [KtorStatsScraper.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/remote/KtorStatsScraper.kt)
- Support fetching and parsing standings and stat leaderboards from `https://bananaball.com/stats/` / `https://banana-stats-pages-seven.vercel.app/` with robust fallback to structured current standings data.

#### [MODIFY] [OfflineStatsRepository.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/repository/OfflineStatsRepository.kt)
- Complete SSOT persistence for Standings & Season Stats in Room.

#### [MODIFY] [OfflineTeamRepository.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/data/repository/OfflineTeamRepository.kt)
- Persist teams in `TeamDao` and merge scraped updates with `StaticTeamProvider`.

---

### UI Layer (Tube)

#### [NEW] [MainAppScaffold.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/ui/navigation/MainAppScaffold.kt)
- Top-level Scaffold with Material 3 NavigationBar:
  - 📅 **Schedule**
  - 📊 **Stats**
  - ⚾ **Teams**
- Smooth cross-fade or state-based switching.

#### [MODIFY] [ScheduleScreen.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/ui/schedule/ScheduleScreen.kt)
- Add Calendar icon button to the `CenterAlignedTopAppBar`.
- Implement Material 3 `DatePickerDialog` to select any date and scroll `DateRibbon` to it.
- Refactor `DateRibbon` to support dynamic date windows centered around selected date.

#### [NEW] [StatsScreen.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/ui/stats/StatsScreen.kt) & [StatsViewModel.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/ui/stats/StatsViewModel.kt)
- **Standings Tab**: Clean sports standings table (Team logo, Name, W-L, Win%, GB, Streak).
- **Leaders Tab**: Batting leaders (AVG, HR, RBI, OPS, B4S) and Pitching leaders (ERA, Wins, SO, WHIP) with player cards and team badges.
- Pull-to-refresh / refresh intent support.

#### [NEW] [TeamsScreen.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/ui/teams/TeamsScreen.kt) & [TeamsViewModel.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/shared/src/commonMain/kotlin/com/example/bananasball/ui/teams/TeamsViewModel.kt)
- Grid/List of Banana Ball teams with branded color accents, team logos, and action buttons:
  - "View Roster" (opens official roster PDF / link)
  - "YouTube Channel" (opens live streams / channel in YouTube app)
  - "Official Site" (opens official website)

#### [MODIFY] [MainActivity.kt](file:///Users/jacobhaynes/AndroidStudioProjects/Bananasball/androidApp/src/main/kotlin/com/example/bananasball/android/MainActivity.kt)
- Wire up `MainAppScaffold` with `ScheduleViewModel`, `StatsViewModel`, and `TeamsViewModel`.
- Support opening external URLs (YouTube live streams, team rosters, website links) with YouTube package targeting and browser fallback.

---

## Verification Plan

### Automated Tests
- Build verification: `./gradlew :androidApp:assembleDebug`
- Unit tests: `./gradlew :shared:test` or custom parser tests verifying:
  - `StaticTeamProvider` returns all 6 teams and resolves aliases correctly.
  - `GameMapper` correctly converts scraped entities to domain models with `TeamProvider`.
  - Scraper error handling and fallback parsing.

### Manual Verification
- Launch app on Android emulator.
- **Schedule**:
  - Test scrolling the `DateRibbon`.
  - Click the Calendar icon, pick a date, verify schedule updates and ribbon centers on that date.
  - Click "Watch Live" / "Open Stream" on a game card to verify intent launch.
- **Stats**:
  - Navigate to Stats tab.
  - Verify Standings table displays all teams with correct records and win percentages.
  - Switch to Leaders tab, verify Batting & Pitching leaderboards render properly.
- **Teams**:
  - Navigate to Teams tab.
  - Verify all 6 teams (Bananas, Party Animals, Firefighters, Tailgaters, Coconuts, Clowns) render with logos and branding.
  - Test clicking "View Roster" and "YouTube Channel" buttons.
