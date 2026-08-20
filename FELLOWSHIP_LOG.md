# 📜 Fellowship Log: Bananasball

## 🗓️ 2026-08-20 (The Foundation)

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
