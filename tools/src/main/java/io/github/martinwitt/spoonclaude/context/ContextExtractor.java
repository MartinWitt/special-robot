package io.github.martinwitt.spoonclaude.context;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.MethodResolver;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.visitor.filter.TypeFilter;

public final class ContextExtractor implements SpoonTool<ContextResult> {

    private final String sourcePath;
    private final String className;
    private final String methodSpec;

    public ContextExtractor(String sourcePath, String className, String methodSpec) {
        this.sourcePath = sourcePath;
        this.className = className;
        this.methodSpec = methodSpec;
    }

    @Override
    public ContextResult execute() {
        var model = SpoonLoader.load(sourcePath);
        var type = TypeResolver.resolve(model, className);
        var method = MethodResolver.resolve(type, methodSpec);

        var qualifiedName = type.getQualifiedName() + "#" + SignatureBuilder.compact(method);
        var signature = SignatureBuilder.full(method);
        var body = renderBody(method.getBody());

        var calls = method.getElements(new TypeFilter<>(CtInvocation.class)).stream()
                .filter(inv -> inv.getExecutable() != null)
                .map(inv -> {
                    var declaring = inv.getExecutable().getDeclaringType() != null
                            ? inv.getExecutable().getDeclaringType().getSimpleName() + "#"
                            : "";
                    return declaring + inv.getExecutable().getSignature();
                })
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        var localTypes = method.getElements(new TypeFilter<>(CtLocalVariable.class)).stream()
                .filter(v -> v.getType() != null)
                .map(v -> SignatureBuilder.renderType(v.getType()) + " " + v.getSimpleName())
                .distinct()
                .collect(Collectors.toList());

        var annotations = method.getAnnotations().stream()
                .map(a -> "@" + a.getAnnotationType().getSimpleName())
                .collect(Collectors.toList());

        return new ContextResult(qualifiedName, signature, body, calls, localTypes, annotations);
    }

    private static String renderBody(CtBlock<?> body) {
        if (body == null) return "";
        SourcePosition pos = body.getPosition();
        if (pos != null && pos.isValidPosition() && pos.getFile() != null) {
            try {
                String content = Files.readString(pos.getFile().toPath());
                int start = pos.getSourceStart();
                int end = pos.getSourceEnd();
                if (start >= 0 && end >= start && end < content.length()) {
                    return content.substring(start, end + 1);
                }
            } catch (IOException ignored) {
                // fall through to pretty-printer
            }
        }
        return body.toString();
    }
}
