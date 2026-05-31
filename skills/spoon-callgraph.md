---
name: spoon-callgraph
description: Show who calls a Java method and what it calls, up to N levels deep. Per-overload accurate — supports `Class#method(Type1,Type2)` disambiguation. Use before refactoring to understand the call chain without reading multiple files.
---

## When to use
- Before renaming or changing a method signature
- When tracing an execution path through unfamiliar code
- When you need to understand how deep a dependency chain goes

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar callgraph \
  --file <path-to-file-or-directory> \
  --method <ClassName#methodName[(Type1,Type2)]> \
  [--depth <1|2|3>]
```

Class names accept simple or fully-qualified form. For overloaded methods, append the parameter type list (simple names): `Logger#log(String)`.

**Example:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar callgraph \
  --file src/main/java \
  --method com.example.OrderService#placeOrder(Cart) \
  --depth 2
```

## Output
```json
{
  "target": "com.example.OrderService#placeOrder(Cart)",
  "callers": ["OrderController#checkout(HttpRequest)", "BatchProcessor#processOrders()"],
  "callees": ["PaymentService#charge(BigDecimal)", "InventoryService#reserve(SkuId,int)"],
  "depth": 2
}
```

## How to interpret
- `callers`: methods that call the target — these are affected if you change the signature.
- `callees`: methods the target calls — these are its dependencies. Each entry includes its parameter signature so overloads are distinguishable.
- At `depth > 1`, callees include transitive calls; the traversal avoids re-expanding the same `(declaring type FQN, signature)` pair but lists every distinct invocation reached.

Do NOT fetch source for each caller/callee separately — use the list to decide which ones actually need deeper inspection.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
