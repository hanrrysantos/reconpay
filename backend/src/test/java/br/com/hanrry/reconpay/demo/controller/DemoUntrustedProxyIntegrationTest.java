package br.com.hanrry.reconpay.demo.controller;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.demo.controller.DemoForwardedAddressIntegrationTest.DemoHttp;
import br.com.hanrry.reconpay.demo.service.DemoPurger;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/* The test client connects from 127.0.0.1, which this context does not trust as a proxy. */
@TestPropertySource(properties = {
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=192\\.0\\.2\\.1"
})
class DemoUntrustedProxyIntegrationTest extends AbstractIntegrationTest {

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
    void forwardedAddressesFromAnUntrustedConnectionAreIgnored() throws Exception {
        tokens.add(DemoHttp.created(DemoHttp.createDemo(port, "1.1.1.1")));
        tokens.add(DemoHttp.created(DemoHttp.createDemo(port, "2.2.2.2")));
        HttpResponse<String> third = DemoHttp.createDemo(port, "3.3.3.3");

        assertThat(third.statusCode()).isEqualTo(429);
        assertThat((String) JsonPath.read(third.body(), "$.error")).isEqualTo("TOO_MANY_REQUESTS");
    }
}
