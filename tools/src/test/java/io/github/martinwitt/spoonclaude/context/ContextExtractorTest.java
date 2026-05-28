package io.github.martinwitt.spoonclaude.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContextExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsMethodSignatureAndBody() throws IOException {
        writeSource("UserService.java", """
                public class UserService {
                  public String findById(Long id) {
                    return id.toString();
                  }
                }
                """);

        var result = new ContextExtractor(tempDir.toString(), "UserService", "findById").execute();

        assertThat(result.method()).startsWith("UserService#findById(");
        assertThat(result.signature()).contains("findById").contains("Long id");
        assertThat(result.body()).contains("id.toString()");
    }

    @Test
    void disambiguatesOverloadByParameterType() throws IOException {
        writeSource("Repo.java", """
                public class Repo {
                  public String find(Long id) { return id.toString(); }
                  public String find(String name) { return name; }
                }
                """);

        var byLong = new ContextExtractor(tempDir.toString(), "Repo", "find(Long)").execute();
        var byString = new ContextExtractor(tempDir.toString(), "Repo", "find(String)").execute();

        assertThat(byLong.signature()).contains("Long id");
        assertThat(byString.signature()).contains("String name");
    }

    @Test
    void throwsOnAmbiguousOverloadWithoutSpec() throws IOException {
        writeSource("Repo.java", """
                public class Repo {
                  public String find(Long id) { return ""; }
                  public String find(String name) { return ""; }
                }
                """);

        assertThatThrownBy(() -> new ContextExtractor(tempDir.toString(), "Repo", "find").execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ambiguous");
    }

    @Test
    void extractsCalledMethodSignatures() throws IOException {
        writeSource("Service.java", """
                public class Service {
                  private Repo repo;
                  public String load(Long id) {
                    return repo.findById(id).toString();
                  }
                }
                """);

        var result = new ContextExtractor(tempDir.toString(), "Service", "load").execute();

        assertThat(result.calls()).anyMatch(c -> c.contains("findById"));
    }

    @Test
    void extractsAnnotations() throws IOException {
        writeSource("TxService.java", """
                public class TxService {
                  @Deprecated
                  public void oldMethod() {}
                }
                """);

        var result = new ContextExtractor(tempDir.toString(), "TxService", "oldMethod").execute();

        assertThat(result.annotations()).contains("@Deprecated");
    }

    @Test
    void throwsWhenTypeNotFound() throws IOException {
        writeSource("Empty.java", "public class Empty {}");

        assertThatThrownBy(() -> new ContextExtractor(tempDir.toString(), "Missing", "method").execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    void throwsWhenMethodNotFound() throws IOException {
        writeSource("Foo.java", "public class Foo {}");

        assertThatThrownBy(() -> new ContextExtractor(tempDir.toString(), "Foo", "bar").execute())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bar");
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
