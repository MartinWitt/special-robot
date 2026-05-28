---
name: spoon-testmap
description: Find which tests cover a Java class and which public methods have no test coverage — statically, without running tests. Filters by declaring type, so `future.cancel()` in some unrelated test no longer counts as coverage of `YourService#cancel`.
---

## When to use
- Before writing tests to avoid duplicating existing coverage
- When asked to improve test coverage for a class
- After adding a new public method — check immediately if it's covered
- During code review to verify test completeness

## How to run

```bash
java -jar /PATH/TO/spoon-claude.jar testmap \
  --src <production-source-directory> \
  --test-src <test-source-directory> \
  --class <ClassName | com.example.ClassName>
```

`--class` accepts either a simple name or a fully-qualified name. On ambiguous simple names re-run with one of the FQN candidates the tool prints.

**Example:**
```bash
java -jar ~/tools/spoon-claude.jar testmap \
  --src src/main/java \
  --test-src src/test/java \
  --class com.example.OrderService
```

## Output
```json
{
  "target": "com.example.OrderService",
  "coveringTests": ["OrderIntegrationTest", "OrderServiceTest"],
  "uncoveredMethods": ["cancelOrder", "exportHistory"]
}
```

## How to interpret
- `coveringTests`: test classes that reference `OrderService` or call one of its public methods. Test-classness requires either a `*Test` / `*Spec` name or at least one method annotated with a recognized `@Test` (JUnit Jupiter, JUnit 4, TestNG).
- `uncoveredMethods`: public methods of the target with no test invocation found that resolves to the target type. An invocation only counts as coverage if its declaring type can be statically matched to the target — generic `cancel()` calls on other types are ignored.
- An empty `uncoveredMethods` list means every public method appears in at least one test under the target's declaring type.

**Note:** this is static analysis — it detects call-presence, not assertion-quality. A method appearing in a test file does not guarantee it is thoroughly exercised. With Spoon's no-classpath mode, very heavily polymorphic code may still produce false negatives where the declaring type cannot be resolved at all.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
