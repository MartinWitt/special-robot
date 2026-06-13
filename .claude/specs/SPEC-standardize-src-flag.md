---
ticket: none
status: impl
---

## Requirements

Three commands — `api`, `callgraph`, and `context` — accept `--file` as the source path flag, while the other 7 commands (`summary`, `impl`, `impact`, `testmap`, `routes`, `cyclomatic`, `annotation`) all use `--src`. This inconsistency causes invocation failures for users who expect the uniform `--src` convention (as experienced when calling `api --src ...`).

Standardize all commands to `--src` (the majority convention). The `DiffCommand` uses `--before`/`--after` which is correct semantically and should not change.

Affected commands:
- `ApiCommand`: `--file` → `--src`
- `CallGraphCommand`: `--file` → `--src`
- `ContextCommand`: `--file` → `--src`

Affected skill reference docs:
- `spoon-api.md`: update example from `--file` to `--src`
- `spoon-callgraph.md`: update if it documents `--file`
- `spoon-context.md`: update if it documents `--file`

## Acceptance Criteria

1. `java -jar spoon-claude.jar api --src <path> --class <name>` runs without error
2. `java -jar spoon-claude.jar callgraph --src <path> --method <name>` runs without error
3. `java -jar spoon-claude.jar context --src <path> --element <name>` runs without error
4. `--file` no longer accepted on any of the three commands (clean break, not aliased)
5. All existing tests pass
6. Skill reference docs reflect `--src` in their examples

## Out of Scope

- Changing `DiffCommand` (`--before`/`--after` is correct semantics)
- Adding backward-compat `--file` alias
- Changing any other flag names

## Technical Notes

- picocli `@Option(names = "--src")` is the only change needed per command
- Internal field name can stay `file` or be renamed to `src` for clarity
- Skill reference `.md` files live in `~/.claude/skills/spoon-claude/references/`
- JAR must be rebuilt after the change: `./gradlew shadowJar`
