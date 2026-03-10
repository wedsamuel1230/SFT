# Memory Bank

This directory contains persistent context files for AI-assisted development sessions.

## Files

| File | Purpose |
| ------ | ------- |
| `projectbrief.md` | Goals, constraints, stakeholders, success criteria |
| `activeContext.md` | Current focus, blockers, working notes |
| `SESSION.md` | Append-only session log with semantic versioning |
| `master-plan.md` | Milestones and upcoming work |
| `plans/` | Directory for implementation plans |
| `logs/` | Daily session logs |
| `archive/` | Archived session entries |
| `README.md` | This file |

## Usage

1. Read files in order: `projectbrief.md` -> `activeContext.md` -> `SESSION.md` -> `master-plan.md`
2. Update `activeContext.md` as focus changes
3. Append to `SESSION.md` at session start/end
4. Update `master-plan.md` when milestones change
5. Store plans in `plans/`

## Rules

- `SESSION.md` is append-only.
- Keep `activeContext.md` current during active work.
- `memory-bank/` must stay tracked in git.
