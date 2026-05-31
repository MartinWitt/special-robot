package io.github.martinwitt.spoonclaude.cyclomatic;

import java.util.List;

public record CyclomaticResult(String className, List<MethodComplexity> methods) {}
