package io.github.martinwitt.spoonclaude.callgraph;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.MethodResolver;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class CallGraphTool implements SpoonTool<CallGraphResult> {

    private final String sourcePath;
    private final String className;
    private final String methodSpec;
    private final int depth;

    public CallGraphTool(String sourcePath, String className, String methodSpec, int depth) {
        this.sourcePath = sourcePath;
        this.className = className;
        this.methodSpec = methodSpec;
        this.depth = depth;
    }

    @Override
    public CallGraphResult execute() {
        var model = SpoonLoader.load(sourcePath);
        var type = TypeResolver.resolve(model, className);
        var target = MethodResolver.resolve(type, methodSpec);
        var targetKey = type.getQualifiedName() + "#" + SignatureBuilder.compact(target);

        Set<String> callees = new TreeSet<>();
        Set<String> expanded = new HashSet<>();
        collectCalleesRecursive(model, target, depth, expanded, callees);

        Set<String> callers = collectCallers(model, target);

        return new CallGraphResult(targetKey, new ArrayList<>(callers), new ArrayList<>(callees), depth);
    }

    private void collectCalleesRecursive(
            CtModel model, CtMethod<?> method, int remainingDepth, Set<String> expanded, Set<String> output) {
        if (remainingDepth <= 0) return;
        String methodKey = methodKey(method);
        if (!expanded.add(methodKey)) return;

        for (CtInvocation<?> inv : method.getElements(new TypeFilter<>(CtInvocation.class))) {
            CtExecutableReference<?> exec = inv.getExecutable();
            if (exec == null) continue;
            CtTypeReference<?> decl = exec.getDeclaringType();
            String label = resolveTypeFqn(model, decl) + "#" + exec.getSignature();
            output.add(label);

            if (remainingDepth > 1 && decl != null) {
                findMethod(model, decl, exec)
                        .ifPresent(next -> collectCalleesRecursive(model, next, remainingDepth - 1, expanded, output));
            }
        }
    }

    private Set<String> collectCallers(CtModel model, CtMethod<?> target) {
        String targetSignature = target.getSignature();
        CtType<?> targetType = target.getDeclaringType();
        String targetFqn = targetType != null ? targetType.getQualifiedName() : null;

        Set<String> callers = new TreeSet<>();
        for (CtInvocation<?> inv : model.getElements(new TypeFilter<>(CtInvocation.class))) {
            CtExecutableReference<?> exec = inv.getExecutable();
            if (exec == null) continue;
            if (!exec.getSignature().equals(targetSignature)) continue;
            CtTypeReference<?> decl = exec.getDeclaringType();
            if (!declaringTypeMatches(decl, targetFqn)) continue;

            CtMethod<?> parent = inv.getParent(CtMethod.class);
            if (parent == null) continue;
            CtType<?> parentType = parent.getDeclaringType();
            String label =
                    (parentType != null ? parentType.getQualifiedName() + "#" : "") + SignatureBuilder.compact(parent);
            callers.add(label);
        }
        return callers;
    }

    private static boolean declaringTypeMatches(CtTypeReference<?> ref, String targetFqn) {
        if (ref == null || targetFqn == null) return true;
        return ref.getQualifiedName().equals(targetFqn) || ref.getSimpleName().equals(simpleNameOf(targetFqn));
    }

    private static String simpleNameOf(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    private static String methodKey(CtMethod<?> method) {
        CtType<?> decl = method.getDeclaringType();
        String typeKey = decl != null ? decl.getQualifiedName() : "?";
        return typeKey + "#" + method.getSignature();
    }

    /**
     * Returns the fully-qualified name for a type reference, falling back to a model
     * lookup by simple name when Spoon's no-classpath mode cannot resolve the package.
     */
    private static String resolveTypeFqn(CtModel model, CtTypeReference<?> ref) {
        if (ref == null) return "?";
        String fqn = ref.getQualifiedName();
        if (!fqn.contains(".")) {
            List<String> candidates = model.getAllTypes().stream()
                    .filter(t -> t.getSimpleName().equals(ref.getSimpleName()))
                    .map(CtType::getQualifiedName)
                    .toList();
            if (candidates.size() == 1) return candidates.get(0);
        }
        return fqn;
    }

    private static Optional<CtMethod<?>> findMethod(
            CtModel model, CtTypeReference<?> declaringRef, CtExecutableReference<?> exec) {
        String declFqn = declaringRef.getQualifiedName();
        String declSimple = declaringRef.getSimpleName();
        String execSignature = exec.getSignature();
        return model.getAllTypes().stream()
                .filter(t -> Objects.equals(t.getQualifiedName(), declFqn)
                        || t.getSimpleName().equals(declSimple))
                .flatMap(t -> t.getMethods().stream())
                .filter(m -> m.getSignature().equals(execSignature))
                .<CtMethod<?>>map(m -> m)
                .findFirst();
    }
}
