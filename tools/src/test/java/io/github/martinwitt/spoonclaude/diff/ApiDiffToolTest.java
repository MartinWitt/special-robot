package io.github.martinwitt.spoonclaude.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiDiffToolTest {

    @TempDir
    Path beforeDir;

    @TempDir
    Path afterDir;

    @Test
    void detectsAddedMethod() throws IOException {
        write(beforeDir, "Repo.java", """
                public class Repo {
                  public void save(String s) {}
                }
                """);
        write(afterDir, "Repo.java", """
                public class Repo {
                  public void save(String s) {}
                  public String load(Long id) { return ""; }
                }
                """);

        var result = new ApiDiffTool(beforeDir.toString(), afterDir.toString()).execute();

        assertThat(result.added()).hasSize(1);
        assertThat(result.added().get(0).classFqn()).isEqualTo("Repo");
        assertThat(result.added().get(0).signature()).contains("load");
        assertThat(result.removed()).isEmpty();
        assertThat(result.changed()).isEmpty();
    }

    @Test
    void detectsRemovedMethod() throws IOException {
        write(beforeDir, "Repo.java", """
                public class Repo {
                  public void save(String s) {}
                  public void delete(Long id) {}
                }
                """);
        write(afterDir, "Repo.java", """
                public class Repo {
                  public void save(String s) {}
                }
                """);

        var result = new ApiDiffTool(beforeDir.toString(), afterDir.toString()).execute();

        assertThat(result.removed()).hasSize(1);
        assertThat(result.removed().get(0).signature()).contains("delete");
        assertThat(result.added()).isEmpty();
    }

    @Test
    void detectsAllMembersOfAddedClass() throws IOException {
        write(beforeDir, "Existing.java", "public class Existing { public void run() {} }");
        write(afterDir, "Existing.java", "public class Existing { public void run() {} }");
        write(afterDir, "Brand.java", """
                public class Brand {
                  public String name() { return ""; }
                  protected int code() { return 0; }
                  private void secret() {}
                }
                """);

        var result = new ApiDiffTool(beforeDir.toString(), afterDir.toString()).execute();

        assertThat(result.added()).extracting(e -> e.classFqn()).contains("Brand");
        assertThat(result.added()).anyMatch(e -> e.signature().contains("name()"));
        assertThat(result.added()).anyMatch(e -> e.signature().contains("code()"));
        assertThat(result.added()).noneMatch(e -> e.signature().contains("secret"));
    }

    @Test
    void detectsChangedSignatureSameCompactKey() throws IOException {
        write(beforeDir, "Repo.java", """
                public class Repo {
                  public String findById(Long id) { return ""; }
                }
                """);
        write(afterDir, "Repo.java", """
                public class Repo {
                  public String findById(Long id) throws IllegalAccessException { return ""; }
                }
                """);

        var result = new ApiDiffTool(beforeDir.toString(), afterDir.toString()).execute();

        assertThat(result.changed()).hasSize(1);
        assertThat(result.changed().get(0).before()).doesNotContain("throws");
        assertThat(result.changed().get(0).after()).contains("throws IllegalAccessException");
        assertThat(result.added()).isEmpty();
        assertThat(result.removed()).isEmpty();
    }

    @Test
    void ignoresPrivateAndPackageMembers() throws IOException {
        write(beforeDir, "Repo.java", """
                public class Repo {
                  public void shown() {}
                  private void hidden() {}
                  void pkg() {}
                }
                """);
        write(afterDir, "Repo.java", """
                public class Repo {
                  public void shown() {}
                  private String hidden(int x) { return ""; }
                  String pkg(int x) { return ""; }
                }
                """);

        var result = new ApiDiffTool(beforeDir.toString(), afterDir.toString()).execute();

        assertThat(result.added()).isEmpty();
        assertThat(result.removed()).isEmpty();
        assertThat(result.changed()).isEmpty();
    }

    @Test
    void emptyDiffForIdenticalSurface() throws IOException {
        write(beforeDir, "Same.java", "public class Same { public void run() {} }");
        write(afterDir, "Same.java", "public class Same { public void run() {} }");

        var result = new ApiDiffTool(beforeDir.toString(), afterDir.toString()).execute();

        assertThat(result.added()).isEmpty();
        assertThat(result.removed()).isEmpty();
        assertThat(result.changed()).isEmpty();
    }

    private static void write(Path dir, String filename, String content) throws IOException {
        Files.writeString(dir.resolve(filename), content);
    }
}
