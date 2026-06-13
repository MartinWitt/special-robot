---
name: spoon-hierarchy
description: Show the full type hierarchy for a Java class or interface — supertypes (transitive chain up) and subtypes (direct project-local descendants) — in one nested JSON tree. Use to answer "what does this type extend?" and "what extends this type?" without reading source files.
---

## When to use
- Before changing a base class or interface — see who is affected
- When tracing where a method is declared up the inheritance chain
- To understand the full polymorphism surface before a refactor
- When `spoon-impl-finder` gives too little (it only finds implementations of interfaces/abstract classes; this covers the full class hierarchy too)

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar hierarchy \
  --src <source-directory> \
  --type <ClassName | com.example.ClassName> \
  [--supertypes-only] [--subtypes-only] [--include-object]
```

`--type` accepts a simple name or FQN. On ambiguous simple names re-run with one of the FQNs the tool prints.

**Options:**
- `--supertypes-only` — omit subtypes section
- `--subtypes-only` — omit supertypes section
- `--include-object` — include `java.lang.Object` at the root of the supertype chain

**Example:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar hierarchy \
  --src src/main/java \
  --type com.example.AbstractRepository
```

## Output

```json
{
  "fqn": "com.example.AbstractRepository",
  "simpleName": "AbstractRepository",
  "kind": "class",
  "source": "project",
  "supertypes": [
    {
      "fqn": "com.example.BaseComponent",
      "simpleName": "BaseComponent",
      "kind": "class",
      "source": "project",
      "supertypes": []
    },
    {
      "fqn": "java.io.Serializable",
      "simpleName": "Serializable",
      "kind": "interface",
      "source": "classpath",
      "supertypes": []
    }
  ],
  "subtypes": [
    {
      "fqn": "com.example.UserRepository",
      "simpleName": "UserRepository",
      "kind": "class",
      "source": "project",
      "supertypes": []
    }
  ],
  "subtypesScope": "project-local only"
}
```

## How to interpret
- `kind`: `class`, `interface`, `enum`, or `record`
- `source: "project"` — type is in the scanned source tree; its `supertypes` are recursively expanded
- `source: "classpath"` — type is from the JDK or a dependency; it appears as a leaf with no further expansion
- `subtypesScope: "project-local only"` — subtypes from the classpath (e.g. `ArrayList` as a subtype of `List`) are not shown
- `supertypes` contains both the superclass and all implemented interfaces as siblings in the array
- A class with no explicit superclass and `--include-object` not set will have an empty `supertypes` array

Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
