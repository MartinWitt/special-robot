package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.hierarchy.HierarchyTool;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "hierarchy", description = "Show the type hierarchy (supertypes and subtypes) for a given type")
public class HierarchyCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to analyze")
    private String src;

    @Option(names = "--type", required = true, description = "Type name (simple or FQN)")
    private String type;

    @Option(names = "--supertypes-only", description = "Show only supertypes, omit subtypes")
    private boolean supertypesOnly;

    @Option(names = "--subtypes-only", description = "Show only subtypes, omit supertypes")
    private boolean subtypesOnly;

    @Option(names = "--include-object", description = "Include java.lang.Object in the supertype chain")
    private boolean includeObject;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(
                    new HierarchyTool(src, type, supertypesOnly, subtypesOnly, includeObject).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
