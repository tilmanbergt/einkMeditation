# Git workflow (main + dev)

This project uses two long-lived branches:

- **main**: stable, “should build & run”
- **dev**: ongoing development

Optionally create feature branches off `dev` for experiments.

---

## Daily work (dev)

### 1) Switch to dev and pull
```bash
git checkout dev
git pull
```

### 2) Commit your changes
```bash
git status
git add .
git commit -m "Describe change briefly"
```

### 3) Push dev to GitHub (share current dev state)
```bash
git push
```

---

## Merge dev → main (publish stable state)

### Option A (recommended): GitHub Pull Request
1. Push `dev`:
   ```bash
   git checkout dev
   git pull
   git push
   ```
2. On GitHub:
   - Pull requests → New pull request
   - Base: `main`  Compare: `dev`
   - Create PR, review, Merge

### Option B: merge locally
```bash
git checkout main
git pull
git merge dev
git push
```

---

## Releases (APK attached on GitHub)

### 1) Tag a release
```bash
git checkout main
git pull
git tag v0.1.0
git push origin v0.1.0
```

### 2) Build release APK
Android Studio:
- Build → Generate Signed Bundle / APK → APK
- Output usually: `app/release/app-release.apk`

### 3) Publish on GitHub
GitHub:
- Releases → Draft a new release
- Choose tag `v0.1.0`
- Upload the APK as an asset
- Publish release

---

## Sharing a dev state for ChatGPT review

1. Push your work:
   ```bash
   git checkout dev
   git push
   ```
2. In ChatGPT, say:
   - repo URL
   - branch name (`dev`)
   - file paths you want reviewed

Example:
> Please review branch `dev`:
> - `app/src/main/java/.../MeditationInsightsRepository.java`
> - `app/src/main/java/.../HistoryActivity.java`
