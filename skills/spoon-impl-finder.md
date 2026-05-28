---
name: spoon-impl-finder
description: Find every concrete implementation of a Java interface or abstract class, with the list of abstract methods each implementer actually overrides. Use to answer "where is this interface implemented?" in one shot — no grep, no false positives from comments.
---

## When to use
- Before tracing how an interface is actually wired up
- When choosing which implementation to read first
- When deciding the blast radius of changing an interface
- To find partial implementations (abstract classes that implement only some methods of an interface)

## How to run

```bash
java -jar /PATH/TO/spoon-claude.jar impl \
  --src <source-directory> \
  --type <ClassName | com.example.ClassName>
```

`--type` accepts a simple name or a fully-qualified name. On ambiguous simple names re-run with one of the FQNs the tool prints.

**Example:**
```bash
java -jar ~/tools/spoon-claude.jar impl \
  --src src/main/java \
  --type com.example.PaymentGateway
```

## Output
```json
{
  "target": "com.example.PaymentGateway",
  "implementations": [
    {
      "classFqn": "com.example.stripe.StripeGateway",
      "overriddenMethods": ["charge(BigDecimal)", "refund(String)"]
    },
    {
      "classFqn": "com.example.PartialGateway",
      "overriddenMethods": ["charge(BigDecimal)"]
    }
  ]
}
```

## How to interpret
- Each `implementations` entry is a class that either directly implements the interface, extends an abstract class, or transitively inherits the relationship through a parent.
- `overriddenMethods` lists the abstract methods from the target that this class actually implements (compact `name(SimpleType,SimpleType)` form). If the list is shorter than the target's abstract surface, the implementer is still abstract or relies on a parent.
- Empty `implementations` means nothing in the scanned sources implements the target.

Do NOT grep for `implements X` afterwards — this tool already covers direct, extended-class, and transitive cases.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
