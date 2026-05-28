package io.github.martinwitt.spoonclaude.routes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpringRoutesFinderTest {

    @TempDir
    Path tempDir;

    @Test
    void findsSingleGetMapping() throws IOException {
        writeSource("UserController.java", """
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                public class UserController {
                  @GetMapping("/users")
                  public String list() { return ""; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes()).hasSize(1);
        var route = result.routes().get(0);
        assertThat(route.httpMethod()).isEqualTo("GET");
        assertThat(route.path()).isEqualTo("/users");
        assertThat(route.handlerClass()).isEqualTo("UserController");
        assertThat(route.handlerMethod()).isEqualTo("list");
        assertThat(route.framework()).isEqualTo("spring-mvc");
    }

    @Test
    void concatenatesClassRequestMappingWithMethodPath() throws IOException {
        writeSource("ApiController.java", """
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                @RequestMapping("/api")
                public class ApiController {
                  @GetMapping("/users/{id}")
                  public String get(String id) { return ""; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes()).hasSize(1);
        assertThat(result.routes().get(0).path()).isEqualTo("/api/users/{id}");
    }

    @Test
    void normalizesDoubleSlashesAndTrailingSlash() throws IOException {
        writeSource("X.java", """
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                @RequestMapping("/api/")
                public class X {
                  @GetMapping("/users/")
                  public String a() { return ""; }
                  @GetMapping("")
                  public String b() { return ""; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes())
                .extracting(RoutesResult.RouteEntry::path)
                .containsExactlyInAnyOrder("/api/users", "/api");
    }

    @Test
    void detectsAllSpringHttpVerbs() throws IOException {
        writeSource("VerbController.java", """
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class VerbController {
                  @GetMapping("/g") public String g() { return ""; }
                  @PostMapping("/p") public String p() { return ""; }
                  @PutMapping("/u") public String u() { return ""; }
                  @DeleteMapping("/d") public String d() { return ""; }
                  @PatchMapping("/a") public String a() { return ""; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes())
                .extracting(RoutesResult.RouteEntry::httpMethod)
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "PATCH");
    }

    @Test
    void detectsRequestMappingWithExplicitMethod() throws IOException {
        writeSource("RmController.java", """
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestMethod;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                public class RmController {
                  @RequestMapping(value = "/items", method = RequestMethod.POST)
                  public String create() { return ""; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes()).hasSize(1);
        assertThat(result.routes().get(0).httpMethod()).isEqualTo("POST");
        assertThat(result.routes().get(0).path()).isEqualTo("/items");
    }

    @Test
    void detectsJaxRsRoutes() throws IOException {
        writeSource("ItemResource.java", """
                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.POST;
                import jakarta.ws.rs.Path;
                @Path("/items")
                public class ItemResource {
                  @GET @Path("/{id}") public String one(String id) { return ""; }
                  @POST public String create() { return ""; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes()).hasSize(2);
        assertThat(result.routes()).allMatch(r -> r.framework().equals("jax-rs"));
        assertThat(result.routes())
                .extracting(RoutesResult.RouteEntry::path, RoutesResult.RouteEntry::httpMethod)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("/items/{id}", "GET"),
                        org.assertj.core.groups.Tuple.tuple("/items", "POST"));
    }

    @Test
    void detectsWebFluxFunctionalRoutes() throws IOException {
        writeSource("RouterConfig.java", """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.web.reactive.function.server.RouterFunction;
                import org.springframework.web.reactive.function.server.ServerResponse;
                import static org.springframework.web.reactive.function.server.RouterFunctions.route;
                import static org.springframework.web.reactive.function.server.RequestPredicates.*;
                @Configuration
                public class RouterConfig {
                  @Bean
                  public RouterFunction<ServerResponse> routes(HealthHandler h) {
                    return route(GET("/health"), h::ping)
                        .andRoute(POST("/items"), h::create);
                  }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes())
                .filteredOn(r -> r.framework().equals("spring-webflux-fn"))
                .hasSize(2);
        assertThat(result.routes())
                .filteredOn(r -> r.framework().equals("spring-webflux-fn"))
                .extracting(RoutesResult.RouteEntry::httpMethod, RoutesResult.RouteEntry::path)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GET", "/health"),
                        org.assertj.core.groups.Tuple.tuple("POST", "/items"));
        assertThat(result.routes())
                .filteredOn(r -> r.framework().equals("spring-webflux-fn"))
                .extracting(RoutesResult.RouteEntry::handlerClass)
                .containsOnly("RouterConfig");
    }

    @Test
    void returnsEmptyForNonControllerClass() throws IOException {
        writeSource("PlainPojo.java", "public class PlainPojo { public String name() { return \"\"; } }");

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes()).isEmpty();
    }

    @Test
    void picksUpControllerWithoutRestControllerAnnotation() throws IOException {
        writeSource("ViewController.java", """
                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                @Controller
                public class ViewController {
                  @GetMapping("/home")
                  public String home() { return "home"; }
                }
                """);

        var result = new SpringRoutesFinder(tempDir.toString()).execute();

        assertThat(result.routes()).hasSize(1);
        assertThat(result.routes().get(0).path()).isEqualTo("/home");
    }

    private void writeSource(String filename, String content) throws IOException {
        Files.writeString(tempDir.resolve(filename), content);
    }
}
