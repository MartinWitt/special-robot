package io.github.martinwitt.spoonclaude.hierarchy;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

public final class HierarchyTool implements SpoonTool<HierarchyResult> {

    private static final String OBJECT_FQN = "java.lang.Object";
    private static final int MAX_DEPTH = 20;
    private static final String SUBTYPES_SCOPE = "project-local only";

    private final String sourcePath;
    private final String typeName;
    private final boolean supertypesOnly;
    private final boolean subtypesOnly;
    private final boolean includeObject;

    public HierarchyTool(
            String sourcePath, String typeName, boolean supertypesOnly, boolean subtypesOnly, boolean includeObject) {
        this.sourcePath = sourcePath;
        this.typeName = typeName;
        this.supertypesOnly = supertypesOnly;
        this.subtypesOnly = subtypesOnly;
        this.includeObject = includeObject;
    }

    @Override
    public HierarchyResult execute() {
        CtModel model = SpoonLoader.load(sourcePath);
        CtType<?> target = TypeResolver.resolve(model, typeName);

        List<HierarchyResult.HierarchyNode> supertypes =
                subtypesOnly ? List.of() : buildSupertypeChain(model, target, 0);
        List<HierarchyResult.HierarchyNode> subtypes = supertypesOnly ? List.of() : findDirectSubtypes(model, target);

        return new HierarchyResult(
                target.getQualifiedName(),
                target.getSimpleName(),
                kindOf(target),
                "project",
                supertypes,
                subtypes,
                SUBTYPES_SCOPE);
    }

    private List<HierarchyResult.HierarchyNode> buildSupertypeChain(CtModel model, CtType<?> type, int depth) {
        if (depth >= MAX_DEPTH) return List.of();

        List<HierarchyResult.HierarchyNode> result = new ArrayList<>();

        CtTypeReference<?> superclassRef = type.getSuperclass();
        if (superclassRef != null) {
            String fqn = superclassRef.getQualifiedName();
            if (!fqn.equals(OBJECT_FQN)) {
                result.add(buildNode(model, superclassRef, depth + 1));
            } else if (includeObject) {
                result.add(new HierarchyResult.HierarchyNode(OBJECT_FQN, "Object", "class", "classpath", List.of()));
            }
        } else if (!(type instanceof CtInterface) && includeObject) {
            result.add(new HierarchyResult.HierarchyNode(OBJECT_FQN, "Object", "class", "classpath", List.of()));
        }

        for (CtTypeReference<?> iface : type.getSuperInterfaces()) {
            result.add(buildNode(model, iface, depth + 1));
        }

        return result;
    }

    private HierarchyResult.HierarchyNode buildNode(CtModel model, CtTypeReference<?> ref, int depth) {
        String fqn = ref.getQualifiedName();
        String simpleName = ref.getSimpleName();

        CtType<?> resolved = model.getAllTypes().stream()
                .filter(t -> t.getQualifiedName().equals(fqn))
                .findFirst()
                .orElse(null);

        if (resolved != null) {
            List<HierarchyResult.HierarchyNode> children = buildSupertypeChain(model, resolved, depth);
            return new HierarchyResult.HierarchyNode(fqn, simpleName, kindOf(resolved), "project", children);
        } else {
            return new HierarchyResult.HierarchyNode(fqn, simpleName, kindOfRef(ref), "classpath", List.of());
        }
    }

    private List<HierarchyResult.HierarchyNode> findDirectSubtypes(CtModel model, CtType<?> target) {
        String targetFqn = target.getQualifiedName();
        return model.getAllTypes().stream()
                .filter(t -> !t.getQualifiedName().equals(targetFqn))
                .filter(t -> isDirectSubtype(t, targetFqn))
                .map(t -> new HierarchyResult.HierarchyNode(
                        t.getQualifiedName(), t.getSimpleName(), kindOf(t), "project", List.of()))
                .collect(Collectors.toList());
    }

    private static boolean isDirectSubtype(CtType<?> type, String targetFqn) {
        CtTypeReference<?> superclass = type.getSuperclass();
        if (superclass != null && superclass.getQualifiedName().equals(targetFqn)) return true;
        return type.getSuperInterfaces().stream()
                .anyMatch(i -> i.getQualifiedName().equals(targetFqn));
    }

    private static String kindOf(CtType<?> type) {
        if (type instanceof CtRecord) return "record";
        if (type instanceof CtEnum) return "enum";
        if (type instanceof CtInterface) return "interface";
        return "class";
    }

    private static String kindOfRef(CtTypeReference<?> ref) {
        try {
            if (ref.isInterface()) return "interface";
        } catch (Exception ignored) {
        }
        return "class";
    }
}
