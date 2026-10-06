package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.DiscrepancyNotFoundException;
import br.com.hanrry.reconpay.exception.DiscrepancyResolutionConflictException;
import br.com.hanrry.reconpay.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscrepancyResolutionErrorTest {

    @RestController
    static class TestController {

        @GetMapping("/test/discrepancy-not-found")
        void notFound() {
            throw new DiscrepancyNotFoundException("discrepancy not found");
        }

        @GetMapping("/test/discrepancy-resolution-conflict")
        void conflict() {
            throw new DiscrepancyResolutionConflictException("resolution conflict");
        }
    }

    private org.springframework.test.web.servlet.MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void discrepancyNotFoundReturnsStandardNotFound() throws Exception {
        mvc().perform(get("/test/discrepancy-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void discrepancyResolutionConflictReturnsStandardConflict() throws Exception {
        mvc().perform(get("/test/discrepancy-resolution-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
