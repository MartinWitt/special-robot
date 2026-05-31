package io.github.martinwitt.spoonclaude.annotation;

import java.util.List;

public record AnnotationUsageResult(String annotationName, List<AnnotationUsage> usages) {}
