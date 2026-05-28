package io.github.martinwitt.spoonclaude.callgraph;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CallGraphToolTest {

    @TempDir
    Path tempDir;

    @Test
    void findsCallers() throws IOException {
        writeSource("Controller.java", """
                public class Controller {
                  private Service service;
                  public String handle(Long id) {
                    return service.load(id);
                  }
                }
                """);
        writeSource("Service.java", """
                public class Service {
                  public String load(Long id) { return id.toString(); }
                }
                """);

        var result = new CallGraphTool(tempDir.toString(), "Service", "load", 1).execute();

        assertThat(result.target()).startsWith("Service#load(");
        assertThat(result.callers()).anyMatch(c -> c.contains("handle"));
    }

    @Test
    void disambiguatesOverloadsByArgCount() throws IOException {
        writeSource("Logger.java", """
                public class Logger {
                  public void log(String msg) {}
                  public void log(String msg, String tag) {}
                }
                """);
        writeSource("UsesOne.java", """
                public class UsesOne {
                  void go(Logger l) { l.log("hi"); }
                }
                """);
        writeSource("UsesTwo.java", """
                public class UsesTwo {
                  void go(Logger l) { l.log("hi", "tag"); }
                }
                """);

        var oneArg = new CallGraphTool(tempDir.toString(), "Logger", "log(String)", 1).execute();

        assertThat(oneArg.callers()).anyMatch(c -> c.contains("UsesOne"));
        assertThat(oneArg.callers()).noneMatch(c -> c.contains("UsesTwo"));
    }

    @Test
    void findsCallees() throws IOException {
        writeSource("Service.java", """
                public class Service {
                  private Repo repo;
                  public String load(Long id) {
                    return repo.findById(id).toString();
                  }
                }
                """);

        var result = new CallGraphTool(tempDir.toString(), "Service", "load", 1).execute();

        assertThat(result.callees()).anyMatch(c -> c.contains("findById"));
    }

    @Test
    void respectsDepthLimit() throws IOException {
        writeSource("A.java", "public class A { public void run() { new B().step(); } }");
        writeSource("B.java", "public class B { public void step() { new C().deep(); } }");
        writeSource("C.java", "public class C { public void deep() {} }");

        var depth1 = new CallGraphTool(tempDir.toString(), "A", "run", 1).execute();
        var depth2 = new CallGraphTool(tempDir.toString(), "A", "run", 2).execute();

        assertThat(depth1.callees()).anyMatch(c -> c.contains("step"));
        assertThat(depth2.callees()).anyMatch(c -> c.contains("deep"));
        assertThat(depth1.depth()).isEqualTo(1);
    }

    @Test
    void returnsEmptyWhenNoCallersFound() throws IOException {
        writeSource("Isolated.java", "public class Isolated { public void alone() {} }");

        var result = new CallGraphTool(tempDir.toString(), "Isolated", "alone", 1).execute();

        assertThat(result.callers()).isEmpty();
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
