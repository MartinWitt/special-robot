package io.github.martinwitt.spoonclaude.api;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

public final class ApiSurfaceTool implements SpoonTool<ApiSurfaceResult> {

    private final String sourcePath;
    private final String className;

    public ApiSurfaceTool(String sourcePath, String className) {
        this.sourcePath = sourcePath;
        this.className = className;
    }

    @Override
    public ApiSurfaceResult execute() {
        var model = SpoonLoader.load(sourcePath);
        var type = TypeResolver.resolve(model, className);

        var classJavadoc = type.getDocComment() != null ? type.getDocComment().trim() : "";

        var constructors = collectConstructors(type);
        var methods = type.getMethods().stream()
                .filter(ApiSurfaceTool::isExported)
                .map(this::toMethodEntry)
                .collect(Collectors.toList());
        var fields = type.getFields().stream()
                .filter(ApiSurfaceTool::isExported)
                .map(this::toFieldEntry)
                .collect(Collectors.toList());

        return new ApiSurfaceResult(type.getQualifiedName(), classJavadoc, constructors, methods, fields);
    }

    private List<ApiSurfaceResult.ConstructorEntry> collectConstructors(CtType<?> type) {
        if (!(type instanceof spoon.reflect.declaration.CtClass<?> klass)) {
            return List.of();
        }
        return klass.getConstructors().stream()
                .filter(ApiSurfaceTool::isExported)
                .map(this::toConstructorEntry)
                .collect(Collectors.toList());
    }

    private static boolean isExported(spoon.reflect.declaration.CtModifiable element) {
        var visibility = element.getVisibility();
        return visibility == ModifierKind.PUBLIC || visibility == ModifierKind.PROTECTED;
    }

    private ApiSurfaceResult.MethodEntry toMethodEntry(CtMethod<?> method) {
        return new ApiSurfaceResult.MethodEntry(
                SignatureBuilder.full(method), trimmedDoc(method.getDocComment()), renderAnnotations(method));
    }

    private ApiSurfaceResult.ConstructorEntry toConstructorEntry(CtConstructor<?> ctor) {
        return new ApiSurfaceResult.ConstructorEntry(
                SignatureBuilder.full(ctor), trimmedDoc(ctor.getDocComment()), renderAnnotations(ctor));
    }

    private ApiSurfaceResult.FieldEntry toFieldEntry(CtField<?> field) {
        return new ApiSurfaceResult.FieldEntry(
                SignatureBuilder.renderType(field.getType()), field.getSimpleName(), renderAnnotations(field));
    }

    private static String trimmedDoc(String doc) {
        return doc != null ? doc.trim() : "";
    }

    private static List<String> renderAnnotations(spoon.reflect.declaration.CtElement element) {
        return element.getAnnotations().stream()
                .map(ApiSurfaceTool::renderAnnotation)
                .collect(Collectors.toList());
    }

    private static String renderAnnotation(CtAnnotation<?> annotation) {
        var type = annotation.getAnnotationType();
        return "@" + (type != null ? type.getSimpleName() : "Unknown");
    }
}
