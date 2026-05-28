package io.github.martinwitt.spoonclaude.impact;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImpactAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void findsDirectCallersOfMethod() throws IOException {
        writeSource("UserService.java", """
                public class UserService {
                  private UserRepo repo;
                  public String find(Long id) { return repo.findById(id); }
                }
                """);
        writeSource("UserRepo.java", """
                public class UserRepo {
                  public String findById(Long id) { return ""; }
                }
                """);

        var result = new ImpactAnalyzer(tempDir.toString(), "UserRepo", "findById").execute();

        assertThat(result.target()).startsWith("UserRepo#findById");
        assertThat(result.callerCount()).isEqualTo(result.directCallers().size());
        assertThat(result.directCallers()).anyMatch(c -> c.contains("find"));
    }

    @Test
    void findsTypeDependents() throws IOException {
        writeSource("Token.java", "public class Token { private String value; }");
        writeSource("TokenService.java", """
                public class TokenService {
                  public Token create() { return new Token(); }
                }
                """);
        writeSource("TokenController.java", """
                public class TokenController {
                  private TokenService svc;
                  public Token get() { return svc.create(); }
                }
                """);

        var result = new ImpactAnalyzer(tempDir.toString(), "Token", null).execute();

        assertThat(result.typeDependents()).contains("TokenService", "TokenController");
        assertThat(result.dependentCount()).isEqualTo(result.typeDependents().size());
    }

    @Test
    void reportsCallerPackageBreakdown() throws IOException {
        Files.createDirectories(tempDir.resolve("p"));
        Files.createDirectories(tempDir.resolve("q"));
        Files.writeString(tempDir.resolve("p/Target.java"), """
                package p;
                public class Target { public void run() {} }
                """);
        Files.writeString(tempDir.resolve("p/SamePkgCaller.java"), """
                package p;
                public class SamePkgCaller { void go(Target t) { t.run(); } }
                """);
        Files.writeString(tempDir.resolve("q/OtherPkgCaller.java"), """
                package q;
                import p.Target;
                public class OtherPkgCaller { void go(Target t) { t.run(); } }
                """);

        var result = new ImpactAnalyzer(tempDir.toString(), "p.Target", "run").execute();

        assertThat(result.callersByPackage()).containsKeys("p", "q");
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
