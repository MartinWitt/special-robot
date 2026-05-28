package io.github.martinwitt.spoonclaude.summary;

import java.util.List;

public record ClassSummaryResult(
        String className,
        String javadoc,
        List<String> classAnnotations,
        String superClass,
        List<String> interfaces,
        List<MemberEntry> constructors,
        List<MemberEntry> methods,
        List<FieldEntry> fields,
        List<String> outboundDependencies) {

    public record MemberEntry(String signature, String javadoc, List<String> annotations) {}

    public record FieldEntry(String type, String name, List<String> annotations) {}
}
