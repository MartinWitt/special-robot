package io.github.martinwitt.spoonclaude.impl;

import java.util.List;

public record ImplementationResult(String target, List<ImplementationEntry> implementations) {

    public record ImplementationEntry(String classFqn, List<String> overriddenMethods) {}
}
