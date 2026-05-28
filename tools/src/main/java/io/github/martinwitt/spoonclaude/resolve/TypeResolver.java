package io.github.martinwitt.spoonclaude.resolve;

import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

public final class TypeResolver {

    private TypeResolver() {}

    /**
     * Resolves a Java type by either its fully-qualified name (e.g. {@code com.example.User}) or
     * its simple name (e.g. {@code User}). On multiple simple-name hits throws an
     * {@link AmbiguousNameException} listing the FQN candidates so the caller can disambiguate.
     */
    public static CtType<?> resolve(CtModel model, String name) {
        boolean isFqn = name.indexOf('.') >= 0;
        List<CtType<?>> matches = model.getAllTypes().stream()
                .filter(t -> isFqn
                        ? t.getQualifiedName().equals(name)
                        : t.getSimpleName().equals(name))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Type not found: " + name);
        }
        if (matches.size() > 1) {
            List<String> candidates =
                    matches.stream().map(CtType::getQualifiedName).sorted().collect(Collectors.toList());
            throw new AmbiguousNameException(
                    "Ambiguous type '" + name + "', qualify with one of: " + String.join(", ", candidates), candidates);
        }
        return matches.get(0);
    }
}
