package io.github.martinwitt.spoonclaude.api;

import java.util.List;

public record ApiSurfaceResult(
        String className,
        String classJavadoc,
        List<ConstructorEntry> constructors,
        List<MethodEntry> methods,
        List<FieldEntry> fields) {

    public record ConstructorEntry(String signature, String javadoc, List<String> annotations) {}

    public record MethodEntry(String signature, String javadoc, List<String> annotations) {}

    public record FieldEntry(String type, String name, List<String> annotations) {}
}
