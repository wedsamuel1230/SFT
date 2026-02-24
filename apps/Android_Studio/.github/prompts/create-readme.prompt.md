---
agent: 'agent'
description: 'Create a README.md file for the project'
---

## Performance & Session Discipline

### Subagent Delegation (Best Performance)
For complex or multi-domain tasks, use `#runSubagent` to delegate to specialized agents:
- **documentation-engineer** — README structure and formatting
- **architect-reviewer** — Architecture overview sections
- **seo-specialist** — Project visibility and discoverability
- Domain-specific agents for technology-specific sections

Parallel subagent calls yield faster results when tasks are independent.

### Memory Bank Discipline
Before executing, ensure `memory-bank/` exists; read in order:
1. `memory-bank/projectbrief.md` → 2. `memory-bank/activeContext.md` → 3. `memory-bank/SESSION.md` → 4. `README.md`

Log sessions as `YYYY-MM-DD — vX.Y.Z` in `SESSION.md`; update `master-plan.md` for multi-step work.

---

## Role

You're a senior expert software engineer with extensive experience in open source projects. You always make sure the README files you write are appealing, informative, and easy to read.

## Task

1. Take a deep breath, and review the entire project and workspace, then create a comprehensive and well-structured README.md file for the project.
2. Take inspiration from these readme files for the structure, tone and content:
   - https://raw.githubusercontent.com/Azure-Samples/serverless-chat-langchainjs/refs/heads/main/README.md
   - https://raw.githubusercontent.com/Azure-Samples/serverless-recipes-javascript/refs/heads/main/README.md
   - https://raw.githubusercontent.com/sinedied/run-on-output/refs/heads/main/README.md
   - https://raw.githubusercontent.com/sinedied/smoke/refs/heads/main/README.md
3. Do not overuse emojis, and keep the readme concise and to the point.
4. Do not include sections like "LICENSE", "CONTRIBUTING", "CHANGELOG", etc. There are dedicated files for those sections.
5. Use GFM (GitHub Flavored Markdown) for formatting, and GitHub admonition syntax (https://github.com/orgs/community/discussions/16925) where appropriate.
6. If you find a logo or icon for the project, use it in the readme's header.
