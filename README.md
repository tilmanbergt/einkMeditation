## Philosophy

This app is guided by the following principles:

- **E‑ink friendly.** Minimal animations, paging instead of fast scrolling, high contrast.
- **Simplicity and minimal UI** reduced UI and minimal feature set
- **Distraction Free** the core features are centered around making the meditation experience itself as calm and distraction free as possible (turn of backlight, adjustable signals, minimum movement on the screen).
- **Simple feedback beats dashboards.** A few meaningful signals (today / recent average / streak) are enough.
- **Encouragement > judgment.** The App is intended to help focus on what went well and what is possible.

---

## Features

- Meditation timer
    - with start/end cues (bell / vibrartion / light flashing)
    - turn background light off during meditation
    - hide meditation time display during meditation
    - adjustable pre-meditation countdown
    - allow additional time to be tracked (or not)
    - use predefined meditation times or adjust to any time (minutes)
- Practice overview with:
  - “Today”
  - “7‑day horizon” (restart-aware, may show fewer days)
  - “30‑day horizon” (only shown when meaningful)
  - motivating headline + suggestion for today
  - streak tracking
- Session list with **paging** and manual **add/delete** (useful for meditation retreats)
- general features
  - supports English and German language
  - you can set your own long term and short term goals, the app will use the to provide you with meaningfull feedback and suggestions.

## Developer and Support
This is developed as a personal project by Tilman Bergt

## Intended Features
- allow interval bells (e.g. a bell every 10 minutes during a meditation session)

Major evolutions
- allow custom practices presets (default time, sound settings etc.) to be defined and saved
---



## Install / Build

### Requirements
- Android Studio (recent)
- JDK 17 (recommended)

### Build & run
1. Clone:
   ```bash
   git clone https://github.com/tilmanbergteinkMeditation.git
   ```
2. Open in Android Studio
3. Let Gradle sync finish
4. Run on device/emulator

### Create a release APK (manual)
Android Studio:
- **Build → Generate Signed Bundle / APK → APK**
- Output is typically under:
  `app/release/app-release.apk`

---

## Getting started (developer)

Recommended branch workflow:
- `main` = stable, “should run”
- `dev` = active work

See: [`docs/git-workflow.md`](docs/git-workflow.md)

---

## Architecture (high level)

- **data/**  
  Room database for session logging (reliable across updates).
- **history/**  
  “Insights” / aggregation layer:
  - computes today minutes, rolling horizons, restart-aware windows
  - suggestion logic (today vs week vs month)
  - streak logic
- **ui activities**  
  - History screen: renders key metrics + headline + suggestion
  - Session list: paged list, add/delete sessions

Note: there are some classes and activities currently not used or underused (particularly around practices). I intended to later use them to introduce new features.
---

## Contributing

This project is still evolving. If you want to contribute later:
- open an issue with observations (especially UX, motivation, e‑ink constraints)
- propose small PRs focusing on clarity and simplicity

---

## License

TBD.
