---
description: 'Transform this Python script into a polished, beginner-friendly project by refactoring the code, adding clear instructional comments, and generating a complete markdown tutorial.'
agent: 'agent'
---

## Performance & Session Discipline

### Subagent Delegation (Best Performance)
For complex or multi-domain tasks, use `#runSubagent` to delegate to specialized agents:
- **python-pro** — Python refactoring, best practices, type hints
- **documentation-engineer** — Tutorial structure, Markdown formatting
- **code-reviewer** — Code quality validation

Parallel subagent calls yield faster results when tasks are independent.

### Memory Bank Discipline
Before executing, ensure `memory-bank/` exists; read in order:
1. `memory-bank/projectbrief.md` → 2. `memory-bank/activeContext.md` → 3. `memory-bank/SESSION.md` → 4. `README.md`

Log sessions as `YYYY-MM-DD — vX.Y.Z` in `SESSION.md`; update `master-plan.md` for multi-step work.

---

Transform this Python script into a polished, beginner-friendly project by refactoring the code, adding clear instructional comments, and generating a complete markdown tutorial.

1. **Refactor the code**  
   - Apply standard Python best practices  
   - Ensure code follows the PEP 8 style guide  
   - Rename unclear variables and functions if needed for clarity

1. **Add comments throughout the code**  
   - Use a beginner-friendly, instructional tone  
   - Explain what each part of the code is doing and why it's important  
   - Focus on the logic and reasoning, not just syntax  
   - Avoid redundant or superficial comments

1. **Generate a tutorial as a `README.md` file**  
   Include the following sections:
   - **Project Overview:** What the script does and why it's useful  
   - **Setup Instructions:** Prerequisites, dependencies, and how to run the script  
   - **How It Works:** A breakdown of the code logic based on the comments  
   - **Example Usage:** A code snippet showing how to use it  
   - **Sample Output:** (Optional) Include if the script returns visible results  
   - Use clear, readable Markdown formatting
