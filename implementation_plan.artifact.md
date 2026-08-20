# Bananasball: Implementation Plan

This document tracks the high-level design, architectural decisions, and progress of the Bananasball KMP application.

---

## 📜 Status Log

| Date | Phase | Decision / Work Done |
| :--- | :--- | :--- |
| 2026-08-19 | Observe/Orient | Project initialized. Vision defined: Watch & Engage engine for Banana Ball. Stacks chosen: KMP, Compose Multiplatform, Ktor, Room KMP. |
| 2026-08-19 | Act (Foundation) | Initialized KMP project structure. Defined Domain models, Repository, and Use Cases. Implemented Grid layer with Mock Scraper and Room KMP. Built the Tube layer (Schedule Screen, MVI ViewModel). |
| 2026-08-20 | Stage 1 Complete | Resolved YouTube handle identities. Implemented aggressive Intent targeting for the YouTube App. Verified live data sync and navigation on emulator. Identified 15-day UI window constraint for Date Ribbon. |

---

## 🎯 Goal: 1st Stage Exit Requirements
Build a cross-platform (Android/iPad) app that displays today's Banana Ball schedule with direct YouTube stream links and live boxscores.

## 🧱 Architectural Design (Tube / Socket / Grid)

### ⚡ Grid (Data Engine)
- **Scraper/Service**: Ktor-based engine to fetch schedule from `thesavannahbananas.com`.
- **SSOT**: Room KMP to cache game details, teams, and YouTube links.
- **YouTube API**: Integration to resolve direct Video IDs from channel search if necessary.

### 🔌 Socket (Domain Architect)
- **Entities**: `Game`, `Team`, `BoxScore`, `StreamInfo`.
- **Use Cases**: `GetGamesByDate`, `GetLiveBoxScore`, `GetStreamLink`.
- **Contracts**: `GameRepository`, `ConnectivityObserver`.

### 📺 Tube (UI Mirror)
- **Framework**: Compose Multiplatform.
- **Pattern**: MVI (Unidirectional Data Flow).
- **Views**:
    - `ScheduleScreen`: Date picker ribbon + LazyColumn of game cards.
    - `GameCard`: Team logos, score, "Watch Live" YouTube CTA.

---

## 🛠️ Phase 1: Foundation & Today's Schedule (COMPLETED)
1. [x] Setup KMP Template (Android + iOS targets).
2. [x] Define Domain Entities and Repository Interface.
3. [x] Implement Grid Layer (Ktor client + Mock Data for testing).
4. [x] Build the Tube (Date Picker + Game List).

---

## 🛠️ Stage 2: Engagement & UX Polish
1. [ ] **Infinite Date Ribbon**: Refactor `DateRibbon` to support lazy scrolling beyond the initial 15-day window.
2. [ ] **Calendar Picker**: Add a jumping-off point to select any date in the season.
3. [ ] **Team Branding**: Fetch and display official team logos from the scraper.
4. [ ] **Notifications**: Implement "Follow" logic for teams and local start-time alerts.

---

## 🧪 Verification Plan
### Automated Tests
- [ ] Unit tests for `GetGamesByDate` use case.
- [ ] Mapper tests for Scraper logic.

### Manual Verification
- [ ] Verify "Watch Live" link opens the YouTube app/browser on both Android and iPad simulators.
- [ ] Verify scrolling through dates updates the game list correctly.
