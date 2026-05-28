package io.github.martinwitt.spoonclaude.summary;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.code.CtComment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ClassSummaryTool implements SpoonTool<ClassSummaryResult> {

    private final String sourcePath;
    private final String className;

    public ClassSummaryTool(String sourcePath, String className) {
        this.sourcePath = sourcePath;
        this.className = className;
    }

    @Override
    public ClassSummaryResult execute() {
        var model = SpoonLoader.load(sourcePath);
        CtType<?> type = TypeResolver.resolve(model, className);

        String javadoc = extractTypeJavadoc(type);
        List<String> classAnnotations = type.getAnnotations().stream()
                .map(a -> "@" + a.getAnnotationType().getSimpleName())
                .collect(Collectors.toList());
        String superClass = type.getSuperclass() != null ? type.getSuperclass().getQualifiedName() : null;
        List<String> interfaces = type.getSuperInterfaces().stream()
                .map(ref -> ref.getQualifiedName())
                .sorted()
                .collect(Collectors.toList());

        List<ClassSummaryResult.MemberEntry> constructors = type instanceof CtClass<?> klass
                ? klass.getConstructors().stream()
                        .filter(ClassSummaryTool::isExported)
                        .map(ClassSummaryTool::toMemberEntry)
                        .collect(Collectors.toList())
                : List.of();

        List<ClassSummaryResult.MemberEntry> methods = type.getMethods().stream()
                .filter(ClassSummaryTool::isExported)
                .map(ClassSummaryTool::toMemberEntry)
                .collect(Collectors.toList());

        List<ClassSummaryResult.FieldEntry> fields = type.getFields().stream()
                .filter(ClassSummaryTool::isExported)
                .map(ClassSummaryTool::toFieldEntry)
                .collect(Collectors.toList());

        List<String> outboundDependencies = collectOutboundDependencies(type);

        return new ClassSummaryResult(
                type.getQualifiedName(),
                javadoc,
                classAnnotations,
                superClass,
                interfaces,
                constructors,
                methods,
                fields,
                outboundDependencies);
    }

    private static List<String> collectOutboundDependencies(CtType<?> type) {
        String selfFqn = type.getQualifiedName();
        return type.getElements(new TypeFilter<>(CtTypeReference.class)).stream()
                .filter(ref -> !(ref instanceof CtTypeParameterReference))
                .filter(ref -> !ref.isPrimitive())
                .map(CtTypeReference::getQualifiedName)
                .filter(fqn -> fqn != null && !fqn.isEmpty())
                .filter(fqn -> !fqn.equals(selfFqn))
                .filter(fqn -> !fqn.equals("void"))
                .filter(fqn -> !fqn.startsWith("java.lang."))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static boolean isExported(CtModifiable element) {
        ModifierKind v = element.getVisibility();
        return v == ModifierKind.PUBLIC || v == ModifierKind.PROTECTED;
    }

    private static ClassSummaryResult.MemberEntry toMemberEntry(CtMethod<?> method) {
        return new ClassSummaryResult.MemberEntry(
                SignatureBuilder.full(method), trimmedDoc(method.getDocComment()), renderAnnotations(method));
    }

    private static ClassSummaryResult.MemberEntry toMemberEntry(CtConstructor<?> ctor) {
        return new ClassSummaryResult.MemberEntry(
                SignatureBuilder.full(ctor), trimmedDoc(ctor.getDocComment()), renderAnnotations(ctor));
    }

    private static ClassSummaryResult.FieldEntry toFieldEntry(CtField<?> field) {
        return new ClassSummaryResult.FieldEntry(
                SignatureBuilder.renderType(field.getType()), field.getSimpleName(), renderAnnotations(field));
    }

    private static String trimmedDoc(String doc) {
        return doc != null ? doc.trim() : "";
    }

    private static List<String> renderAnnotations(CtElement element) {
        return element.getAnnotations().stream()
                .map(ClassSummaryTool::renderAnnotation)
                .collect(Collectors.toList());
    }

    private static String renderAnnotation(CtAnnotation<?> annotation) {
        var type = annotation.getAnnotationType();
        return "@" + (type != null ? type.getSimpleName() : "Unknown");
    }

    private static String extractTypeJavadoc(CtType<?> type) {
        String spoonDoc = type.getComments().stream()
                .filter(c -> c.getCommentType() == CtComment.CommentType.JAVADOC)
                .map(CtComment::getContent)
                .findFirst()
                .orElse("");
        if (!spoonDoc.isBlank()) return spoonDoc.trim();
        return readJavadocBeforeDeclaration(type);
    }

    private static String readJavadocBeforeDeclaration(CtType<?> type) {
        SourcePosition pos = type.getPosition();
        if (pos == null || !pos.isValidPosition() || pos.getFile() == null) return "";
        try {
            String content = Files.readString(pos.getFile().toPath());
            String prefix = content.substring(0, Math.max(0, pos.getSourceStart()));
            int docEnd = prefix.lastIndexOf("*/");
            if (docEnd < 0) return "";
            int docStart = prefix.lastIndexOf("/**", docEnd);
            if (docStart < 0) return "";
            String raw = prefix.substring(docStart + 3, docEnd);
            return cleanJavadocLines(raw);
        } catch (IOException e) {
            return "";
        }
    }

    private static String cleanJavadocLines(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\\R")) {
            String trimmed = line.replaceFirst("^\\s*\\*\\s?", "").trim();
            if (!trimmed.isEmpty()) sb.append(trimmed).append(' ');
        }
        return sb.toString().trim();
    }
}
