package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.context.ContextExtractor;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "context", description = "Extract focused method context: signature, body, calls, annotations")
public class ContextCommand implements Runnable {

    @Option(names = "--file", required = true, description = "Path to source file or directory")
    private String file;

    @Option(names = "--element", required = true, description = "ClassName#methodName (e.g. UserService#findById)")
    private String element;

    @Override
    public void run() {
        try {
            var parts = splitElement(element);
            System.out.println(JsonRenderer.render(new ContextExtractor(file, parts[0], parts[1]).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }

    private static String[] splitElement(String element) {
        int hash = element.indexOf('#');
        if (hash < 0) throw new IllegalArgumentException("--element must be ClassName#methodName");
        return new String[] {element.substring(0, hash), element.substring(hash + 1)};
    }
}
