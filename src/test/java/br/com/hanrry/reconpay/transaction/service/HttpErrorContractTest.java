package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.exception.handler.GlobalExceptionHandler;
import br.com.hanrry.reconpay.transaction.controller.TransactionController;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HttpErrorContractTest {
    private org.springframework.test.web.servlet.MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new TransactionController(mock(TransactionService.class)),
                        new br.com.hanrry.reconpay.externalsettlement.controller.ExternalSettlementController(mock(br.com.hanrry.reconpay.externalsettlement.service.ExternalSettlementService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver()).build();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/api/merchants/not-a-uuid/transactions/00000000-0000-0000-0000-000000000002",
            "/api/merchants/00000000-0000-0000-0000-000000000001/transactions?status=UNKNOWN",
            "/api/merchants/00000000-0000-0000-0000-000000000001/transactions?fromDate=not-a-date"})
    void malformedQueryOrPathReturnsStandardBadRequest(String path) throws Exception {
        mvc().perform(get(path)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"{", "{\"status\":\"UNKNOWN\"}"})
    void malformedJsonReturnsStandardBadRequest(String body) throws Exception {
        mvc().perform(patch("/api/merchants/00000000-0000-0000-0000-000000000001/transactions/00000000-0000-0000-0000-000000000002/status")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test void missingMultipartFileReturnsStandardBadRequest() throws Exception {
        mvc().perform(multipart("/api/merchants/00000000-0000-0000-0000-000000000001/external-settlements/import"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
    @Test void staleStatusUpdateReturnsStandardConflict() throws Exception {
        var service = mock(TransactionService.class);
        when(service.updateStatus(any(), any(), any())).thenThrow(new OptimisticLockingFailureException("SQL details"));
        var mvc = MockMvcBuilders.standaloneSetup(new TransactionController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(patch("/api/merchants/00000000-0000-0000-0000-000000000001/transactions/00000000-0000-0000-0000-000000000002/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REFUNDED\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
