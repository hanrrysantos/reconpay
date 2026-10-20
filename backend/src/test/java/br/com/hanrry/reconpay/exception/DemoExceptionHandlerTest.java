package br.com.hanrry.reconpay.exception;

import br.com.hanrry.reconpay.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DemoExceptionHandlerTest {

    @RestController
    static class TestController {

        @GetMapping("/test/demo-rate-limit")
        void rateLimited() {
            throw new DemoRateLimitException();
        }

        @GetMapping("/test/demo-unavailable")
        void unavailable() {
            throw new DemoUnavailableException();
        }
    }

    private org.springframework.test.web.servlet.MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void demoRateLimitReturnsHttp429TooManyRequests() throws Exception {
        mvc().perform(get("/test/demo-rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message")
                        .value("Limite de demonstrações atingido. Tente novamente em até 1 hora."));
    }

    @Test
    void demoUnavailableReturnsHttp503ServiceUnavailable() throws Exception {
        mvc().perform(get("/test/demo-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message")
                        .value("Demonstrações esgotadas no momento. Tente novamente mais tarde."));
    }
}
