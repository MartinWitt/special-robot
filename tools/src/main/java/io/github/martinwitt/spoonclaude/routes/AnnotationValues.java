package io.github.martinwitt.spoonclaude.routes;

import java.util.ArrayList;
import java.util.List;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.declaration.CtAnnotation;

final class AnnotationValues {

    private AnnotationValues() {}

    /** Returns the first string value of an annotation attribute, or "" if missing/non-string. */
    static String firstString(CtAnnotation<?> annotation, String key) {
        List<String> all = strings(annotation, key);
        return all.isEmpty() ? "" : all.get(0);
    }

    /** Returns all string values of an annotation attribute, flattening single literals and {@code {…}} arrays. */
    static List<String> strings(CtAnnotation<?> annotation, String key) {
        CtExpression<?> expr = annotation.getValue(key);
        List<String> out = new ArrayList<>();
        if (expr == null) return out;
        collectStrings(expr, out);
        return out;
    }

    private static void collectStrings(CtExpression<?> expr, List<String> out) {
        if (expr instanceof CtLiteral<?> literal) {
            Object v = literal.getValue();
            if (v instanceof String s) out.add(s);
            return;
        }
        if (expr instanceof CtNewArray<?> array) {
            for (CtExpression<?> element : array.getElements()) collectStrings(element, out);
        }
    }

    /** Returns enum constant simple names, e.g. {@code POST} for {@code method = RequestMethod.POST}. */
    static List<String> enumNames(CtAnnotation<?> annotation, String key) {
        CtExpression<?> expr = annotation.getValue(key);
        List<String> out = new ArrayList<>();
        if (expr == null) return out;
        collectEnumNames(expr, out);
        return out;
    }

    private static void collectEnumNames(CtExpression<?> expr, List<String> out) {
        if (expr instanceof CtFieldRead<?> fieldRead) {
            String simple = fieldRead.getVariable().getSimpleName();
            if (simple != null && !simple.isEmpty()) out.add(simple);
            return;
        }
        if (expr instanceof CtNewArray<?> array) {
            for (CtExpression<?> element : array.getElements()) collectEnumNames(element, out);
        }
    }
}
