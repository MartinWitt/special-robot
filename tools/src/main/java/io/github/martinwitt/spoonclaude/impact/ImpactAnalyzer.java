package io.github.martinwitt.spoonclaude.impact;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.MethodResolver;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ImpactAnalyzer implements SpoonTool<ImpactResult> {

    private final String sourcePath;
    private final String className;
    private final String methodSpec;

    public ImpactAnalyzer(String sourcePath, String className, String methodSpec) {
        this.sourcePath = sourcePath;
        this.className = className;
        this.methodSpec = methodSpec;
    }

    @Override
    public ImpactResult execute() {
        var model = SpoonLoader.load(sourcePath);
        var targetType = TypeResolver.resolve(model, className);
        String targetFqn = targetType.getQualifiedName();

        String target;
        CallerSet callerSet;
        if (methodSpec != null) {
            var targetMethod = MethodResolver.resolve(targetType, methodSpec);
            target = targetFqn + "#" + SignatureBuilder.compact(targetMethod);
            callerSet = findCallersOfMethod(model, targetFqn, targetMethod);
        } else {
            target = targetFqn;
            callerSet = CallerSet.empty();
        }

        TypeDependents typeDependents = findTypeDependents(model, targetFqn);

        return new ImpactResult(
                target,
                callerSet.labels.size(),
                typeDependents.names.size(),
                List.copyOf(callerSet.labels),
                List.copyOf(typeDependents.names),
                Map.copyOf(callerSet.byPackage),
                Map.copyOf(typeDependents.byPackage));
    }

    private CallerSet findCallersOfMethod(CtModel model, String targetFqn, CtMethod<?> targetMethod) {
        String targetSignature = targetMethod.getSignature();
        var labels = new TreeSet<String>();
        var byPackage = new TreeMap<String, Integer>();
        for (CtInvocation<?> inv : model.getElements(new TypeFilter<>(CtInvocation.class))) {
            var exec = inv.getExecutable();
            if (exec == null) continue;
            if (!exec.getSignature().equals(targetSignature)) continue;
            CtTypeReference<?> decl = exec.getDeclaringType();
            if (!declaringTypeMatches(decl, targetFqn)) continue;

            CtMethod<?> parentMethod = inv.getParent(CtMethod.class);
            if (parentMethod == null) continue;
            CtType<?> parentType = parentMethod.getDeclaringType();
            if (parentType == null) continue;
            String label = parentType.getSimpleName() + "#" + SignatureBuilder.compact(parentMethod);
            if (labels.add(label)) {
                byPackage.merge(packageOf(parentType.getQualifiedName()), 1, Integer::sum);
            }
        }
        return new CallerSet(labels, byPackage);
    }

    private TypeDependents findTypeDependents(CtModel model, String targetFqn) {
        var names = new TreeSet<String>();
        var byPackage = new TreeMap<String, Integer>();
        String targetSimple = simpleNameOf(targetFqn);
        for (CtTypeReference<?> ref : model.getElements(new TypeFilter<>(CtTypeReference.class))) {
            if (!Objects.equals(ref.getQualifiedName(), targetFqn)
                    && !ref.getSimpleName().equals(targetSimple)) {
                continue;
            }
            CtType<?> owner = ref.getParent(CtType.class);
            if (owner == null) continue;
            if (Objects.equals(owner.getQualifiedName(), targetFqn)) continue;
            String label = owner.getQualifiedName();
            if (names.add(label)) {
                byPackage.merge(packageOf(label), 1, Integer::sum);
            }
        }
        return new TypeDependents(names, byPackage);
    }

    private static boolean declaringTypeMatches(CtTypeReference<?> ref, String targetFqn) {
        if (ref == null) return true;
        return ref.getQualifiedName().equals(targetFqn) || ref.getSimpleName().equals(simpleNameOf(targetFqn));
    }

    private static String packageOf(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? "<default>" : fqn.substring(0, dot);
    }

    private static String simpleNameOf(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    private record CallerSet(TreeSet<String> labels, Map<String, Integer> byPackage) {
        static CallerSet empty() {
            return new CallerSet(new TreeSet<>(), new LinkedHashMap<>());
        }
    }

    private record TypeDependents(TreeSet<String> names, Map<String, Integer> byPackage) {}
}
