---
ticket: none
status: done
---

## Requirements
Add a `spoon-hierarchy` CLI command and companion skill that, given a type name and a source root, shows the full type hierarchy in both directions:
- **Supertypes**: transitive chain of superclasses and implemented interfaces up to (but not including) `Object`
- **Subtypes**: all project-local classes/interfaces that extend or implement the target type

## Acceptance Criteria
1. `--src <path> --type Foo` emits a nested JSON tree with both `supertypes` and `subtypes` sections
2. `--supertypes-only` emits only the supertypes section; `--subtypes-only` emits only the subtypes section
3. `--include-object` adds `java.lang.Object` as the root of the supertype chain
4. Every node in the tree includes a `kind` field: one of `class`, `interface`, `enum`, `record`
5. Classpath types (not in project source) appear as leaf nodes with `"source": "classpath"` and no children
6. Project-local nodes have `"source": "project"` and their children are recursively resolved
7. The subtypes section always includes a note `"subtypesScope": "project-local only"`
8. Ambiguous simple name → non-zero exit + JSON error listing all candidate FQNs (consistent with `AmbiguousNameException`)
9. Unknown type → non-zero exit + JSON error message
10. Final class or type with no hierarchy → exits zero, emits full JSON structure with empty `supertypes`/`subtypes` arrays

## Out of Scope
- Resolving subtypes from classpath (JDK, external jars)
- Method-level override tracking (that's `spoon-impact` / a future override-map tool)
- Cycle detection beyond what Spoon's model already handles

## Technical Notes
- CLI: `spoon-hierarchy --src <path> --type Foo [--supertypes-only] [--subtypes-only] [--include-object]`
- Type resolution via existing `TypeResolver` (FQN + simple-name lookup, throws `AmbiguousNameException` on ambiguity)
- Output: nested JSON tree — root node is the target type, with `supertypes` and `subtypes` arrays of nodes
- Node shape: `{ "fqn": "...", "simpleName": "...", "kind": "class|interface|enum|record", "source": "project|classpath", "supertypes": [...] }`
- Subtypes are found by scanning all `CtType` in the model and checking `getSuperclass()` / `getSuperInterfaces()`
- Supertype chain walks `getSuperclass()` and `getSuperInterfaces()` recursively; stops at `Object` unless `--include-object`
- Classpath supertypes: include as leaf nodes (no recursion), mark `"source": "classpath"`
- Follow existing `*Result` record pattern for the result type; use `JsonRenderer` for output
- New command class: `HierarchyCommand` in `commands/` package
