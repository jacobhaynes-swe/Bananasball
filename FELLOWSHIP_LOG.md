# 📜 Fellowship Log: Bananasball

## 🗓️ 2026-08-19 (The Foundation)

- feat: initialize Bananasball KMP repository and directory structure
- feat(domain): define core models for Game, Team, and BoxScore
- feat(domain): implement GameRepository contract and GetGamesByDateUseCase
- feat(data): setup Room KMP with platform-specific builders for Android and iOS
- feat(data): implement Ktor-based ScheduleScraper with ksoup for live web scraping
- feat(data): add robust date parsing logic to GameMapper for scraped website data
- feat(ui): build ScheduleScreen with MLB-style date ribbon and reactive game list
- feat(ui): implement Bananasball theme with brand-aligned colors (Yellow/Navy)
- feat(android): wire up MainActivity with HttpClient and repository sync logic
- feat(android): configure AndroidManifest with internet permissions and package queries
- feat(android): populate app resources with launcher icons and set mipmap targets
- fix(intent): force YouTube app targeting to resolve 404s and browser fallback issues
- fix(data): verify and apply official team handles for SB, PA, FF, TG, and IC
- fix(env): force 3-button navigation mode on emulator via ADB for easier testing
- docs: initialize Bananasball implementation plan and intake tickets
- docs(council): refactor orchestrator to modular AGENTS.md / Antigravity standard
- docs(council): rename QA specialist to Galadriel to avoid workspace confusion
- docs(council): finalize sanitization of SampleUserApp references and company IP
- test: verify live data sync and "Watch Live" intent success on Pixel 7 emulator
- docs: identify 15-day DateRibbon hardcoding and plan for Stage 2 infinite scroll / calendar picker

## 🗓️ 2026-08-20 (Multi-View Expansion & FOSS Polish)

- feat(data): implement "Deep Stream Discovery" in KtorScheduleScraper to resolve direct YouTube video IDs
- fix(data): update team handle map with verified channels for FF, TG, IC, and official backup
- fix(intent): refactor Android Intents to force YouTube app and land directly on video players
- test: confirm "one-click" landing on scheduled live stream for Firefighters game
- feat(domain): enrich Team model with primary/secondary colors, roster URLs, website URLs, and YouTube channel URLs
- feat(domain): create TeamProvider domain contract and UseCases for GetTeams, GetStandings, and GetSeasonStats
- feat(data): implement StaticTeamProvider covering all 6 official Banana Ball teams (SB, PA, FF, TG, IC, LBC)
- feat(data): implement KtorTeamScraper for roster and team metadata extraction from bananaball.com/teams/
- feat(data): implement KtorStatsScraper parsing standings and batting/pitching leaderboards from banana-stats-pages-seven.vercel.app
- feat(data): update Room AppDatabase schema (v4) with TeamEntity, StandingEntity, and DAOs
- feat(data): migrate stats and standings to official Directus / Next.js JSON API endpoints
- fix(stats): filter qualified hitters (AB >= 40) and qualified pitchers (IP >= 15) matching bananaball.com/stats 2026 World Tour table
- test(stats): add MockEngine unit test suite in KtorStatsScraperTest validating 1:1 website qualifications, ranking rules, and team mappings
- feat(data): enrich schedule with live in-game scores and point totals from stats API
- feat(ui): add 45s adaptive game-day foreground poller in ScheduleViewModel for live stream hype and scores
- feat(ui): integrate Material 3 PullToRefreshBox across Schedule and Stats screens
- fix(teams): update Savannah Bananas and Firefighters remote logo URLs to verified endpoints
- feat(ui): enhance ScheduleScreen with dynamic DateRibbon window, Material 3 DatePickerDialog, and local device start time banner
- feat(data): parse scraped game times with timezone awareness and convert to user local device time
- feat(ui): implement MainAppScaffold bottom navigation shell linking Schedule, Stats, and Teams
- feat(foss): enforce strict FOSS/F-Droid compliance by loading all logos and images dynamically via Coil with zero APK-bundled brand assets
- test: deploy and verify full multi-view navigation on Pixel 7 emulator across all screens

## 🗓️ 2026-08-21 (Release Automation, Council Directives & UI Edge-to-Edge)

- feat(ui): remove legacy Android platform ActionBar via `Theme.Bananasball` for seamless edge-to-edge Material 3 styling
- docs(council): establish Council of Android autonomous specialist directives and work-hours commit blockout rules in `AGENTS.md` and `.agents/`
- docs(api): map out comprehensive Directus and stats API surface in `docs/BANANABALL_INTELLIGENCE.md` covering box scores, lineups, player ledgers, and social feeds
- feat(ci): add automated GitHub Actions APK release pipeline (`.github/workflows/release.yml`) for tag-driven builds
- docs(readme): embed 3-view framed screenshot showcase gallery and prominent Download APK badge with sideload guide
- release: prepare v1.0.0 public beta release with automated asset distribution

