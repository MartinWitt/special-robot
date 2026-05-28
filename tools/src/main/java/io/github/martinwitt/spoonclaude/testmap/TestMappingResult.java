package io.github.martinwitt.spoonclaude.testmap;

import java.util.List;

public record TestMappingResult(String target, List<String> coveringTests, List<String> uncoveredMethods) {}
