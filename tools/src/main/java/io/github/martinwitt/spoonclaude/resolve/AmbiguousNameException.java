package io.github.martinwitt.spoonclaude.resolve;

import java.util.List;

public final class AmbiguousNameException extends IllegalArgumentException {

    private final List<String> candidates;

    public AmbiguousNameException(String message, List<String> candidates) {
        super(message);
        this.candidates = List.copyOf(candidates);
    }

    public List<String> candidates() {
        return candidates;
    }
}
