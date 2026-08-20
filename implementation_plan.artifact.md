# Bananasball: Implementation Plan

This document tracks the high-level design, architectural decisions, and progress of the Bananasball KMP application.

---

## 📜 Status Log

| Date | Phase | Decision / Work Done |
| :--- | :--- | :--- |
| 2026-08-19 | Observe/Orient | Project initialized. Vision defined: Watch & Engage engine for Banana Ball. Stacks chosen: KMP, Compose Multiplatform, Ktor, Room KMP. |
| 2026-08-19 | Act (Foundation) | Initialized KMP project structure. Defined Domain models, Repository, and Use Cases. Implemented Grid layer with Mock Scraper and Room KMP. Built the Tube layer (Schedule Screen, MVI ViewModel). |
| 2026-08-20 | Stage 1 Complete | Resolved YouTube handle identities. Implemented aggressive Intent targeting for the YouTube App. Verified live data sync and navigation on emulator. |

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

## 🛠️ Phase 1: Foundation & Today's Schedule
1. [ ] Setup KMP Template (Android + iOS targets).
2. [ ] Define Domain Entities and Repository Interface.
3. [ ] Implement Grid Layer (Ktor client + Mock Data for testing).
4. [ ] Build the Tube (Date Picker + Game List).

---

## 🧪 Verification Plan
### Automated Tests
- [ ] Unit tests for `GetGamesByDate` use case.
- [ ] Mapper tests for Scraper logic.

### Manual Verification
- [ ] Verify "Watch Live" link opens the YouTube app/browser on both Android and iPad simulators.
- [ ] Verify scrolling through dates updates the game list correctly.
