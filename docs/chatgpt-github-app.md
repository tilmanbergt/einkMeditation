# ChatGPT GitHub app/connector: what it is & how to use it

OpenAI has renamed “connectors” to **apps** in ChatGPT. GitHub can be connected as an app so ChatGPT can
**search and read your repositories** directly. (See OpenAI Help Center: “Apps in ChatGPT”.)

## What you get (advantages)

- **No more file uploads**: ask questions and ChatGPT can reference the repo directly.
- **Live context**: ChatGPT searches your repo content (code, README, docs) to answer questions.
- **Works with private repos in your own account**, once authorized.
- **Faster iteration**: “Please review file X on branch dev” becomes the normal flow.

## Important limitation

The GitHub app in ChatGPT is **read-only** for typical ChatGPT usage: it can analyze and search,
but it **cannot push commits or open PRs**.

## How to connect (once)

In ChatGPT:
1. Settings → Apps (or Connected apps) → GitHub → Connect
2. Authorize on GitHub and select which repos ChatGPT may access.

Note: it can take a few minutes until repos appear (index delay).

## How to use it day-to-day

### In a chat
Ask targeted questions and mention repo/branch when relevant:
- “In branch dev, where is session paging implemented?”
- “Find where MeditationInsights recomputes on resume.”

### Good prompts
- “Search for the method that computes the restart-aware 7-day horizon.”
- “Where do we write sessions to Room, and how is deletion implemented?”
- “List all places that read AppSettings daily goal minutes.”

### Tips
- Keep prompts **specific** (class/method names, behavior).
- If a brand-new repo/branch doesn’t appear immediately, wait a few minutes and try again.

## Security & privacy notes (practical)
- Only authorize repos you want ChatGPT to access.
- Avoid committing secrets (API keys, tokens) to the repo.
