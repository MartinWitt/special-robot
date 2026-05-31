package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.cyclomatic.CyclomaticTool;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "cyclomatic", description = "Compute cyclomatic complexity per method for a class")
public class CyclomaticCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to analyze")
    private String src;

    @Option(names = "--class", required = true, description = "Class name (simple or FQN)")
    private String className;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new CyclomaticTool(src, className).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
