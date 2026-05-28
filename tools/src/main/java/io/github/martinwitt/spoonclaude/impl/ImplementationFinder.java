package io.github.martinwitt.spoonclaude.impl;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

public final class ImplementationFinder implements SpoonTool<ImplementationResult> {

    private final String sourcePath;
    private final String typeName;

    public ImplementationFinder(String sourcePath, String typeName) {
        this.sourcePath = sourcePath;
        this.typeName = typeName;
    }

    @Override
    public ImplementationResult execute() {
        var model = SpoonLoader.load(sourcePath);
        var target = TypeResolver.resolve(model, typeName);
        String targetFqn = target.getQualifiedName();

        Set<String> abstractTargetSigs = target.getMethods().stream()
                .filter(m -> target.isInterface() || m.isAbstract())
                .map(SignatureBuilder::compact)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ImplementationResult.ImplementationEntry> implementations = model.getAllTypes().stream()
                .filter(t -> !t.getQualifiedName().equals(targetFqn))
                .filter(t -> isAssignableTo(model, t, targetFqn))
                .map(t -> new ImplementationResult.ImplementationEntry(
                        t.getQualifiedName(), overrides(t, abstractTargetSigs)))
                .collect(Collectors.toList());

        return new ImplementationResult(targetFqn, implementations);
    }

    private static List<String> overrides(CtType<?> impl, Set<String> abstractTargetSigs) {
        return impl.getMethods().stream()
                .map(SignatureBuilder::compact)
                .filter(abstractTargetSigs::contains)
                .collect(Collectors.toList());
    }

    private static boolean isAssignableTo(CtModel model, CtType<?> type, String targetFqn) {
        Set<String> visited = new HashSet<>();
        return walk(model, type, targetFqn, visited);
    }

    private static boolean walk(CtModel model, CtType<?> type, String targetFqn, Set<String> visited) {
        if (!visited.add(type.getQualifiedName())) return false;
        return parentsOf(type).anyMatch(parent -> matchesOrInheritsFrom(model, parent, targetFqn, visited));
    }

    private static boolean matchesOrInheritsFrom(
            CtModel model, CtTypeReference<?> parent, String targetFqn, Set<String> visited) {
        if (parent.getQualifiedName().equals(targetFqn)) return true;
        CtType<?> resolved = findType(model, parent);
        return resolved != null && walk(model, resolved, targetFqn, visited);
    }

    private static Stream<CtTypeReference<?>> parentsOf(CtType<?> type) {
        Stream<CtTypeReference<?>> interfaces = type.getSuperInterfaces().stream();
        CtTypeReference<?> superClass = type.getSuperclass();
        return superClass == null ? interfaces : Stream.concat(interfaces, Stream.of(superClass));
    }

    private static CtType<?> findType(CtModel model, CtTypeReference<?> ref) {
        String fqn = ref.getQualifiedName();
        return model.getAllTypes().stream()
                .filter(t -> t.getQualifiedName().equals(fqn))
                .findFirst()
                .orElse(null);
    }
}
