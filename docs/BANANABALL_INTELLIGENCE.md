# Banana Ball Intelligence: Master API & Analytics Specification

This document is the comprehensive, auto-catalogued technical specification and domain architecture for the **Banana Ball Analytics Platform** (`https://banana-stats-pages-seven.vercel.app`).

---

## 🧭 System Architecture & API Endpoints

Base URL: `https://banana-stats-pages-seven.vercel.app`  
Directus Asset CDN: `https://stats.bananaball.com/assets/{asset_id}` (Supports dynamic Directus image transformations)  
Current Season UUID: `31e2f9a5-66cc-4352-9297-017ea4162ff0`

### Directus Dynamic Image Transformations:
The asset CDN natively supports Directus on-the-fly image transformations:
- **Optimized Mobile Player Headshots**: `https://stats.bananaball.com/assets/{asset_id}?width=150&height=150&fit=cover&format=webp&quality=80`
- **Performance Impact**: Reduces image size from **~328 KB to 4.6 KB** (a **98.6% bandwidth & memory reduction**), providing instant rendering in Compose without UI lag or memory strain.

---

## 1. 📋 Navigation Matrix: Categories & Subcategories

The web platform defines 6 top-level categories and dedicated subcategories:

| Category | Subcategory | Description | Underlying API Route |
|---|---|---|---|
| `players` | `hitting` | Player batting ledger (traditional + B4S, golden batters, walk-offs) | `GET /api/stats/players_stats?subCategory=hitting` |
| `players` | `pitching` | Pitcher charts (ERA, strikeouts, Minutes Per Inning, sub-2-min innings) | `GET /api/stats/players_stats?subCategory=pitching` |
| `players` | `fielding` | Defensive stats & Trick Play metrics (TPO, TPM, TPR, Errors) | `GET /api/stats/players_stats?subCategory=fielding` |
| `teams` | `hitting` | Aggregate franchise batting metrics, team sprint totals | `GET /api/stats/teams_stats?subCategory=hitting` |
| `teams` | `pitching` | Team bullpen ERA, staff MPI, sub-2-minute innings | `GET /api/stats/teams_stats?subCategory=pitching` |
| `teams` | `fielding` | Franchise trick play conversion and defensive rates | `GET /api/stats/teams_stats?subCategory=fielding` |
| `showdowns` | `overview` | Showdown moments ledger by game (round, outcome, chases allowed) | `GET /api/stats/showdowns_stats?season={seasonId}` |
| `showdowns` | `hitting` | Batter showdown success rates, walk-offs, showdown home runs | `GET /api/stats/showdown_players_stats?subCategory=hitting` |
| `showdowns` | `pitching` | Pitcher showdown shutdown %, showdown strikeouts | `GET /api/stats/showdown_players_stats?subCategory=pitching` |
| `games` | `scores` | Game scores, line score point matrix, in-depth box scores | `GET /api/directus-items/box-score?gameId={id}` & `GET /api/stats/games` |
| `games` | `schedule` | Upcoming tour calendar, date range filtering, venue lookup | `GET /api/stats/games?dateFrom={date}&dateTo={date}` |
| `standings` | — | Official standings, point tallies, win streaks, tiebreaker rules | `GET /api/directus-items/standings?season={seasonId}` |
| `standings` | `playoffs` | Playoff race tiers (`Clinched`, `In the Hunt`, `Eliminated`), magic numbers | `GET /api/directus-items/playoff-race?season={seasonId}` |
| `leaders` | `hitting` | League-wide batting leaders across 14 categories | `GET /api/stats/leaders?subCategory=hitting` |
| `leaders` | `pitching` | League-wide pitching leaders across 12 categories | `GET /api/stats/leaders?subCategory=pitching` |
| `glossary` | — | 99 official rule definitions & Banana Ball specific terms | `GET /api/directus-items/glossary` |

---

## 2. ⚾ Game Calendar, Scores & In-Depth Box Scores

### A. Game Calendar & Date Filtering (`GET /api/stats/games`)
- **Query Parameters**:
  - `dateFrom={YYYY-MM-DD}&dateTo={YYYY-MM-DD}`: Specific date (e.g. `2026-08-22`) or date range filter.
  - `season={seasonId}`: Filter schedule by season UUID.
  - `team={teamId}`: Filter schedule by specific franchise UUID.
- **Returns**: Array of game summaries with `id`, `date`, `home_team`, `away_team`, `status` (`scheduled`, `in_progress`, `final`), and `venue`.

### B. In-Depth Game Box Score (`GET /api/directus-items/box-score?gameId={gameId}`)
- **Schema Overview**:
  - `gameId`: UUID matching the game identity
  - `gameDate`: `YYYY-MM-DD`
  - `time` / `gameTime`: Local start time (e.g. `"19:00:00"`)
  - `status`: `"scheduled"`, `"in_progress"`, `"final"`
  - `numberOfInnings`: Total innings played (e.g. `9`)
  - `equalizerPointAwarded` (Boolean), `equalizerPointInning`
  - `venue`: Stadium name (`name`), city (`city`), state (`state`), timezone (`timezone`)
  - `teams[]`:
    - `teamId`, `teamName`, `teamAbbreviation`, `teamLogo`, `isHomeTeam`
    - `prh`: `{ points_regular, points_sd, points_total, runs, hits }`
    - `lineScore`:
      - `innings[]`: `[ { inning: 1, runs: 3, hits: 4, points_awarded: 1 }, ... ]` (9th inning awards 2 points)
      - `showdown[]`: Round-by-round showdown tiebreaker results
    - `batters[]`:
      - `playerId`, `name`, `jersey_number`, `order` (1–10, 999 for subs)
      - `positions[]`: `SS`, `LF`, `1B`, `CF`, `2B`, `3B`, `C`, `RF`, `DH`
      - Traditional: `AB`, `R`, `H`, `RBI`, `K`, `AVG`, `OPS`
      - Banana Ball: `B4S` (Ball Four Sprints), `WO` (Walk-Offs), `SB` (Stolen Bases)
    - `pitchers[]`:
      - `playerId`, `name`, `jersey_number`, `designations[]` (`SP`, `RP`, `CL`, `W`, `L`, `SVO`)
      - Traditional: `IP` (e.g. `4.2`, `1.1`), `H`, `R`, `ER`, `BB`, `K`, `ERA`
      - Pace-of-Play: `MPI` (Minutes Per Inning, e.g. `"3:51"`)

---

## 3. 👤 Comprehensive Player Statistics Ledger

- **Endpoint**: `GET /api/stats/players_stats?season={seasonId}&subCategory={hitting|pitching|fielding}&limit={limit}&offset={offset}`
- **Total Players**: `77`
- **Player Profile Fields**:
  - **Identity**: `id`, `first_name`, `last_name`, `jersey_number`, `image` (Asset UUID), `primary_position` (`{ id, label, value }`), `active_status`
  - **Team**: `team.id`, `team.name`, `team.abbreviation`, `team.logo`, `team.color`
  - **Batting (Traditional)**: `games_played`, `at_bats`, `hits`, `runs`, `home_runs`, `runs_batted_in`, `doubles`, `triples`, `strikeouts`, `batting_average`, `on_base_percentage`, `slugging_percentage`, `on_base_plus_slugging`, `batting_average_on_balls_in_play`
  - **Batting (Banana Ball)**:
    - `ball_four_sprints`: Total Ball Four Sprints
    - `sprints_1`, `sprints_2`, `sprints_3`, `sprints_4`: Sprints advanced to 1st, 2nd, 3rd, or Home
    - `sprint_rate`: Sprint success conversion frequency
    - `stolen_bases_first_base`: Stealing first base on passed balls/wild pitches
    - `foul_outs_to_fans_batter`: Outs caused by fan catches
    - `golden_batters`: Golden Batter appearances
    - `walk_offs`: Walk-off hits/sprints
  - **Pitching (Traditional)**: `innings_pitched`, `earned_run_average`, `earned_runs`, `runs_allowed`, `hits_allowed`, `home_runs_allowed`, `strikeouts`, `wins`, `losses`, `saves`, `save_opportunities`, `shutouts`, `complete_games`, `expected_fip`, `siera`
  - **Pitching (Banana Ball)**:
    - `minutes_per_inning` (MPI): Clock time per inning
    - `sub_two_minute_innings`: Innings completed under 120 seconds
    - `trick_play_outs`: Outs converted via defensive trick plays
    - `trick_plays_missed`: Failed trick play attempts
    - `trick_play_rate`: Trick play success rate
    - `foul_outs_to_fans`: Fan catches recorded on defense
    - `ball_four_sprints_allowed`: Walks conceding sprints
  - **Fielding & Defense**:
    - `trick_play_outs` (`TPO`): Trick play outs recorded
    - `trick_plays_missed` (`TPM`): Trick plays missed
    - `trick_play_rate` (`TPR`): Trick play conversion percentage
    - `errors` (`E`): Traditional fielding errors
    - `passed_outs`: Outs recorded on passed balls

---

## 4. 🏟 Franchise & Team Aggregates (`GET /api/stats/teams_stats?season={seasonId}&subCategory={hitting|pitching|fielding}`)

Provides full-franchise statistical totals and comparative rankings:

### A. Team Fielding & Trick Play Conversion (`subCategory=fielding`)
- `trick_play_outs` (`TPO`): Total trick play outs converted (e.g. Tailgaters `36`, Bananas `31`).
- `trick_plays_missed` (`TPM`): Trick plays attempted that resulted in no out.
- `trick_play_rate` (`TPR`): Average trick plays executed per game (e.g. `12.0 TPO/game`).
- `errors` (`E`): Traditional fielding errors.
- `passed_outs`: Outs recorded on passed balls / wild pitches.
- `foul_outs_to_fans`: Fan catches recorded while on defense.

### B. Team Hitting & Sprint Totals (`subCategory=hitting`)
- `batting_average`, `on_base_plus_slugging`, `home_runs`, `runs`.
- `ball_four_sprints`: Team total Ball Four Sprints.
- `sprints_1`, `sprints_2`, `sprints_3`, `sprints_4`: Base-by-base breakdown of completed sprints.

### C. Team Bullpen & Pitching Staff (`subCategory=pitching`)
- `earned_run_average` (ERA), `pitcher_strikeouts`, `innings_pitched`.
- `minutes_per_inning` (Staff MPI, e.g. `"00:03:57"` for Firefighters, `"00:04:01"` for Bananas).
- `sprints_allowed`: Total ball four sprints surrendered by the staff.

---

## 5. 🏆 League Leaderboard Categories (`GET /api/stats/leaders?season={seasonId}&subCategory={subCategory}`)

The leaderboards engine calculates 4 distinct subcategory boards:

### A. Batting Leaders (`subCategory=hitting` - 14 Boards)
- `avg`: Batting Average (e.g. `1.000`)
- `ops`: On-Base Plus Slugging (e.g. `2.417`)
- `obp`: On-Base Percentage
- `slg`: Slugging Percentage
- `hr`: Home Runs
- `rbi`: Runs Batted In
- `h`: Hits
- `doubles`: Doubles
- `triples`: Triples
- `sb`: Stolen Bases
- `sb1b`: Stolen First Bases (Banana Ball rule: stealing first on wild pitches/passed balls)
- `b4s_hitting`: Ball Four Sprints
- `so_hitting`: Strikeouts
- `fan_catches_hitting`: Fan Catches (Foul Outs Caught by Fans)

### B. Pitching Leaders (`subCategory=pitching` - 14 Boards)
- `era`: Earned Run Average (e.g. `0.00`)
- `so_pitching`: Strikeouts
- `mpi`: Minutes Per Inning (e.g. `"00:01:28"`)
- `w`: Wins
- `l`: Losses
- `ip`: Innings Pitched
- `sv`: Saves
- `b4s_pitching`: Ball Four Sprints Allowed
- `pe`: Points Earned
- `pl`: Points Lost
- `so_percentage_pitching`: Strikeout %
- `b4s_percentage_pitching`: B4S % Allowed
- `fan_catches_pitching`: Fan Catches Allowed
- `sub_two_innings`: Sub-Two-Minute Innings Completed

### C. Fielding Leaders (`subCategory=fielding` - 3 Boards)
- `tpo`: Trick Play Outs
- `tpm`: Trick Plays Missed
- `tpr`: Trick Play Rate

### D. Showdowns Leaders (`subCategory=showdowns` - 14 Boards)
- Showdown individual leaders across hitting/pitching showdown categories.

---

## 6. 📊 Standings & Playoff Race Engine

### A. League Standings (`GET /api/directus-items/standings?season={seasonId}`)
- **Metadata**: `season`, `as_of`, `through_date`, `tiebreaker_order`, `tiebreaker_copy_html`, `playoff_format`
- **Table Row Fields**:
  - `rank`: League position (1–6)
  - `team_id`, `team_name`, `team_abbreviation`, `team_logo`, `team_color`
  - `wins`, `losses`, `ties`, `win_pct`
  - `points_scored`, `points_allowed`, `point_differential` (e.g. `+5`, `-4`)
  - `games_back` (`GB`): Games back from 1st place
  - `streak`: Current streak (e.g. `"W3"`, `"L2"`)
  - `trick_plays`: Team total defensive trick play outs
  - `home_record`, `away_record`, `last_10` (`L10`)
  - `clinch_status` (`"none"`, `"clinched"`, `"in_the_hunt"`, `"eliminated"`), `clinch_prefix` (`"x - "`, `"y - "`, `"e - "`)
  - `magic_number`, `elimination_number`

### B. Playoff Race Tiers (`GET /api/directus-items/playoff-race?season={seasonId}`)
- **Buckets**:
  - `clinched[]`: Teams that secured playoff seeds
  - `in_contention[]`: Teams actively in the playoff hunt with active seed, magic number, and elimination number calculations
  - `eliminated[]`: Teams mathematically eliminated from postseason contention

---

## 7. ⚔️ Showdown Tiebreaker Analytics Engine

When Banana Ball games are tied after 9 innings (or 2 hours), they enter the famous **Showdown Tiebreaker** rounds:
- **Round 1**: Pitcher vs. Batter with 1 Fielder.
- **Round 2**: Pitcher vs. Batter with 2 Fielders.
- **Round 3**: Pitcher vs. Batter with 3 Fielders (or bases loaded).

### A. Game Showdown Moments Ledger (`GET /api/stats/showdowns_stats?season={seasonId}`)
- **Per-Moment Schema**:
  - `id`: Event UUID
  - `game_number`, `game_id`: Associated game
  - `round`: `1`, `2`, or `3`
  - `result`: Human-readable summary (e.g. `"Miller Showdown Shutdown (1), Cruz Strikes Out"`)
  - `outcome_type`: `"shutdown"`, `"walkoff"`, `"home_run"`, `"chase"`
  - `runs_scored`, `hits_recorded`, `is_walkoff`, `chases_allowed`
  - `batter_id`: `{ id, first_name, last_name, jersey_number, image }`
  - `pitcher_id`: `{ id, first_name, last_name, jersey_number, image }`
  - `fielder_id`: `{ id, first_name, last_name, jersey_number, image }`

### B. Showdown Player Metrics (`GET /api/stats/showdown_players_stats?season={seasonId}&subCategory={hitting|pitching}`)
- **Batter Showdown Stats (`subCategory=hitting`)**:
  - `success_rate_hitting`: Showdown batting conversion %
  - `show_down_opportunities_hitting`: Total showdown plate appearances
  - `showdown_rbis`, `showdown_home_runs`, `showdown_walk_offs`, `showdown_strikeouts`
- **Pitcher Showdown Stats (`subCategory=pitching`)**:
  - `show_down_shutdown_p`: Shutdown conversion %
  - `show_down_opportunities`: Total showdown rounds pitched
  - `showdown_strikeouts`, `chases_allowed`, `showdown_walk_offs_allowed`, `showdown_home_runs_allowed`

---

## 8. 📖 Official Banana Ball Glossary & Rule Registry

The platform provides `99` registered rules and terms (`GET /api/directus-items/glossary`).

### Key Banana Ball Specific Terms:
- **Ball-Four Sprints (Allowed by pitcher)** (`B4S`): Number of times a pitcher allows a batter to sprint to any base after earning ball four
- **Ball-Four Sprint** (`B4S`): Total number of times a player has sprinted to first base, or any additional base, after earning ball four
- **Ball-Four Sprint** (`B4S`): When a batter who earns ball four can sprint to any base they choose as all defenders touch the ball, rather than being limited to first base.
- **One-base Sprint** (`B4S1`): When a batter sprints to first base after earning ball four
- **Two-base Sprint** (`B4S2`): When a batter sprints to second base after earning ball four
- **Three-base Sprint** (`B4S3`): When a batter sprints to third base after earning ball four
- **Four-base Sprint** (`B4S4`): When a batter sprints all the way home after earning ball four, scoring a run
- **Caught Stealing of First Base** (`CS1B`): Number of times a batter is thrown out while attempting to steal first base on a wild pitch or passed ball
- **Foul Outs to Fan** (`FAN`): When a fan catches a foul ball without any bobble resulting in an out
- **Fastest Inning** (`FI`): The fastest single inning a pitcher has thrown, timed in whole seconds. An inning qualifies only if the pitcher recorded all three outs.
- **Golden Batter Opportunities** (`GBAT`): The total opportunities a player has to serve as the Golden Batter
- **Hold** (`HLD`): Any time a visiting pitcher is able to protect a lead in an inning of a Banana Ball game
- **Hold Opportunities** (`HLDO`): Any time a visiting pitcher has the chance to protect a lead in an inning of a Banana Ball game
- **Minutes Per Inning** (`MPI`): Average time a pitcher takes to complete an inning, measuring both pace of play and effectiveness
- **Points Earned** (`PE`): Number of points a pitcher earns for their team
- **Points Lost** (`PL`): Number of points a pitcher costs their team
- **Steals of First Base** (`SB1B`): Times a batter successfully steals first base on a wild pitch or passed ball
- **Showdown Opportunities** (`SDO`): Number of times a pitcher has the opportunity to pitch in a round of Showdown Tiebreakers
- **Showdown Shutdowns** (`SDS`): Successfully preventing the batter from scoring in any round of Showdown Tiebreakers
- **Ball-Four Sprints + Hits Per Inning Pitched** (`SHIP`): Combined rate of ball-four sprints and hits allowed per inning pitched
- **Sub-Two-Minute Innings** (`SUB2`): The total number of innings thrown in less than 2 minutes for an individual pitcher. 
- **Trick Plays Missed** (`TPM`): Number of failed attempts at executing trick plays that allow runners to advance or reach base
- **Trick Play Outs** (`TPO`): Outs recorded using extraordinary and unique defensive plays
- **Trick Play Rate** (`TPR`): The average number of trick plays a player accumulates in a Banana Ball game
- **Trick Plays** (`Trick Plays`): Extraordinary or unique plays designed to encourage mind-blowing defensive plays, rated from one to three stars based on difficulty and creativity
- **Walk-Off** (`WO`): Inning-winning or game-winning play that ends the inning or game immediately
- **Walk-Offs Allowed** (`WOA`): Number of walk-off losses charged to a pitcher

---

## 9. 🏟 Venues & Ballparks (`GET /api/directus-items/venues`)
- **Total Venues**: 131
- **Metadata**: Stadium name, city, state, surface type, capacity, geo-coordinates, and timezone string.

---

## 10. 🛡 Domain Integration Roadmap for Bananasball App

1. **Game Detail Screen**:
   - Tap any game on the schedule to open `GameDetailScreen(gameId)`.
   - Inning line score matrix with points awarded badge.
   - Expandable batting lineups and bullpen logs.
2. **Player Roster & Profiles**:
   - Circular player headshots loaded from `https://stats.bananaball.com/assets/{image}`.
   - Tabbed view: Traditional Stats vs Banana Ball Metrics (B4S, TPO, MPI).
3. **Glossary & Rules Tab**:
   - In-app interactive rulebook explaining Trick Plays, Golden Batters, Showdown rules, and Fan Catches.
