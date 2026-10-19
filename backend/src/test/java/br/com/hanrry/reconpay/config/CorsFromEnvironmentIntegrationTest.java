package br.com.hanrry.reconpay.config;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "reconpay.cors.allowed-origins= https://reconpay.vercel.app , https://app.hanrry.top/")
class CorsFromEnvironmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromEachConfiguredOriginIsAllowed() throws Exception {
        for (String origin : new String[]{"https://reconpay.vercel.app", "https://app.hanrry.top"}) {
            preflight(origin, "/api/auth/login")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        }
        preflight("https://reconpay.vercel.app", "/api/merchants")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://reconpay.vercel.app"));
    }

    @Test
    void defaultLocalhostOriginsAreNoLongerAllowed() throws Exception {
        for (String origin : new String[]{"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000", "http://127.0.0.1:3000"}) {
            preflight(origin, "/api/auth/login")
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    private ResultActions preflight(String origin, String path) throws Exception {
        return mockMvc.perform(options(path)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization"));
    }
}
