---
name: spoon-class-summary
description: One-shot orientation for an unfamiliar Java class — class javadoc, extends/implements, annotations, constructors, public API, fields, and outbound type dependencies in a single JSON. Use on first contact with a class to skip a full file read.
---

## When to use
- First time you look at a class — get the shape before any line of code
- Before writing tests or callers
- Before deciding which method to read in detail with `spoon-context`

This skill is a superset of `spoon-api` plus class-level metadata and dependency graph.

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar summary \
  --src <source-directory> \
  --class <ClassName | com.example.ClassName>
```

`--class` accepts a simple name or a fully-qualified name.

**Example:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar summary \
  --src src/main/java \
  --class com.example.OrderService
```

## Output
```json
{
  "className": "com.example.OrderService",
  "javadoc": "Coordinates order placement and inventory.",
  "classAnnotations": ["@Service", "@Transactional"],
  "superClass": "com.example.BaseService",
  "interfaces": ["com.example.Auditable"],
  "constructors": [
    {
      "signature": "public OrderService(OrderRepo repo, Mailer mailer)",
      "javadoc": "",
      "annotations": ["@Inject"]
    }
  ],
  "methods": [
    {
      "signature": "public Order place(Cart cart) throws PaymentException",
      "javadoc": "Place an order from a cart.",
      "annotations": ["@Retryable"]
    }
  ],
  "fields": [
    { "type": "OrderRepo", "name": "repo", "annotations": [] },
    { "type": "Mailer", "name": "mailer", "annotations": [] }
  ],
  "outboundDependencies": ["com.example.Cart", "com.example.Mailer", "com.example.Order", "com.example.OrderRepo", "com.example.PaymentException"]
}
```

## How to interpret
- `javadoc` is the class-level javadoc, normalized (leading `*` stripped).
- `superClass` is `null` if the class extends nothing (or extends `Object` implicitly — Spoon may report it as `java.lang.Object`).
- `interfaces` are FQNs in alphabetical order.
- `constructors` / `methods` carry full signatures from `SignatureBuilder` (modifiers, generics, throws).
- `outboundDependencies` lists every distinct non-primitive, non-`java.lang.*`, non-self type referenced anywhere in this class (fields, parameters, return types, method bodies). Sorted alphabetically. Use it to scope the next investigation — these are the types this class actually talks to.

Do NOT read the source file after running this — the summary is intentionally complete enough to plan further work.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
