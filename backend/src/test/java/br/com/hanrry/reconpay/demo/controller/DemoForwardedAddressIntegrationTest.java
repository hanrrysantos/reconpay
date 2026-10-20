package br.com.hanrry.reconpay.demo.controller;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.demo.service.DemoPurger;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/* Real HTTP through Tomcat, as in prod: the test client connects from 127.0.0.1, an internal proxy.
 * The rate limiter lives in this context only, so fixed addresses start with a clean quota. */
@TestPropertySource(properties = "server.forward-headers-strategy=native")
class DemoForwardedAddressIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DemoPurger purger;

    private final List<String> tokens = new ArrayList<>();

    @AfterEach
    void purgeCreatedDemos() throws Exception {
        for (String token : tokens) {
            purger.purge(DemoHttp.userIdOf(port, token));
        }
        tokens.clear();
    }

    @Test
    void theRightmostForwardedAddressIsTheOneCounted() throws Exception {
        tokens.add(DemoHttp.created(DemoHttp.createDemo(port, "1.1.1.1, 9.9.9.9")));
        tokens.add(DemoHttp.created(DemoHttp.createDemo(port, "1.1.1.1, 9.9.9.9")));
        HttpResponse<String> third = DemoHttp.createDemo(port, "2.2.2.2, 9.9.9.9");

        assertThat(third.statusCode()).isEqualTo(429);
        assertThat((String) JsonPath.read(third.body(), "$.error")).isEqualTo("TOO_MANY_REQUESTS");
        tokens.add(DemoHttp.created(DemoHttp.createDemo(port, "1.1.1.1, 8.8.8.8")));
    }

    static final class DemoHttp {

        private static final HttpClient CLIENT = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        private DemoHttp() {
        }

        static HttpResponse<String> createDemo(int port, String forwardedFor) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/demo"))
                    .header("X-Forwarded-For", forwardedFor)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        }

        static String created(HttpResponse<String> response) {
            assertThat(response.statusCode()).isEqualTo(201);
            return JsonPath.read(response.body(), "$.token");
        }

        static UUID userIdOf(int port, String token) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/me"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            return UUID.fromString(JsonPath.read(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body(), "$.id"));
        }
    }
}
