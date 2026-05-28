package io.github.martinwitt.spoonclaude.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiSurfaceToolTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsOnlyPublicAndProtectedMethods() throws IOException {
        writeSource("MyService.java", """
                public class MyService {
                  public String publicMethod() { return ""; }
                  protected void protectedMethod() {}
                  private void privateMethod() {}
                  void packageMethod() {}
                }
                """);

        var result = new ApiSurfaceTool(tempDir.toString(), "MyService").execute();

        assertThat(result.methods()).hasSize(2);
        assertThat(result.methods()).anyMatch(m -> m.signature().contains("publicMethod"));
        assertThat(result.methods()).anyMatch(m -> m.signature().contains("protectedMethod"));
        assertThat(result.methods()).noneMatch(m -> m.signature().contains("privateMethod"));
    }

    @Test
    void capturesAnnotations() throws IOException {
        writeSource("Annotated.java", """
                public class Annotated {
                  @Deprecated
                  @SuppressWarnings("all")
                  public void old() {}
                }
                """);

        var result = new ApiSurfaceTool(tempDir.toString(), "Annotated").execute();

        assertThat(result.methods().get(0).annotations())
                .contains("@Deprecated")
                .contains("@SuppressWarnings");
    }

    @Test
    void capturesJavadoc() throws IOException {
        writeSource("Documented.java", """
                public class Documented {
                  /** Returns the name. */
                  public String getName() { return ""; }
                }
                """);

        var result = new ApiSurfaceTool(tempDir.toString(), "Documented").execute();

        assertThat(result.methods().get(0).javadoc()).contains("Returns the name");
    }

    @Test
    void capturesConstructorsWithThrowsAndGenerics() throws IOException {
        writeSource("Box.java", """
                public class Box<T> {
                  public Box(T value) throws IllegalStateException {}
                  protected Box() {}
                  private Box(int internal) {}
                }
                """);

        var result = new ApiSurfaceTool(tempDir.toString(), "Box").execute();

        assertThat(result.constructors()).hasSize(2);
        assertThat(result.constructors()).anyMatch(c -> c.signature().contains("throws IllegalStateException"));
        assertThat(result.constructors()).noneMatch(c -> c.signature().contains("internal"));
    }

    @Test
    void preservesGenericReturnTypesAndModifiers() throws IOException {
        writeSource("Repo.java", """
                import java.util.Optional;
                public class Repo {
                  public static final Optional<String> findById(Long id) throws IllegalAccessException { return Optional.empty(); }
                }
                """);

        var result = new ApiSurfaceTool(tempDir.toString(), "Repo").execute();

        var sig = result.methods().get(0).signature();
        assertThat(sig).contains("Optional<String>");
        assertThat(sig).contains("static");
        assertThat(sig).contains("final");
        assertThat(sig).contains("throws IllegalAccessException");
    }

    @Test
    void returnsPublicFields() throws IOException {
        writeSource("Config.java", """
                public class Config {
                  public static final int MAX = 10;
                  private String secret;
                }
                """);

        var result = new ApiSurfaceTool(tempDir.toString(), "Config").execute();

        assertThat(result.fields()).hasSize(1);
        assertThat(result.fields().get(0).name()).isEqualTo("MAX");
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
