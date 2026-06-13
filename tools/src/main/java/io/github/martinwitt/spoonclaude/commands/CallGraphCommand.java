package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.callgraph.CallGraphTool;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "callgraph", description = "Show callers and callees of a method up to --depth levels")
public class CallGraphCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Path to source file or directory")
    private String src;

    @Option(names = "--method", required = true, description = "ClassName#methodName (e.g. UserService#findById)")
    private String method;

    @Option(names = "--depth", defaultValue = "1", description = "Traversal depth (default: 1)")
    private int depth;

    @Override
    public void run() {
        try {
            var parts = splitElement(method);
            System.out.println(JsonRenderer.render(new CallGraphTool(src, parts[0], parts[1], depth).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }

    private static String[] splitElement(String element) {
        int hash = element.indexOf('#');
        if (hash < 0) throw new IllegalArgumentException("--method must be ClassName#methodName");
        return new String[] {element.substring(0, hash), element.substring(hash + 1)};
    }
}
