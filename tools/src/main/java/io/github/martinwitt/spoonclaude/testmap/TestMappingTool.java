package io.github.martinwitt.spoonclaude.testmap;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class TestMappingTool implements SpoonTool<TestMappingResult> {

    private static final Set<String> TEST_ANNOTATION_FQNS = Set.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.Test",
            "org.testng.annotations.Test");

    private final String sourcePath;
    private final String testSourcePath;
    private final String className;

    public TestMappingTool(String sourcePath, String testSourcePath, String className) {
        this.sourcePath = sourcePath;
        this.testSourcePath = testSourcePath;
        this.className = className;
    }

    @Override
    public TestMappingResult execute() {
        CtModel prodModel = SpoonLoader.load(sourcePath);
        CtModel testModel = SpoonLoader.load(testSourcePath);

        CtType<?> targetType = TypeResolver.resolve(prodModel, className);
        String targetFqn = targetType.getQualifiedName();

        List<String> publicMethods = targetType.getMethods().stream()
                .filter(m -> m.getVisibility() == ModifierKind.PUBLIC)
                .map(CtMethod::getSimpleName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Set<String> coveringTests = testModel.getAllTypes().stream()
                .filter(this::isTestClass)
                .filter(t -> referencesTarget(t, targetFqn) || callsMethodOfTarget(t, targetFqn, publicMethods))
                .map(CtType::getSimpleName)
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> coveredMethods = testModel.getAllTypes().stream()
                .filter(this::isTestClass)
                .flatMap(t -> t.getElements(new TypeFilter<>(CtInvocation.class)).stream())
                .filter(inv -> inv.getExecutable() != null)
                .filter(inv -> declaringTypeMatches(inv.getExecutable().getDeclaringType(), targetFqn))
                .map(inv -> inv.getExecutable().getSimpleName())
                .collect(Collectors.toSet());

        List<String> uncoveredMethods =
                publicMethods.stream().filter(m -> !coveredMethods.contains(m)).collect(Collectors.toList());

        return new TestMappingResult(targetFqn, List.copyOf(coveringTests), uncoveredMethods);
    }

    private boolean isTestClass(CtType<?> type) {
        if (type.getSimpleName().endsWith("Test") || type.getSimpleName().endsWith("Spec")) return true;
        return type.getMethods().stream()
                .anyMatch(m -> m.getAnnotations().stream().anyMatch(a -> {
                    var annType = a.getAnnotationType();
                    return annType != null && TEST_ANNOTATION_FQNS.contains(annType.getQualifiedName());
                }));
    }

    private boolean referencesTarget(CtType<?> testType, String targetFqn) {
        String targetSimple = simpleNameOf(targetFqn);
        return testType.getElements(new TypeFilter<>(CtTypeReference.class)).stream()
                .anyMatch(ref -> Objects.equals(ref.getQualifiedName(), targetFqn)
                        || ref.getSimpleName().equals(targetSimple));
    }

    private boolean callsMethodOfTarget(CtType<?> testType, String targetFqn, List<String> methodNames) {
        return testType.getElements(new TypeFilter<>(CtInvocation.class)).stream()
                .anyMatch(inv -> inv.getExecutable() != null
                        && methodNames.contains(inv.getExecutable().getSimpleName())
                        && declaringTypeMatches(inv.getExecutable().getDeclaringType(), targetFqn));
    }

    private static boolean declaringTypeMatches(CtTypeReference<?> ref, String targetFqn) {
        if (ref == null) return false;
        String refFqn = ref.getQualifiedName();
        // When Spoon resolved the FQN (qualified name differs from simple name), use strict
        // FQN comparison only — the simple-name fallback would produce false positives for
        // classes that share a simple name but live in different packages.
        if (!refFqn.equals(ref.getSimpleName())) {
            return refFqn.equals(targetFqn);
        }
        return ref.getSimpleName().equals(simpleNameOf(targetFqn));
    }

    private static String simpleNameOf(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }
}
