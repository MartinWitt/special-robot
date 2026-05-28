package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.impact.ImpactAnalyzer;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "impact", description = "Analyze what breaks if you change a class or method")
public class ImpactCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to analyze")
    private String src;

    @Option(names = "--element", required = true, description = "ClassName or ClassName#methodName")
    private String element;

    @Override
    public void run() {
        try {
            int hash = element.indexOf('#');
            var className = hash >= 0 ? element.substring(0, hash) : element;
            var methodName = hash >= 0 ? element.substring(hash + 1) : null;
            System.out.println(JsonRenderer.render(new ImpactAnalyzer(src, className, methodName).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
