---
name: spoon-annotation-usage
description: Find all usages of a Java annotation across a source tree — classes, methods, fields, parameters. Avoids grep false positives from annotations mentioned in comments or string literals.
---

## When to use
- Auditing annotation consistency: "is @Transactional applied everywhere it should be?"
- Finding all `@Cacheable` methods before changing cache config.
- Locating every `@Deprecated` element before a library upgrade.
- Any time grep would match annotation names in comments or Strings.

Do NOT use to understand what an annotated element does — use `spoon-context` for that.

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar annotation \
  --src <source-directory> \
  --annotation <AnnotationName>
```

`--annotation` accepts a simple name (`Transactional`) or a fully-qualified name (`org.springframework.transaction.annotation.Transactional`). Simple-name lookup matches any annotation with that name regardless of package.

**Example:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar annotation \
  --src src/main/java \
  --annotation Transactional
```

## Output
```json
{
  "annotationName": "Transactional",
  "usages": [
    {
      "elementFqn": "com.example.OrderService",
      "elementKind": "class",
      "declaringClass": "com.example.OrderService"
    },
    {
      "elementFqn": "com.example.OrderService#cancel()",
      "elementKind": "method",
      "declaringClass": "com.example.OrderService"
    },
    {
      "elementFqn": "com.example.PaymentService#charge(Amount)#amount",
      "elementKind": "parameter",
      "declaringClass": "com.example.PaymentService"
    }
  ]
}
```

`elementKind` is one of: `class`, `method`, `field`, `parameter`.  
`elementFqn` format: class → FQN; method → `Class#method(params)`; field → `Class#fieldName`; parameter → `Class#method(params)#paramName`.  
Results are sorted by `elementFqn`.

## How to interpret the output
1. Count usages by `elementKind` to understand the annotation's usage pattern.
2. Use `declaringClass` to group by class — missing classes may indicate inconsistent application.
3. Cross-reference with `spoon-impact` on `declaringClass` to understand the blast radius of annotation changes.

Do NOT read source files to find annotation usages — use only what this tool returns.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
