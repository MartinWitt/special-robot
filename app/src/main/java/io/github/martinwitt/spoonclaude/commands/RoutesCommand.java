package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import io.github.martinwitt.spoonclaude.routes.SpringRoutesFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "routes", description = "List HTTP endpoints from Spring MVC, JAX-RS, and Spring WebFlux Functional")
public class RoutesCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to scan")
    private String src;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new SpringRoutesFinder(src).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
