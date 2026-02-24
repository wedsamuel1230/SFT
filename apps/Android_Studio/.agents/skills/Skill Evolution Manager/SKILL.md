---
name: Skill Evolution Manager
description: A core tool specifically designed to summarize, optimize and iterate existing Skills based on user feedback and conversation content at the end of a conversation. It continuously evolves the Skills library by drawing on the "best" of the conversation (such as successful solutions, failed lessons, specific code specifications).
license: MIT
---

# Skill Evolution Manager

This is the "evolutionary center" of the entire AI skill system. It is not only responsible for optimizing a single Skill, but also responsible for reviewing and accumulating experience across Skills.

## Core responsibilities

1. **Session Review**: At the end of the conversation, analyze the performance of all called Skills.
2. **Experience Extraction**: Convert unstructured user feedback into structured JSON data (`evolution.json`).
3. **Smart Stitching**: Automatically write accumulated experience to `SKILL.md` to ensure persistence and is not overwritten by version updates.

## Usage scenarios

**触发**：
- `/evolve`
- "Review the conversation just now"
- "I don't think the tool just now is easy to use. Let's record it."
- "Save this experience to Skill"

## 工作流（The Evolution Workflow）

### 1. Review & Extract
When the user triggers a review, the Agent must execute:
1. **Scan context**: Find the points where the user is dissatisfied (error, wrong style, wrong parameters) or the points where they are satisfied (specific Prompt works well).
2. **Location Skill**: Determine which Skill needs to evolve (such as `yt-dlp` or `baoyu-comic`).
3. **Generate JSON**: Build the following JSON structure in memory: 
```json 
{ 
"preferences": ["User wants download by default mute"], 
"fixes": ["ffmpeg path needs to be escaped under Windows"], 
"custom_prompts": "Always print the estimated time before execution" 
} 
```

### 2. Experience Persistence
The Agent calls `scripts/merge_evolution.py` to write the above JSON increment to the `evolution.json` file of the target Skill.
- **Command**: `python scripts/merge_evolution.py <skill_path> <json_string>`

### 3. Document Stitch
The Agent calls `scripts/smart_stitch.py` to convert the contents of `evolution.json` to Markdown and append it to the end of `SKILL.md`.
- **Command**: `python scripts/smart_stitch.py ​​<skill_path>`

### 4. Cross-version alignment (Align)
When `skill-manager` updates a certain Skill, the Agent should actively run `smart_stitch.py` to "restitch" the previously saved experience into the new version of the document.

## Core script

- `scripts/merge_evolution.py`: **Incremental merging tool**. Responsible for reading the old JSON, re-merging the new List, and saving it.
- `scripts/smart_stitch.py`: **Document generation tool**. Responsible for reading JSON, generating or updating the `## User-Learned Best Practices & Constraints` chapter at the end of `SKILL.md`.
- `scripts/align_all.py`: **Full alignment tool**. Traverse all Skill folders with one click and re-stitch the existing `evolution.json` experience back to the corresponding `SKILL.md`. Commonly used for experience restoration after `skill-manager` batch updates.

## Best practices

- **Do not modify the body of SKILL.md directly**: Unless it is an obvious spelling error. All experience corrections should be made through the `evolution.json` channel to ensure that experience is not lost when the skill is upgraded.
- **Multi-Skill Collaboration**: If a conversation involves multiple Skills, perform the above process for each Skill in turn.