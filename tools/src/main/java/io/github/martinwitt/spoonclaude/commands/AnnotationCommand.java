package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.annotation.AnnotationUsageTool;
import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "annotation", description = "Find all usages of an annotation across a source tree")
public class AnnotationCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Source directory to analyze")
    private String src;

    @Option(
            names = "--annotation",
            required = true,
            description =
                    "Annotation simple name (e.g. Transactional) or FQN (e.g. org.springframework.transaction.annotation.Transactional)")
    private String annotation;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new AnnotationUsageTool(src, annotation).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
