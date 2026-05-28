package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import io.github.martinwitt.spoonclaude.summary.ClassSummaryTool;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "summary",
        description =
                "One-shot orientation for a class: javadoc, extends/implements, constructors, public API, annotations, outbound dependencies")
public class SummaryCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to analyze")
    private String src;

    @Option(names = "--class", required = true, description = "Class name (simple or FQN)")
    private String className;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new ClassSummaryTool(src, className).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
