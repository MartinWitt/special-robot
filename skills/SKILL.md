---
name: spoon-claude
description: Java AST analysis toolkit built on Spoon. Bundles eight read-only commands that answer common refactoring questions — method context, call graphs, public API, API diffs, blast radius of a change, test mapping, interface implementations, class summaries, and HTTP routes — without reading whole files. Use whenever working on a Java codebase where you need precise, type-aware answers rather than grep + guesses.
---

## When to use
Pick the matching sub-skill when you need a precise, AST-level answer that grep would miss or that would otherwise require reading multiple files. All commands operate on Java source directories, return JSON to stdout, and surface errors to stderr.

## The JAR
The bundle ships `scripts/spoon-claude.jar`. Resolve its path relative to this skill folder and invoke commands as:

```bash
java -jar /PATH/TO/SKILL/scripts/spoon-claude.jar <command> --src <source-directory> [options]
```

Every command exits with code 1 on error and prints `{"error": "..."}` to stderr.

## Sub-skills (in `references/`)
| Sub-skill | Command | Answers |
|---|---|---|
| `spoon-context` | `context` | What does this one method do? (~70% token savings vs full-file read) |
| `spoon-callgraph` | `callgraph` | Who calls this method, and what does it call, N levels deep? |
| `spoon-api` | `api` | What is the public/protected API surface of this class? |
| `spoon-class-summary` | `summary` | One-shot orientation: javadoc, supertypes, public members, outbound deps |
| `spoon-impl-finder` | `impl` | Where is this interface or abstract class actually implemented? |
| `spoon-impact` | `impact` | What breaks if I change this method or type? |
| `spoon-diff-api` | `diff-api` | What changed in the public API between two source trees? |
| `spoon-testmap` | `testmap` | Which tests cover this class? Which methods are uncovered? |
| `spoon-spring-routes` | `routes` | Which HTTP endpoints does this project expose (Spring MVC / JAX-RS / WebFlux Functional)? |

Read the matching `references/<sub-skill>.md` for invocation details, JSON schema, and interpretation guidance before running.

## How to choose
- Just need to read one method → `spoon-context`
- First contact with an unfamiliar class → `spoon-class-summary`
- Planning a refactor → `spoon-impact` first, then `spoon-callgraph` to trace specifics
- PR review of a library change → `spoon-diff-api`
- "Where is X implemented?" → `spoon-impl-finder`
- "Which endpoints does this expose?" → `spoon-spring-routes`
- Test coverage gaps → `spoon-testmap`

Do NOT fall back to grep + full-file reads after consulting these tools — the JSON output is designed to be self-sufficient for the question it answers.
