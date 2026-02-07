# Memory Bank

This directory contains persistent context files for AI-assisted development sessions.

## Files
| File | Purpose |
|------|---------|
| `projectbrief.md` | Goals, constraints, stakeholders, success criteria |
| `activeContext.md` | Current focus, blockers, working notes |
| `SESSION.md` | Append-only session log with semantic versioning |
| `master-plan.md` | Milestones and upcoming work |
| `plans/` | Directory for implementation plans |
| `README.md` | This file — usage guide |

## Usage
1. Read files in order: `projectbrief.md` → `activeContext.md` → `SESSION.md` → `README.md`
2. Update `activeContext.md` as focus changes
3. Append to `SESSION.md` at session start/end
4. Update `master-plan.md` when milestones change
5. Store plans in `plans/` directory alongside other context files

## Rules
- `SESSION.md` is append-only (never delete entries)
- Use semantic versioning: `vX.Y.Z` (major.minor.patch)
- Keep `activeContext.md` current — it's the "working memory"
- Plans are stored in `plans/` directory alongside other context files
