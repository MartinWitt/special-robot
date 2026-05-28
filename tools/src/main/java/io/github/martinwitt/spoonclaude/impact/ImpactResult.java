package io.github.martinwitt.spoonclaude.impact;

import java.util.List;
import java.util.Map;

public record ImpactResult(
        String target,
        int callerCount,
        int dependentCount,
        List<String> directCallers,
        List<String> typeDependents,
        Map<String, Integer> callersByPackage,
        Map<String, Integer> dependentsByPackage) {}
