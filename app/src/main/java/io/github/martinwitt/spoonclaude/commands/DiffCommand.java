package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.diff.ApiDiffTool;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "diff",
        description =
                "Compute public/protected API diff between two source trees: added, removed and changed signatures with before/after")
public class DiffCommand implements Runnable {

    @Option(names = "--before", required = true, description = "Source directory of the previous version")
    private String before;

    @Option(names = "--after", required = true, description = "Source directory of the new version")
    private String after;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new ApiDiffTool(before, after).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
