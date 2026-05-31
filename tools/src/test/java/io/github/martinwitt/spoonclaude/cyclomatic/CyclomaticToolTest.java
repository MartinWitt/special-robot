package io.github.martinwitt.spoonclaude.cyclomatic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CyclomaticToolTest {

    @TempDir
    Path tempDir;

    @Test
    void trivialMethodHasComplexityOne() throws IOException {
        writeSource("Sample.java", """
                public class Sample {
                  public void simple() {}
                }
                """);

        var result = new CyclomaticTool(tempDir.toString(), "Sample").execute();

        assertThat(result.className()).isEqualTo("Sample");
        assertThat(result.methods()).hasSize(1);
        assertThat(result.methods().get(0).complexity()).isEqualTo(1);
    }

    @Test
    void countsIfAsBranchPoint() throws IOException {
        writeSource("Sample.java", """
                public class Sample {
                  public void withIf(int x) {
                    if (x > 0) System.out.println(x);
                  }
                }
                """);

        var result = new CyclomaticTool(tempDir.toString(), "Sample").execute();

        assertThat(complexity(result, "withIf")).isEqualTo(2);
    }

    @Test
    void countsForEachAndTernary() throws IOException {
        writeSource("Sample.java", """
                public class Sample {
                  public int loopAndTernary(int[] arr) {
                    for (int a : arr) { System.out.println(a); }
                    return arr.length > 0 ? arr[0] : 0;
                  }
                }
                """);

        var result = new CyclomaticTool(tempDir.toString(), "Sample").execute();

        assertThat(complexity(result, "loopAndTernary")).isEqualTo(3);
    }

    @Test
    void countsLogicalOperatorsAndCatch() throws IOException {
        writeSource("Sample.java", """
                public class Sample {
                  public void complex(int x, boolean flag) {
                    if (x > 0 && flag) {
                      while (x-- > 0) {}
                    }
                    try { int y = x / 2; } catch (Exception e) {}
                  }
                }
                """);

        var result = new CyclomaticTool(tempDir.toString(), "Sample").execute();

        // 1(base) +1(if) +1(&&) +1(while) +1(catch) = 5
        assertThat(complexity(result, "complex")).isEqualTo(5);
    }

    @Test
    void signatureMatchesCompact() throws IOException {
        writeSource("Sample.java", """
                public class Sample {
                  public void go(String s) {}
                  public void go(int n) {}
                }
                """);

        var result = new CyclomaticTool(tempDir.toString(), "Sample").execute();

        assertThat(result.methods()).hasSize(2);
        assertThat(result.methods()).extracting("signature")
                .containsExactlyInAnyOrder("go(String)", "go(int)");
    }

    private int complexity(CyclomaticResult result, String methodName) {
        return result.methods().stream()
                .filter(m -> m.methodName().equals(methodName))
                .mapToInt(MethodComplexity::complexity)
                .findFirst()
                .orElseThrow(() -> new AssertionError("method not found: " + methodName));
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
