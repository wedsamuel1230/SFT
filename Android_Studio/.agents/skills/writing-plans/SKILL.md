---
name: writing-plans
description: Use when you have a spec or requirements for a multi-step task, before touching code
---

# Writing Plans

## Overview

Write comprehensive implementation plans assuming the engineer has zero context for our codebase and questionable taste. Document everything they need to know: which files to touch for each task, code, testing, docs they might need to check, how to test it. Give them the whole plan as bite-sized tasks. DRY. YAGNI. TDD. Frequent commits.

Assume they are a skilled developer, but know almost nothing about our toolset or problem domain. Assume they don't know good test design very well.

**Announce at start:** "I'm using the writing-plans skill to create the implementation plan. First, I'll check the memory bank and gather context."

**Context:** This should be run in a dedicated worktree (created by brainstorming skill).

**Save plans to:** `memory-bank/plans/YYYY-MM-DD-<feature-name>.md`

## Memory Bank Workflow

**Before creating any plan, enforce the following memory bank workflow:**

### 1. Ensure Memory Bank Structure Exists
Check for and create missing files with agent templates if absent. Use the `create_file` tool to create any missing files with the appropriate agent templates. The memory bank directory must exist at `memory-bank/` with these files and directories:

**Directory:**
- `memory-bank/plans/` - Directory for storing implementation plans

**Files:**

**Agent Template for `memory-bank/projectbrief.md`:**
```markdown
# Project Brief

## Goal
[Describe the project goal in one sentence]

## Constraints
- Platform: [e.g., ESP32, XIAO nRF52840]
- Sensor: [e.g., MPU6050, LSM6DS3]
- Communication: [e.g., BLE, WiFi]
- Data Format: [e.g., CSV, JSON, Binary]

## Stakeholders
- [e.g., Developer, User, QA]

## Definition of Done
- [ ] [Task 1]
- [ ] [Task 2]

---
*Created: YYYY-MM-DD | Last Updated: YYYY-MM-DD*
```

**Agent Template for `memory-bank/activeContext.md`:**
```markdown
# Active Context

## Current Focus
[Describe current focus or objective]

## Open Questions / Blockers
- [Question 1]
- [Question 2]

## Notes
[Technical notes, decisions, or observations]

---
*Last Updated: YYYY-MM-DD*
```

**Agent Template for `memory-bank/SESSION.md`:**
```markdown
# Session Log

## YYYY-MM-DD — vX.Y.Z
**Objective:** [One sentence describing what this session accomplishes]

**Actions:**
- [Bullet point 1]
- [Bullet point 2]

**Status:** ✅ Complete | ⚠️ In Progress | ❌ Blocked

---
```

**Agent Template for `memory-bank/master-plan.md`:**
```markdown
# Master Plan

## Milestones
1. [ ] [Milestone 1]
2. [ ] [Milestone 2]

## Upcoming Work
- [ ] [Next task 1]
- [ ] [Next task 2]

## Completed
- [x] [Completed task 1]

---
*Last Updated: YYYY-MM-DD*
```

**Agent Template for `memory-bank/README.md`:**
```markdown
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
5. Store plans in `plans/` directory

## Rules
- `SESSION.md` is append-only (never delete entries)
- Use semantic versioning: `vX.Y.Z` (major.minor.patch)
- Keep `activeContext.md` current — it's the "working memory"
- Plans are stored in `plans/` directory alongside other context files
```

### 2. Read Files in Order (Context Gathering)
Read these files in the specified order to gather context:
1. `memory-bank/projectbrief.md` - Understand project goals and constraints
2. `memory-bank/activeContext.md` - Understand current focus and blockers
3. `memory-bank/SESSION.md` - Understand recent work and session history
4. `README.md` - Understand project structure and technical details
5. `memory-bank/plans/` - Review existing implementation plans for context

### 3. Enforce Session Log Format
All session log entries in `memory-bank/SESSION.md` MUST follow this format:
```markdown
## YYYY-MM-DD — vX.Y.Z
**Objective:** [One sentence describing what this session accomplishes]

**Actions:**
- [Bullet point 1]
- [Bullet point 2]

**Status:** ✅ Complete | ⚠️ In Progress | ❌ Blocked

---
```

**Versioning Rules:**
- Use semantic versioning (vX.Y.Z) for session entries
- Increment major version (X) for breaking changes or major milestones
- Increment minor version (Y) for new features or significant improvements
- Increment patch version (Z) for bug fixes and minor adjustments
- Append-only: Never modify or delete existing entries
- Each session gets a unique entry with current date and version

### 4. Update Master Plan for Multi-Step Work
When planning multi-step work, update `memory-bank/master-plan.md`:
- Add new milestones to the "Milestones" section
- Update "Upcoming Work" with next steps
- Mark completed items with `[x]` or `[ ]` checkboxes
- Update "Last Updated" timestamp

### 5. Plan Header References Memory Bank Context
Every plan header MUST reference relevant memory bank context:
```markdown
# [Feature Name] Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** [One sentence describing what this builds]

**Architecture:** [2-3 sentences about approach]

**Tech Stack:** [Key technologies/libraries]

**Plan Location:** `memory-bank/plans/YYYY-MM-DD-<feature-name>.md`

**Memory Bank Context:** [Reference relevant context from projectbrief.md, activeContext.md, or SESSION.md]

---
```

## Bite-Sized Task Granularity

**Each step is one action (2-5 minutes):**
- "Write the failing test" - step
- "Run it to make sure it fails" - step
- "Implement the minimal code to make the test pass" - step
- "Run the tests and make sure they pass" - step
- "Commit" - step

## Plan Document Header

**Every plan MUST start with this header:**

```markdown
# [Feature Name] Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** [One sentence describing what this builds]

**Architecture:** [2-3 sentences about approach]

**Tech Stack:** [Key technologies/libraries]

**Plan Location:** `memory-bank/plans/YYYY-MM-DD-<feature-name>.md`

**Memory Bank Context:** [Reference relevant context from projectbrief.md, activeContext.md, or SESSION.md]

---
```

## Task Structure

```markdown
### Task N: [Component Name]

**Memory Bank Updates:**
- Update: `memory-bank/activeContext.md` - [Describe what to update]
- Update: `memory-bank/master-plan.md` - [Mark milestone as complete if applicable]
- Append: `memory-bank/SESSION.md` - [Add session entry: YYYY-MM-DD — vX.Y.Z]
- Update: `memory-bank/plans/YYYY-MM-DD-<feature-name>.md` - [Mark task as complete]

**Files:**
- Create: `exact/path/to/file.py`
- Modify: `exact/path/to/existing.py:123-145`
- Test: `tests/exact/path/to/test.py`

**Step 1: Write the failing test**

```python
def test_specific_behavior():
    result = function(input)
    assert result == expected
```

**Step 2: Run test to verify it fails**

Run: `pytest tests/path/test.py::test_name -v`
Expected: FAIL with "function not defined"

**Step 3: Write minimal implementation**

```python
def function(input):
    return expected
```

**Step 4: Run test to verify it passes**

Run: `pytest tests/path/test.py::test_name -v`
Expected: PASS

**Step 5: Update Memory Bank**

Update `memory-bank/activeContext.md` with current progress and blockers.

**Step 6: Commit**

```bash
git add tests/path/test.py src/path/file.py memory-bank/activeContext.md memory-bank/SESSION.md memory-bank/plans/YYYY-MM-DD-<feature-name>.md
git commit -m "feat: add specific feature"
```
```

## Remember
- Exact file paths always
- Complete code in plan (not "add validation")
- Exact commands with expected output
- Reference relevant skills with @ syntax
- DRY, YAGNI, TDD, frequent commits

## Execution Handoff

After saving the plan, offer execution choice:

**"Plan complete and saved to `memory-bank/plans/<filename>.md`. Two execution options:**

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach?"**

**If Subagent-Driven chosen:**
- **REQUIRED SUB-SKILL:** Use superpowers:subagent-driven-development
- Stay in this session
- Fresh subagent per task + code review
- **Memory Bank Updates:** Each task must update `memory-bank/activeContext.md`, append to `memory-bank/SESSION.md`, and update `memory-bank/plans/<filename>.md`

**If Parallel Session chosen:**
- Guide them to open new session in worktree
- **REQUIRED SUB-SKILL:** New session uses superpowers:executing-plans
- **Memory Bank Updates:** Each task must update `memory-bank/activeContext.md`, append to `memory-bank/SESSION.md`, and update `memory-bank/plans/<filename>.md`
