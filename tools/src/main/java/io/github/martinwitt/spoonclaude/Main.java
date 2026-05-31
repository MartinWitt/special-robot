package io.github.martinwitt.spoonclaude;

import io.github.martinwitt.spoonclaude.commands.AnnotationCommand;
import io.github.martinwitt.spoonclaude.commands.ApiCommand;
import io.github.martinwitt.spoonclaude.commands.CallGraphCommand;
import io.github.martinwitt.spoonclaude.commands.ContextCommand;
import io.github.martinwitt.spoonclaude.commands.CyclomaticCommand;
import io.github.martinwitt.spoonclaude.commands.DiffCommand;
import io.github.martinwitt.spoonclaude.commands.ImpactCommand;
import io.github.martinwitt.spoonclaude.commands.ImplCommand;
import io.github.martinwitt.spoonclaude.commands.RoutesCommand;
import io.github.martinwitt.spoonclaude.commands.SummaryCommand;
import io.github.martinwitt.spoonclaude.commands.TestMapCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "spoon-claude",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Spoon-based AST analysis tools for Claude Code",
        subcommands = {
            AnnotationCommand.class,
            ContextCommand.class,
            CallGraphCommand.class,
            ApiCommand.class,
            CyclomaticCommand.class,
            DiffCommand.class,
            ImpactCommand.class,
            ImplCommand.class,
            RoutesCommand.class,
            SummaryCommand.class,
            TestMapCommand.class
        })
public class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
