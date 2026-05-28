package io.github.martinwitt.spoonclaude.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassSummaryToolTest {

    @TempDir
    Path tempDir;

    @Test
    void capturesClassMetadata() throws IOException {
        writeSource("UserService.java", """
                /** Handles user lifecycle. */
                @Deprecated
                public class UserService extends BaseService implements Auditable, Reloadable {
                  public void doNothing() {}
                }
                """);

        var result = new ClassSummaryTool(tempDir.toString(), "UserService").execute();

        assertThat(result.className()).isEqualTo("UserService");
        assertThat(result.javadoc()).contains("Handles user lifecycle");
        assertThat(result.classAnnotations()).contains("@Deprecated");
        assertThat(result.superClass()).isEqualTo("BaseService");
        assertThat(result.interfaces()).containsExactlyInAnyOrder("Auditable", "Reloadable");
    }

    @Test
    void capturesPublicApi() throws IOException {
        writeSource("OrderService.java", """
                public class OrderService {
                  public OrderService(Repo repo) {}
                  public void place(Order o) {}
                  protected int score() { return 0; }
                  private void hidden() {}
                  public final Repo repo = null;
                  private int internal;
                }
                """);

        var result = new ClassSummaryTool(tempDir.toString(), "OrderService").execute();

        assertThat(result.constructors()).hasSize(1);
        assertThat(result.constructors().get(0).signature()).contains("OrderService(Repo repo)");
        assertThat(result.methods()).hasSize(2);
        assertThat(result.methods()).anyMatch(m -> m.signature().contains("place(Order o)"));
        assertThat(result.methods()).anyMatch(m -> m.signature().contains("score()"));
        assertThat(result.methods()).noneMatch(m -> m.signature().contains("hidden"));
        assertThat(result.fields()).hasSize(1);
        assertThat(result.fields().get(0).name()).isEqualTo("repo");
    }

    @Test
    void capturesOutboundDependencies() throws IOException {
        writeSource("OrderRepo.java", "public interface OrderRepo {}");
        writeSource("Mailer.java", "public class Mailer {}");
        writeSource("OrderService.java", """
                public class OrderService {
                  private OrderRepo repo;
                  public void send(Mailer m) { m.toString(); }
                }
                """);

        var result = new ClassSummaryTool(tempDir.toString(), "OrderService").execute();

        assertThat(result.outboundDependencies()).contains("OrderRepo", "Mailer");
    }

    @Test
    void excludesPrimitivesAndJavaLangAndSelfFromDependencies() throws IOException {
        writeSource("Calculator.java", """
                public class Calculator {
                  public int add(int a, int b) { return a + b; }
                  public String name() { return "calc"; }
                  public Calculator copy() { return new Calculator(); }
                }
                """);

        var result = new ClassSummaryTool(tempDir.toString(), "Calculator").execute();

        assertThat(result.outboundDependencies()).doesNotContain("int", "String", "Calculator");
    }

    @Test
    void throwsWhenTypeNotFound() throws IOException {
        writeSource("Existing.java", "public class Existing {}");

        assertThatThrownBy(() -> new ClassSummaryTool(tempDir.toString(), "Missing").execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing");
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
