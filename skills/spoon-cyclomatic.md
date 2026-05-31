---
name: spoon-cyclomatic
description: Compute McCabe cyclomatic complexity per method for a Java class. Use to triage which methods to test or refactor first — no file reading required.
---

## When to use
- Deciding where to add tests first: highest complexity = highest risk.
- Triaging a refactoring: which method is hardest to reason about?
- Reviewing a PR for methods that became unexpectedly complex.

Do NOT use to understand what a method does — use `spoon-context` for that.

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar cyclomatic \
  --src <source-directory> \
  --class <ClassName>
```

`ClassName` may be a simple name or a fully-qualified name.

**Example:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar cyclomatic \
  --src src/main/java \
  --class com.example.OrderService
```

## Output
```json
{
  "className": "com.example.OrderService",
  "methods": [
    { "methodName": "cancel",   "signature": "cancel()",       "complexity": 1 },
    { "methodName": "charge",   "signature": "charge(Amount)", "complexity": 7 },
    { "methodName": "validate", "signature": "validate(Order)","complexity": 4 }
  ]
}
```

Methods are sorted alphabetically by name, then by signature for overloads.

## How to interpret the output

| Complexity | Meaning |
|---|---|
| 1–4 | Simple — easy to understand and test |
| 5–10 | Moderate — consider splitting or adding test coverage |
| 11–20 | Complex — high bug risk, prioritize testing |
| 21+ | Very complex — refactor strongly recommended |

**Counting rules:** base = 1, +1 per: `if`/`else-if`, `for`, `for-each`, `while`, `do-while`, `switch case`, `catch`, ternary `? :`, `&&`, `||`.

Do NOT read the source file to compute complexity — use only what this tool returns.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
