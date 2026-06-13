---
name: spoon-context
description: Extract focused Java method context using Spoon AST — saves ~70% tokens vs reading the full file. Use when you need to understand a specific method without reading the whole class. Supports overload disambiguation.
---

## When to use
Before reading a Java source file to understand what a specific method does.
Use this instead of `Read` when you only need one method.

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar context \
  --src <path-to-file-or-directory> \
  --element <ClassName#methodName>
```

`ClassName` may be a simple name or a fully-qualified name. To disambiguate overloads append the parameter type list using simple names: `ClassName#methodName(Type1,Type2)`.

If the class name or the method is ambiguous, the tool fails with a message listing the candidates — re-run with a qualified class FQN or with an explicit parameter list.

**Examples:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar context \
  --src src/main/java/com/example/UserService.java \
  --element UserService#findById

# disambiguate an overload
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar context \
  --src src/main/java \
  --element com.example.UserService#findById(Long)
```

## Output
```json
{
  "method": "com.example.UserService#findById(Long)",
  "signature": "public Optional<User> findById(Long id)",
  "body": "{\n    return userRepository.findById(id);\n  }",
  "calls": ["UserRepository#findById(Long)"],
  "localTypes": ["Optional<User>"],
  "annotations": ["@Transactional"]
}
```

`body` is the raw source between the opening and closing brace of the method block — exactly what the file contains, not a pretty-printed reconstruction.

## How to interpret the output
1. Read `signature` and `body` to understand what the method does.
2. Use `calls` (signatures only, no bodies) to understand its dependencies — do NOT fetch the bodies of called methods unless specifically needed.
3. Check `annotations` for AOP concerns (@Transactional, @Cacheable, etc.).
4. `localTypes` shows what data flows through the method.

Do NOT read the full file — use only what this tool returns.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
