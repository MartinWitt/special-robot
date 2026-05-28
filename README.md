# spoon-claude-tools

> ⚠️ **Work in progress, personal sandbox.** I built this to try a few things with [Spoon](https://spoon.gforge.inria.fr/) and Claude Code's skill system. It is **not** a maintained library. Don't depend on it. APIs, output schemas, CLI flags and package names may change at any moment, or the whole thing may be deleted. No guarantees about correctness, stability or backwards compatibility. Issues and PRs are not tracked.

## What it is

A Gradle multi-module project (`tools` + `app`) that exposes Spoon AST queries as a small CLI, plus a handful of Claude Code skills that tell the model when and how to call them.

Goal of the experiment: see whether a focused, AST-aware tool helps an LLM coder more than letting it grep + read whole files. Each skill answers one concrete refactoring question:

| CLI command | Skill file | Question it answers |
|---|---|---|
| `context`   | `skills/spoon-context.md`        | What does *this method* do, without reading the whole file? |
| `api`       | `skills/spoon-api.md`            | What is the public surface of *this class*? |
| `summary`   | `skills/spoon-class-summary.md`  | Give me orientation for *this class*: docs, hierarchy, API, outbound deps. |
| `callgraph` | `skills/spoon-callgraph.md`      | Who calls *this method*, and what does it call, N levels deep? |
| `impact`    | `skills/spoon-impact.md`         | What breaks if I change *this class or method*? |
| `impl`      | `skills/spoon-impl-finder.md`    | Where is *this interface* actually implemented? |
| `testmap`   | `skills/spoon-testmap.md`        | Which tests cover *this class*, which public methods are uncovered? |
| `diff`      | `skills/spoon-diff-api.md`       | What changed between *these two source trees* at the public-API level? |

## Build

```bash
./gradlew build
```

The shadowed CLI lands at `app/build/spoon-claude-dist/spoon-claude.jar` next to a copy of the skill markdowns.

## Run

```bash
java -jar app/build/spoon-claude-dist/spoon-claude.jar --help
java -jar app/build/spoon-claude-dist/spoon-claude.jar context \
  --file src/main/java \
  --element com.example.UserService#findById
```

Output is JSON on stdout; errors go to stderr as `{"error": "..."}` with exit code 1.

## What you should know before using anything here

- Java 21, Spoon 11.3.x, runs with `setNoClasspath(true)` — type resolution is **best-effort**. Tools fall back from FQN matching to simple-name matching where Spoon couldn't resolve a reference. Expect occasional false positives in heavily polymorphic code.
- The model is cached in-process by `(canonicalPath, max .java mtime)`. Disable with `-Dspoon.cache.disable=true`.
- Tests live in `tools/src/test/...` and are the only thing I trust to still be correct on any given day.

That's it. Have fun, expect nothing.
