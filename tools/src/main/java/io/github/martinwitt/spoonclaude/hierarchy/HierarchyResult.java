package io.github.martinwitt.spoonclaude.hierarchy;

import java.util.List;

public record HierarchyResult(
        String fqn,
        String simpleName,
        String kind,
        String source,
        List<HierarchyNode> supertypes,
        List<HierarchyNode> subtypes,
        String subtypesScope) {

    public record HierarchyNode(
            String fqn, String simpleName, String kind, String source, List<HierarchyNode> supertypes) {}
}
