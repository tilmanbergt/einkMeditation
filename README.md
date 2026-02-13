# Bodhisattva Friend (Android)

A minimalist meditation companion designed for **consistency, kindness, and clarity**—especially on **e‑ink** devices.

The core idea: make it *easy to practice today*, and make progress feel *warm and motivating* without turning practice into “performance”.

---

## Philosophy

This app is guided by a few simple principles:

- **Small wins matter.** If you sat today, that’s a win.
- **Restarting is a skill.** The UI should be especially supportive after a pause.
- **Simple feedback beats dashboards.** A few meaningful signals (today / recent average / streak) are enough.
- **Encouragement > judgment.** When you’re on track, you get a thumbs‑up. When you’re off track, you get a gentle, doable next step.
- **E‑ink friendly.** Minimal animations, paging instead of fast scrolling, high contrast.

---

## Features (current direction)

- Meditation timer with start/end cues
- History overview with:
  - “Today”
  - “7‑day horizon” (restart-aware, may show fewer days)
  - “30‑day horizon” (only shown when meaningful)
  - motivating headline + suggestion for today
  - streak tracking
- Session list with **paging** and manual **add/delete** (useful for sesshins and testing)

---

## Install / Build

### Requirements
- Android Studio (recent)
- JDK 17 (recommended)

### Build & run
1. Clone:
   ```bash
   git clone https://github.com/tilmanbergt/bodhisattva-friend.git
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

The intent is to keep computation in small, testable methods (≈ 10 lines each where possible),
so edge cases can be validated and refined over time.

---

## Using ChatGPT with the GitHub app/connector

If you connect GitHub as an “app” inside ChatGPT, you can ask questions about this repo
without pasting files. See: [`docs/chatgpt-github-app.md`](docs/chatgpt-github-app.md)

---

## Contributing (future-friendly)

This project is still evolving. If you want to contribute later:
- open an issue with observations (especially UX, motivation, e‑ink constraints)
- propose small PRs focusing on clarity and simplicity

---

## License

TBD. (Decide before inviting external contributors.)
