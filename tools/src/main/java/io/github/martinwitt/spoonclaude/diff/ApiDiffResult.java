package io.github.martinwitt.spoonclaude.diff;

import java.util.List;

public record ApiDiffResult(List<ChangeEntry> added, List<ChangeEntry> removed, List<SignatureChange> changed) {

    public record ChangeEntry(String classFqn, String signature) {}

    public record SignatureChange(String classFqn, String before, String after) {}
}
