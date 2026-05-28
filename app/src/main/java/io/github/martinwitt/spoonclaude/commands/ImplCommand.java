package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.impl.ImplementationFinder;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "impl", description = "Find all concrete implementations of an interface or abstract class")
public class ImplCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to analyze")
    private String src;

    @Option(names = "--type", required = true, description = "Interface or abstract class name (simple or FQN)")
    private String type;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new ImplementationFinder(src, type).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
