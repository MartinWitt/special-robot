---
name: spoon-spring-routes
description: List every HTTP endpoint in a Java project — Spring MVC (@RestController/@RequestMapping/@GetMapping/...), JAX-RS (@Path/@GET/@POST/...), and Spring WebFlux Functional (RouterFunctions.route(GET("/x"), ...)). Use to answer "which endpoints does this service expose?" without reading every controller.
---

## When to use
- Mapping the HTTP surface of an unfamiliar service
- PR review: did this change add, move, or remove a route?
- Migration audits (e.g. Spring MVC → WebFlux): list everything that needs to move
- Picking which controller to read first when chasing a bug

## How to run

```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar routes \
  --src <source-directory>
```

**Example:**
```bash
java -jar ~/.claude/skills/spoon-claude/scripts/spoon-claude.jar routes \
  --src src/main/java
```

## Output
```json
{
  "routes": [
    {
      "framework": "spring-mvc",
      "httpMethod": "GET",
      "path": "/api/users/{id}",
      "handlerClass": "com.example.UserController",
      "handlerMethod": "getById"
    },
    {
      "framework": "jax-rs",
      "httpMethod": "POST",
      "path": "/items",
      "handlerClass": "com.example.ItemResource",
      "handlerMethod": "create"
    },
    {
      "framework": "spring-webflux-fn",
      "httpMethod": "GET",
      "path": "/health",
      "handlerClass": "com.example.RouterConfig",
      "handlerMethod": "routes"
    }
  ]
}
```

## How to interpret
- `framework` distinguishes Spring MVC, JAX-RS, and Spring WebFlux Functional. Treat each kind on its own terms.
- `path` is the fully-joined route (class-level prefix + method-level suffix), normalized (no `//`, no trailing slash).
- `handlerClass` is the controller / resource / `@Configuration` FQN. For WebFlux Functional, `handlerMethod` is the `@Bean` method that builds the `RouterFunction`, not the request handler itself — the handler is a lambda or method reference inside that bean.
- `httpMethod` is `ANY` for `@RequestMapping` without an explicit `method = …` (Spring treats it as all verbs).
- An empty `routes` list means: no Spring MVC, no JAX-RS, no WebFlux-Functional endpoints were detected. It does not rule out custom routing layers.

Do NOT grep controllers to find endpoints afterwards — this tool already covers all three framework styles in one pass.
Errors are printed to stderr as `{"error": "..."}` and the process exits with code 1.
