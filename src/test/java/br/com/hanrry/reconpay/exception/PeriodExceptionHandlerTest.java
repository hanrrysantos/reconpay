package br.com.hanrry.reconpay.exception;

import br.com.hanrry.reconpay.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PeriodExceptionHandlerTest {

    @RestController
    static class TestController {

        @GetMapping("/test/period-conflict")
        void conflict() {
            throw new PeriodConflictException("Janela já travada");
        }

        @GetMapping("/test/period-not-found")
        void notFound() {
            throw new PeriodNotFoundException("Período não encontrado");
        }
    }

    private org.springframework.test.web.servlet.MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void periodConflictReturnsHttp409Conflict() throws Exception {
        mvc().perform(get("/test/period-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Janela já travada"));
    }

    @Test
    void periodNotFoundReturnsHttp404NotFound() throws Exception {
        mvc().perform(get("/test/period-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Período não encontrado"));
    }
}
