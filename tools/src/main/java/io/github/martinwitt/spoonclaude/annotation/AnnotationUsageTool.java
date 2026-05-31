package io.github.martinwitt.spoonclaude.annotation;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

public final class AnnotationUsageTool implements SpoonTool<AnnotationUsageResult> {

    private final String sourcePath;
    private final String annotationName;

    public AnnotationUsageTool(String sourcePath, String annotationName) {
        this.sourcePath = sourcePath;
        this.annotationName = annotationName;
    }

    @Override
    public AnnotationUsageResult execute() {
        var model = SpoonLoader.load(sourcePath);
        List<AnnotationUsage> usages = new ArrayList<>();

        model.getAllTypes().forEach(type -> {
            String typeFqn = type.getQualifiedName();

            if (hasAnnotation(type.getAnnotations())) {
                usages.add(new AnnotationUsage(typeFqn, "class", typeFqn));
            }

            type.getFields().forEach(field -> {
                if (hasAnnotation(field.getAnnotations())) {
                    usages.add(new AnnotationUsage(typeFqn + "#" + field.getSimpleName(), "field", typeFqn));
                }
            });

            type.getMethods().forEach(method -> {
                if (hasAnnotation(method.getAnnotations())) {
                    usages.add(new AnnotationUsage(
                            typeFqn + "#" + SignatureBuilder.compact(method), "method", typeFqn));
                }
                method.getParameters().forEach(param -> {
                    if (hasAnnotation(param.getAnnotations())) {
                        usages.add(new AnnotationUsage(
                                typeFqn + "#" + SignatureBuilder.compact(method) + "#" + param.getSimpleName(),
                                "parameter",
                                typeFqn));
                    }
                });
            });
        });

        usages.sort(Comparator.comparing(AnnotationUsage::elementFqn));
        return new AnnotationUsageResult(annotationName, usages);
    }

    private boolean hasAnnotation(List<CtAnnotation<?>> annotations) {
        return annotations.stream().anyMatch(this::matches);
    }

    private boolean matches(CtAnnotation<?> ann) {
        var annType = ann.getAnnotationType();
        if (annType == null) return false;
        if (annotationName.contains(".")) {
            return annType.getQualifiedName().equals(annotationName);
        }
        return annType.getSimpleName().equals(annotationName);
    }
}
