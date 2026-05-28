package io.github.martinwitt.spoonclaude.callgraph;

import java.util.List;

public record CallGraphResult(String target, List<String> callers, List<String> callees, int depth) {}
