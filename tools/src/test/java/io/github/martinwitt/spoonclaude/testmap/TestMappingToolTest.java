package io.github.martinwitt.spoonclaude.testmap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestMappingToolTest {

    @TempDir
    Path srcDir;

    @TempDir
    Path testDir;

    @Test
    void findsCoveringTestsByTypeReference() throws IOException {
        writeSrc("UserService.java", """
                public class UserService { public String load(Long id) { return ""; } }
                """);
        writeTest("UserServiceTest.java", """
                public class UserServiceTest {
                  private UserService svc;
                  @Test public void testLoad() { svc.load(1L); }
                }
                """);

        var result = new TestMappingTool(srcDir.toString(), testDir.toString(), "UserService").execute();

        assertThat(result.coveringTests()).contains("UserServiceTest");
    }

    @Test
    void detectsUncoveredMethods() throws IOException {
        writeSrc("OrderService.java", """
                public class OrderService {
                  public void create() {}
                  public void delete() {}
                }
                """);
        writeTest("OrderServiceTest.java", """
                public class OrderServiceTest {
                  private OrderService svc;
                  @Test public void testCreate() { svc.create(); }
                }
                """);

        var result = new TestMappingTool(srcDir.toString(), testDir.toString(), "OrderService").execute();

        assertThat(result.uncoveredMethods()).contains("delete");
        assertThat(result.uncoveredMethods()).doesNotContain("create");
    }

    @Test
    void doesNotCountForeignMethodInvocationsAsCoverage() throws IOException {
        writeSrc("OrderService.java", """
                public class OrderService {
                  public void cancel() {}
                }
                """);
        writeTest("UnrelatedTest.java", """
                public class UnrelatedTest {
                  private java.util.concurrent.Future<?> future;
                  @org.junit.jupiter.api.Test public void someTest() { future.cancel(true); }
                }
                """);

        var result = new TestMappingTool(srcDir.toString(), testDir.toString(), "OrderService").execute();

        assertThat(result.uncoveredMethods()).contains("cancel");
    }

    @Test
    void findsTestsByNamingConvention() throws IOException {
        writeSrc("PaymentService.java", """
                public class PaymentService { public void pay() {} }
                """);
        writeTest("PaymentServiceTest.java", """
                public class PaymentServiceTest {
                  private PaymentService ps;
                  public void someMethod() { ps.pay(); }
                }
                """);

        var result = new TestMappingTool(srcDir.toString(), testDir.toString(), "PaymentService").execute();

        assertThat(result.coveringTests()).contains("PaymentServiceTest");
    }

    private void writeSrc(String filename, String content) throws IOException {
        Files.writeString(srcDir.resolve(filename), content);
    }

    private void writeTest(String filename, String content) throws IOException {
        Files.writeString(testDir.resolve(filename), content);
    }
}
