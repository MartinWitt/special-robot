package io.github.martinwitt.spoonclaude.cyclomatic;

import io.github.martinwitt.spoonclaude.SpoonLoader;
import io.github.martinwitt.spoonclaude.SpoonTool;
import io.github.martinwitt.spoonclaude.resolve.TypeResolver;
import io.github.martinwitt.spoonclaude.signature.SignatureBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

public final class CyclomaticTool implements SpoonTool<CyclomaticResult> {

    private final String sourcePath;
    private final String className;

    public CyclomaticTool(String sourcePath, String className) {
        this.sourcePath = sourcePath;
        this.className = className;
    }

    @Override
    public CyclomaticResult execute() {
        var model = SpoonLoader.load(sourcePath);
        var type = TypeResolver.resolve(model, className);

        List<MethodComplexity> methods = type.getMethods().stream()
                .map(m -> new MethodComplexity(m.getSimpleName(), SignatureBuilder.compact(m), computeComplexity(m)))
                .sorted(Comparator.comparing(MethodComplexity::methodName).thenComparing(MethodComplexity::signature))
                .collect(Collectors.toList());

        return new CyclomaticResult(type.getQualifiedName(), methods);
    }

    private static int computeComplexity(CtMethod<?> method) {
        int c = 1;
        c += method.getElements(new TypeFilter<>(CtIf.class)).size();
        c += method.getElements(new TypeFilter<>(CtFor.class)).size();
        c += method.getElements(new TypeFilter<>(CtForEach.class)).size();
        c += method.getElements(new TypeFilter<>(CtWhile.class)).size();
        c += method.getElements(new TypeFilter<>(CtDo.class)).size();
        c += method.getElements(new TypeFilter<>(CtCase.class)).size();
        c += method.getElements(new TypeFilter<>(CtCatch.class)).size();
        c += method.getElements(new TypeFilter<>(CtConditional.class)).size();
        c += method.getElements(new TypeFilter<>(CtBinaryOperator.class)).stream()
                .filter(op -> op.getKind() == BinaryOperatorKind.AND || op.getKind() == BinaryOperatorKind.OR)
                .count();
        return c;
    }
}
