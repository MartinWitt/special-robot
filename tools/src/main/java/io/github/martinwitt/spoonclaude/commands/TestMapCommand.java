package io.github.martinwitt.spoonclaude.commands;

import io.github.martinwitt.spoonclaude.output.JsonRenderer;
import io.github.martinwitt.spoonclaude.testmap.TestMappingTool;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "testmap", description = "Find which tests cover a class and which public methods are uncovered")
public class TestMapCommand implements Runnable {

    @Option(names = "--src", required = true, description = "Production source directory")
    private String src;

    @Option(names = "--test-src", required = true, description = "Test source directory")
    private String testSrc;

    @Option(names = "--class", required = true, description = "Simple class name to analyze")
    private String className;

    @Override
    public void run() {
        try {
            System.out.println(JsonRenderer.render(new TestMappingTool(src, testSrc, className).execute()));
        } catch (Exception e) {
            System.err.println(JsonRenderer.renderError(e.getMessage()));
            System.exit(1);
        }
    }
}
