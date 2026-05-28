package io.github.martinwitt.spoonclaude.resolve;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

public final class MethodResolver {

    private MethodResolver() {}

    /**
     * Resolves a method on the given type. The spec is either {@code "methodName"} or
     * {@code "methodName(Type1,Type2)"} where parameter types are simple names. Overloads must
     * be disambiguated explicitly via the parameter list; otherwise an
     * {@link AmbiguousNameException} is thrown.
     */
    public static CtMethod<?> resolve(CtType<?> type, String spec) {
        Spec parsed = parse(spec);
        List<CtMethod<?>> matches = type.getMethods().stream()
                .filter(m -> m.getSimpleName().equals(parsed.name()))
                .filter(m -> parsed.params() == null || paramsMatch(m, parsed.params()))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Method not found: " + type.getSimpleName() + "#" + spec);
        }
        if (matches.size() > 1) {
            List<String> candidates =
                    matches.stream().map(MethodResolver::renderSpec).sorted().collect(Collectors.toList());
            throw new AmbiguousNameException(
                    "Ambiguous method '" + type.getSimpleName() + "#" + parsed.name()
                            + "', specify overload, e.g. one of: " + String.join(", ", candidates),
                    candidates);
        }
        return matches.get(0);
    }

    public static Spec parse(String spec) {
        int paren = spec.indexOf('(');
        if (paren < 0) {
            return new Spec(spec, null);
        }
        if (!spec.endsWith(")")) {
            throw new IllegalArgumentException("Method spec missing closing ')': " + spec);
        }
        String name = spec.substring(0, paren);
        String paramList = spec.substring(paren + 1, spec.length() - 1).trim();
        List<String> params = paramList.isEmpty()
                ? List.of()
                : Arrays.stream(paramList.split(",")).map(String::trim).collect(Collectors.toList());
        return new Spec(name, params);
    }

    private static boolean paramsMatch(CtMethod<?> m, List<String> expected) {
        if (m.getParameters().size() != expected.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            String actual = m.getParameters().get(i).getType().getSimpleName();
            if (!actual.equals(expected.get(i))) return false;
        }
        return true;
    }

    private static String renderSpec(CtMethod<?> m) {
        String params =
                m.getParameters().stream().map(p -> p.getType().getSimpleName()).collect(Collectors.joining(","));
        return m.getSimpleName() + "(" + params + ")";
    }

    public record Spec(String name, List<String> params) {}
}
