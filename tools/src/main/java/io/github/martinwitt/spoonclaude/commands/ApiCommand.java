package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.api.ApiSurfaceTool;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "api", description = "Dump public/protected API surface of a class: signatures + javadoc, no bodies")
public class ApiCommand implements Runnable {

    @Option(names = "--file", required = true, description = "Path to source file or directory")
    private String file;

    @Option(names = "--class", required = true, description = "Simple class name (e.g. UserService)")
    private String className;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new ApiSurfaceTool(file, className).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
