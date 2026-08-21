# 🍌 Banana Ball Intelligence: API Specification & Domain Mapping

This document details the complete surface, schema definitions, endpoint boundaries, and domain architecture for the **Banana Ball Stats & Analytics Engine** (`https://banana-stats-pages-seven.vercel.app`).

---

## 🧭 System Architecture & Endpoints

Base URL: `https://banana-stats-pages-seven.vercel.app`

### 1. In-Depth Game Box Scores & Lineups
- **Endpoint**: `GET /api/directus-items/box-score?gameId={gameId}`
- **Purpose**: Full gameday box score with individual batting logs, bullpen pitching charts, trick plays, and inning breakdowns.
- **Key Response Fields**:
  - `gameId`: UUID of the game
  - `gameDate`, `time`, `end_time`, `gameTime`, `status`, `winnerTeamId`
  - `numberOfInnings`, `equalizerPointAwarded`, `equalizerPointInning`
  - `venue`: Stadium details (id, name, city, state)
  - `teams[]`:
    - `teamId`, `name`, `abbreviation`, `logo`, `color`
    - `innings[]`: Inning-by-inning runs, hits, points awarded
    - `batters[]`:
      - `playerId`, `name`, `jersey_number`, `order` (1–10, 999 for subs)
      - `positions[]` (`SS`, `LF`, `1B`, `CF`, `2B`, `3B`, `C`, `RF`, `DF`)
      - `hitting_roles[]` (`DH`, `EH`, `DR`)
      - `AB`, `R`, `H`, `RBI`, `B4S` (Ball 4 Sprints), `K`, `WO` (Walk-offs), `SB`, `AVG`, `OPS`
    - `pitchers[]`:
      - `playerId`, `name`, `jersey_number`
      - `designations[]` (`SP`, `RP`, `CL`, `W`, `L`, `SVO`)
      - `IP` (Float, e.g. `5.0`, `1.2`, `1.1`), `H`, `R`, `ER`, `B4S`, `K`, `FAN` (Fan Catches Allowed), `ERA`, `MPI` (Minutes Per Inning, e.g. `"4:13"`)
    - `fielding[]`:
      - `playerId`, `name`, `jersey_number`, `positions[]`
      - `TPO` (Trick Play Outs), `TPM` (Trick Plays Missed), `E` (Errors)
    - `designated_runners[]`, `designated_fielders[]`, `golden_batters[]`

---

### 2. Complete League Player Profiles
- **Endpoint**: `GET /api/stats/players_stats?season={seasonId}&limit={limit}&offset={offset}`
- **Purpose**: Career & season statistical ledger for every player in the league.
- **Key Response Fields**:
  - `data[]`:
    - Player Identity: `id`, `first_name`, `last_name`, `jersey_number`, `image`
    - Team: `team.id`, `team.name`, `team.abbreviation`, `team.logo`, `team.color`
    - Traditional Batting: `at_bats`, `hits`, `runs`, `home_runs`, `runs_batted_in`, `doubles`, `triples`, `strikeouts`, `batting_average`, `on_base_percentage`, `slugging_percentage`, `on_base_plus_slugging`, `batting_average_on_balls_in_play`
    - Banana Ball Batting: `stolen_bases`, `stolen_bases_first_base`, `ball_four_sprints`, `sprint_rate`, `foul_outs_to_fans_batter`, `golden_batters`, `walk_offs`
    - Traditional Pitching: `innings_pitched`, `earned_run_average`, `earned_runs`, `runs_allowed`, `hits_allowed`, `home_runs_allowed`, `strikeouts`, `wins`, `losses`, `saves`, `save_opportunities`, `shutouts`, `complete_games`, `expected_fip`, `siera`
    - Banana Ball Pitching: `ball_four_sprints_allowed`, `foul_outs_to_fans`, `trick_play_outs`, `trick_plays_missed`, `trick_play_rate`, `minutes_per_inning`, `mpi_average_seconds`, `mpi_innings`

---

### 3. League Leaderboards
- **Batting Leaders**: `GET /api/stats/leaders`
- **Pitching Leaders**: `GET /api/stats/leaders?subCategory=pitching`
- **Supported Boards**:
  - **Batting (14 Categories)**: Batting Average, OPS, OBP, SLG, Home Runs, RBIs, Hits, Doubles, Triples, Stolen Bases, Stolen First Bases, Ball Four Sprints, Strikeouts, Fan Catches.
  - **Pitching (12 Categories)**: Earned Run Average, Strikeouts, Minutes Per Inning, Wins, Losses, Innings Pitched, Saves, Ball Four Sprints Allowed, Points Earned, Points Lost, Strikeout %, Sub-Two-Minute Innings.

---

### 4. Playoff Race & Standings
- **Standings**: `GET /api/directus-items/standings?season={seasonId}`
- **Playoff Race**: `GET /api/directus-items/playoff-race?season={seasonId}`
- **Key Response Fields**:
  - `standings[]`: Team records, Points, Win %, Streak, Run Differential, Trick Plays
  - `playoff_format`: Seed structure and postseason rules
  - `tiebreaker_order`: Explicit sequence of tiebreakers
  - `tiebreaker_copy_html`: Renderable HTML explanation of tiebreaker calculations
  - `buckets[]`: Qualification tiers (`Clinched`, `In the Hunt`, `Eliminated`)

---

### 5. Showdown Analytics Engine
- **Player Showdown Stats**: `GET /api/stats/showdown_players_stats`
- **Game Showdown Moments**: `GET /api/stats/showdowns_stats`
- **Key Response Fields**:
  - `id`, `game_id`, `round` (`Round 1`, `Round 2`, `Round 3`), `outcome_type`, `runs_scored`, `hits_recorded`, `is_walkoff`, `chases_allowed`
  - `batter_id`, `pitcher_id`, `fielder_id`
  - Player showdown metrics: `show_down_opportunities`, `show_down_shutdown_p`, `showdown_strikeouts`, `showdown_home_runs`, `showdown_walk_offs`, `success_rate`

---

### 6. Teams, Ballparks & Glossary
- **Teams**: `GET /api/directus-items/teams` (23 franchises, rosters, hex colors, team logos, `is_bbcl_team`)
- **Venues**: `GET /api/directus-items/venues` (131 stadiums, capacity, surface type, coordinates, timezones)
- **Glossary**: `GET /api/directus-items/glossary` (99 Banana Ball terms and rule definitions)

---

## 🛡 Architectural Boundaries & Strategy

1. **Hybrid Real-Time Pipeline**:
   - YouTube Scraping delivers real-time live viewership counts (`🔴 5.7K watching`), pre-game waiting hype (`🔥 X waiting`), and broadcast thumbnails.
   - The Directus API provides high-precision post-game box scores, pitching logs, and official standings.
   - Combine both in `RoomGameRepository` with SQLite persistence as the Single Source of Truth (SSOT).

2. **Baseball Notation & IP Derivation**:
   - `4.1 IP` = 4 complete innings + 1 out.
   - `4.2 IP` = 4 complete innings + 2 outs (equivalent to 4.6667 innings).
   - Home team pitchers pitch in the **Top** of the inning; Away team pitchers pitch in the **Bottom** of the inning.

3. **Domain Layer Expansion (Socket)**:
   - `Player`: Full statistical model mapping batting, pitching, and showdown metrics.
   - `GameDetail`: Comprehensive model encapsulating `BoxScore`, `Lineup`, `BullpenLog`, and `FieldingStats`.
   - `PlayoffPicture`: Domain entity resolving tiebreakers and seeding buckets.

---

## 🌐 Ecosystem Crawl & Social Intelligence Engine

### 1. Official Team Social Registry
Every Banana Ball franchise maintains distinct, official channels across YouTube, Instagram, TikTok, and X (Twitter):

| Franchise | YouTube Channel | Instagram | TikTok | X (Twitter) |
|---|---|---|---|---|
| **Savannah Bananas** | `@TheSavannahBananas` | `@thesavbananas` | `@thesavbananas` | `@TheSavBananas` |
| **Party Animals** | `@thepartyanimals` | `@thepartyanimals` | `@theofficialpartyanimals` | `@theprtyanimals` |
| **The Firefighters** | `@TheOfficialFirefighters` | `@thefirefightersbb` | `@theofficialfirefighters` | — |
| **Texas Tailgaters** | `@TheTexasTailgaters` | `@thetexastailgaters` | `@thetexastailgaters` | — |
| **Indianapolis Clowns**| `@indianapolisclowns` | `@theindianapolisclowns` | — | — |
| **Loco Beach Coconuts**| `@locobeachcoconuts` | `@locobeachcoconuts` | — | — |
| **Banana Ball League** | `@officialbananaball` | `@bananaball` | `@officialbananaball` | — |

---

### 2. Zero-Auth Live Social Feed Architecture
To power the upcoming **Socials Tab** with strict deduplication (no repeated videos, shorts, or clips across platforms):

1. **Scraping Pipeline (Videos & Shorts)**:
   - **Endpoint**: `https://www.youtube.com/{handle}/videos` and `https://www.youtube.com/{handle}/shorts`
   - **Extractor**: Parse `ytInitialData` JSON (`lockupViewModel` hierarchy) to extract:
     - `id` (YouTube Video ID / Short ID)
     - `title`
     - `thumbnailUrl`
     - `views` (e.g. `"48K views"`)
     - `publishedTime` (e.g. `"1 month ago"`, `"2 days ago"`)
     - `duration` / `contentType` (`VIDEO` vs `SHORT`)

2. **Deduplication Engine**:
   - Primary Key: `content_id` (e.g. `yt_w9dqiv8Qpzk`).
   - Normalization Hash: `SHA-256(team_id + normalized_title)`.
   - If a clip is published as both a short and an announcement, the deduplication engine retains the highest-resolution stream item.

3. **Favorite Team Social Aggregator**:
   - Filter by user's selected `favoriteTeamId`.
   - Unified chronological feed supporting direct playback in-app or launching to the native app/browser.

---

### 3. WordPress REST APIs & Media Backends
- `https://bananaball.com/wp-json/wp/v2/event`: Official schedule events and tour dates.
- `https://bananaball.com/wp-json/wp/v2/team`: Official team lore, rosters, and ACF custom metadata.
- `https://bananaball.com/wp-json/wp/v2/bb_gallery`: Photo galleries and highlight media items.

