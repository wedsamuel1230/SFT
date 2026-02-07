---
name: rename-and-cleanup
description: Rename files in bulk and clean duplicate content using smart-rename and fdupes. Use for organizing folders, standardizing names, and removing duplicates safely.
license: Proprietary. LICENSE.txt has complete terms
---

# Rename and Cleanup

## Overview
Provide a safe workflow for bulk renaming and duplicate cleanup with reversible steps.

## When to Use
- The user wants to standardize filenames or folder structures
- The user wants to detect and remove duplicates
- The user needs a report of changes before applying them

## Required Inputs
- Target folder(s)
- Naming convention preferences
- Duplicate policy (delete, keep newest, or keep largest)

## Workflow (Sequential)
1. Inventory target files and current naming patterns
2. Draft rename rules and preview results
3. Resolve collisions and ordering rules
4. Run duplicate detection
5. Review duplicate policy with the user
6. Apply renames and cleanup
7. Provide a summary report and rollback plan

## Safety Rules
- Always preview before applying changes
- Avoid destructive deletes without confirmation
- Keep a rollback plan (rename map or backup)

## Output Expectations
- List of proposed and applied renames
- List of duplicates detected and action taken
- Final summary of files changed/removed

## Error Handling
- Name collisions → add unique suffixes or preserve indices
- Permission errors → skip and report
- Locked files → defer and report

## References
- Read `references/rename-rules.md` for pattern guidance
- Read `references/dedupe-policy.md` for safe duplicate handling
