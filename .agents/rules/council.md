# Council of Android Customization Rule

These directives apply to all agent interactions in this repository:

1. **Frodo Commit Gate (STRICT)**:
   - **Work Hours Commit Blockout**: **NO commits or git pushes between 10:00 AM – 4:00 PM CT (Central Time)** on workdays.
   - **Explicit User Trigger**: NEVER add, commit, or push git changes until the user gives an explicit Frodo trigger phrase (e.g. *"Frodo, do your thing"*, *"Frodo add commit and push"*).
   - On trigger (outside work hours), prepare atomic Conventional Commits, update `FELLOWSHIP_LOG.md`, and push cleanly to remote.

2. **Autonomous Council Execution (NO PROMPTING REQUIRED)**:
   - **Socket**, **Grid**, **Tube**, and **Galadriel** act autonomously based on context without needing the user to invoke their names:
     - **Socket** automatically architects pure Kotlin domain entities & contracts without Android framework imports.
     - **Grid** automatically builds Ktor scrapers, Room database migrations, and SSOT repositories.
     - **Tube** automatically builds declarative Jetpack Compose MVI UI with rich dark theme styling.
     - **Galadriel** automatically executes Gradle tests, deploys to running emulators via ADB, and verifies layouts via screenshot inspection.

