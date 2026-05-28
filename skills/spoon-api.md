---
name: spoon-api
description: Dump the public/protected API surface of a Java class — constructors, methods, fields with signatures, modifiers, throws, generics and javadoc, zero implementation details. Use to understand an interface before implementing against it or writing tests.
---

## When to use
- Before writing code that calls a class you're unfamiliar with
- When generating tests for a class (see what public methods exist)
- When reviewing whether a class's API is clean and well-documented

## How to run

```bash
java -jar /PATH/TO/spoon-claude.jar api \
  --file <path-to-file-or-directory> \
  --class <ClassName | com.example.ClassName>
```

`--class` accepts either a simple name or a fully-qualified name. On ambiguous simple names the tool fails with `Ambiguous type 'X', qualify with one of: ...` — re-run with one of the listed FQNs.

**Example:**
```bash
java -jar ~/tools/spoon-claude.jar api \
  --file src/main/java \
  --class com.example.UserRepository
```

## Output
```json
{
  "className": "com.example.UserRepository",
  "classJavadoc": "Repository for User entities.",
  "constructors": [
    {
      "signature": "public UserRepository(EntityManager em)",
      "javadoc": "",
      "annotations": ["@Inject"]
    }
  ],
  "methods": [
    {
      "signature": "public final Optional<User> findById(Long id) throws DataAccessException",
      "javadoc": "Find a user by their primary key.",
      "annotations": ["@Query", "@Transactional"]
    }
  ],
  "fields": [
    {
      "type": "EntityManager",
      "name": "em",
      "annotations": ["@PersistenceContext"]
    }
  ]
}
```

## How to interpret
- `signature` already encodes modifiers, generic type arguments and `throws` clauses — do not fetch the source for this.
- Read `methods` and `constructors` to understand what the class offers; `javadoc` explains intent.
- `fields` shows injected dependencies of the class.
- No method bodies are included — that is intentional.

Do NOT read the full source file after running this — use the API surface to form your understanding.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
