package io.github.martinwitt.spoonclaude.diff;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

public final class ApiDiffTool implements SpoonTool<ApiDiffResult> {

    private final String beforePath;
    private final String afterPath;

    public ApiDiffTool(String beforePath, String afterPath) {
        this.beforePath = beforePath;
        this.afterPath = afterPath;
    }

    @Override
    public ApiDiffResult execute() {
        Map<String, Map<String, String>> beforeSurface = surface(SpoonLoader.load(beforePath));
        Map<String, Map<String, String>> afterSurface = surface(SpoonLoader.load(afterPath));

        List<ApiDiffResult.ChangeEntry> added = new ArrayList<>();
        List<ApiDiffResult.ChangeEntry> removed = new ArrayList<>();
        List<ApiDiffResult.SignatureChange> changed = new ArrayList<>();

        for (String classFqn : sortedUnion(beforeSurface.keySet(), afterSurface.keySet())) {
            Map<String, String> before = beforeSurface.getOrDefault(classFqn, Map.of());
            Map<String, String> after = afterSurface.getOrDefault(classFqn, Map.of());
            for (String key : sortedUnion(before.keySet(), after.keySet())) {
                String beforeSig = before.get(key);
                String afterSig = after.get(key);
                if (beforeSig == null) {
                    added.add(new ApiDiffResult.ChangeEntry(classFqn, afterSig));
                } else if (afterSig == null) {
                    removed.add(new ApiDiffResult.ChangeEntry(classFqn, beforeSig));
                } else if (!beforeSig.equals(afterSig)) {
                    changed.add(new ApiDiffResult.SignatureChange(classFqn, beforeSig, afterSig));
                }
            }
        }

        return new ApiDiffResult(added, removed, changed);
    }

    /** Returns class FQN -> (compactKey -> fullSignature) for every public/protected executable. */
    private static Map<String, Map<String, String>> surface(CtModel model) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (CtType<?> type : model.getAllTypes()) {
            Map<String, String> members = new LinkedHashMap<>();
            executables(type)
                    .filter(ApiDiffTool::isExportedExecutable)
                    .forEach(exec -> members.put(SignatureBuilder.compact(exec), SignatureBuilder.full(exec)));
            if (!members.isEmpty()) {
                result.put(type.getQualifiedName(), members);
            }
        }
        return result;
    }

    private static Stream<? extends CtExecutable<?>> executables(CtType<?> type) {
        Stream<? extends CtExecutable<?>> methods = type.getMethods().stream();
        if (type instanceof CtClass<?> klass) {
            return Stream.concat(methods, klass.getConstructors().stream());
        }
        return methods;
    }

    private static boolean isExportedExecutable(CtExecutable<?> exec) {
        return exec instanceof CtModifiable mod && isExported(mod);
    }

    private static boolean isExported(CtModifiable element) {
        ModifierKind v = element.getVisibility();
        return v == ModifierKind.PUBLIC || v == ModifierKind.PROTECTED;
    }

    private static List<String> sortedUnion(java.util.Set<String> a, java.util.Set<String> b) {
        return Stream.concat(a.stream(), b.stream())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
