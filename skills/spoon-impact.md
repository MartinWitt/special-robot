---
name: spoon-impact
description: Answer "what breaks if I change X?" — finds all callers of a method or all dependents of a type, with package-level breakdown. Use before any refactoring or API change.
---

## When to use
- Before changing a method signature
- Before deleting or renaming a class
- Before making a field private that was previously public
- When assessing the blast radius of a change

## How to run

```bash
java -jar /PATH/TO/spoon-claude.jar impact \
  --src <source-directory> \
  --element <ClassName | ClassName#methodName[(Type1,Type2)]>
```

`ClassName` may be a simple name or a fully-qualified name. For overloaded methods append the parameter type list (simple names) to scope to one overload: `OrderService#charge(BigDecimal)`.

**Examples:**
```bash
# Impact of changing one overload of a method
java -jar ~/tools/spoon-claude.jar impact \
  --src src/main/java \
  --element com.example.UserRepository#findById(Long)

# Impact of changing a type
java -jar ~/tools/spoon-claude.jar impact \
  --src src/main/java \
  --element com.example.UserDto
```

## Output
```json
{
  "target": "com.example.UserRepository#findById(Long)",
  "callerCount": 2,
  "dependentCount": 4,
  "directCallers": ["UserService#loadUser(Long)", "AdminService#fetchUser(Long)"],
  "typeDependents": ["com.example.AdminService", "com.example.UserController", "com.example.UserService", "com.example.UserServiceTest"],
  "callersByPackage": {"com.example": 2},
  "dependentsByPackage": {"com.example": 4}
}
```

## How to interpret
- `directCallers`: methods with a direct invocation of the target overload — must be updated if signature changes. The parameter list is included so you can tell overloaded callees apart.
- `typeDependents`: types that reference the target class as a name — may be affected by type-level changes.
- `callersByPackage` / `dependentsByPackage`: distribution across packages — use to judge how cross-cutting the change is (single package = local refactor, many packages = coordinate broadly).
- Counts are precomputed so the caller doesn't have to do `length()`.

**Note:** Spoon runs without classpath, so type resolution is best-effort. The tool filters by declaring-type FQN where resolvable and falls back to simple-name match where Spoon could not resolve a reference. Use the package breakdown as a sanity check.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
