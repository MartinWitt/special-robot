---
name: spoon-diff-api
description: Diff the public/protected API between two Java source trees — added, removed and changed signatures with before/after. Use for PR review ("is this an ABI break?"), release notes, and migration guides.
---

## When to use
- Reviewing a PR that touches library code — answer "what did this change in the public surface?" without reading every diff hunk
- Generating a CHANGELOG entry or release notes
- Writing a migration guide for a major version bump
- Sanity-checking that an internal refactor didn't accidentally change anything visible

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar diff \
  --before <previous-source-directory> \
  --after  <new-source-directory>
```

Both arguments are source directories — for a git-ref diff, check out the two refs into separate worktrees and pass those paths.

**Example:**
```bash
git worktree add /tmp/before main
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar diff \
  --before /tmp/before/src/main/java \
  --after  ./src/main/java
```

## Output
```json
{
  "added": [
    { "classFqn": "com.example.Repo", "signature": "public Optional<User> findByEmail(String email)" }
  ],
  "removed": [
    { "classFqn": "com.example.Repo", "signature": "public User load(Long id)" }
  ],
  "changed": [
    {
      "classFqn": "com.example.PaymentGateway",
      "before": "public void charge(BigDecimal amount)",
      "after":  "public void charge(BigDecimal amount) throws PaymentException"
    }
  ]
}
```

## How to interpret
- `added`: methods/constructors present in `after` but not `before` — these are new API.
- `removed`: methods/constructors present in `before` but not `after` — these are **breaking changes** for downstream callers.
- `changed`: same compact key (`name(SimpleType,SimpleType)`) but full signature differs — return type changed, generics changed, `throws` clause changed, or modifiers changed. Each entry has `before` and `after` full signatures.
- Only `public` and `protected` members are diffed. Private and package-private members are intentionally invisible.
- A method whose parameter types changed (e.g. `find(Long)` → `find(String)`) appears as one `added` + one `removed` rather than a `changed`, because callers cannot transparently follow that rename.

Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
