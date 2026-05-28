package io.github.martinwitt.spoonclaude.signature;

import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

public final class SignatureBuilder {

    private static final List<ModifierKind> MODIFIER_ORDER = List.of(
            ModifierKind.PUBLIC,
            ModifierKind.PROTECTED,
            ModifierKind.PRIVATE,
            ModifierKind.ABSTRACT,
            ModifierKind.STATIC,
            ModifierKind.FINAL,
            ModifierKind.SYNCHRONIZED,
            ModifierKind.NATIVE,
            ModifierKind.STRICTFP);

    private SignatureBuilder() {}

    /** Full source-level signature: modifiers + type params + return type + params + throws. */
    public static String full(CtExecutable<?> exec) {
        StringBuilder sb = new StringBuilder();
        appendModifiers(sb, exec);
        appendTypeParameters(sb, exec);
        if (exec instanceof CtMethod<?> method) {
            sb.append(renderType(method.getType())).append(' ');
        }
        sb.append(displayName(exec));
        sb.append('(').append(renderParameters(exec)).append(')');
        appendThrows(sb, exec);
        return sb.toString();
    }

    private static String displayName(CtExecutable<?> exec) {
        if (exec instanceof CtConstructor<?> ctor) {
            var decl = ctor.getDeclaringType();
            return decl != null ? decl.getSimpleName() : ctor.getSimpleName();
        }
        return exec.getSimpleName();
    }

    /** Compact identifier used for overload disambiguation: {@code name(SimpleType,SimpleType)}. */
    public static String compact(CtExecutable<?> exec) {
        String params = exec.getParameters().stream()
                .map(p -> p.getType().getSimpleName())
                .collect(Collectors.joining(","));
        return exec.getSimpleName() + "(" + params + ")";
    }

    private static void appendModifiers(StringBuilder sb, CtExecutable<?> exec) {
        var modifiers = exec instanceof CtMethod<?> m
                ? m.getModifiers()
                : exec instanceof CtConstructor<?> c ? c.getModifiers() : java.util.Set.<ModifierKind>of();
        for (ModifierKind mod : MODIFIER_ORDER) {
            if (modifiers.contains(mod)) {
                sb.append(mod.toString()).append(' ');
            }
        }
    }

    private static void appendTypeParameters(StringBuilder sb, CtExecutable<?> exec) {
        List<CtTypeParameter> typeParams = exec instanceof CtMethod<?> m
                ? m.getFormalCtTypeParameters()
                : exec instanceof CtConstructor<?> c ? c.getFormalCtTypeParameters() : List.of();
        if (typeParams.isEmpty()) return;
        sb.append('<');
        sb.append(typeParams.stream().map(SignatureBuilder::renderTypeParameter).collect(Collectors.joining(", ")));
        sb.append("> ");
    }

    private static String renderTypeParameter(CtTypeParameter tp) {
        var bound = tp.getSuperclass();
        if (bound == null || "java.lang.Object".equals(bound.getQualifiedName())) {
            return tp.getSimpleName();
        }
        return tp.getSimpleName() + " extends " + renderType(bound);
    }

    private static String renderParameters(CtExecutable<?> exec) {
        return exec.getParameters().stream()
                .map(p -> renderType(p.getType()) + " " + p.getSimpleName())
                .collect(Collectors.joining(", "));
    }

    private static void appendThrows(StringBuilder sb, CtExecutable<?> exec) {
        var thrown = exec.getThrownTypes();
        if (thrown == null || thrown.isEmpty()) return;
        sb.append(" throws ");
        sb.append(thrown.stream().map(SignatureBuilder::renderType).collect(Collectors.joining(", ")));
    }

    /** Renders a type reference preserving generic arguments at every level. */
    public static String renderType(CtTypeReference<?> ref) {
        if (ref == null) return "void";
        String base = ref.getSimpleName();
        var args = ref.getActualTypeArguments();
        if (args == null || args.isEmpty()) return base;
        String rendered = args.stream().map(SignatureBuilder::renderType).collect(Collectors.joining(", "));
        return base + "<" + rendered + ">";
    }
}
