package io.github.martinwitt.spoonclaude.context;

import java.util.List;

public record ContextResult(
        String method,
        String signature,
        String body,
        List<String> calls,
        List<String> localTypes,
        List<String> annotations) {}
