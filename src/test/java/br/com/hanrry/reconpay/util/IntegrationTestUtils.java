package br.com.hanrry.reconpay.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class IntegrationTestUtils {

    private static final String BEARER = "Bearer ";

    private IntegrationTestUtils() {
    }

    public static String obtainAdminToken(MockMvc mockMvc) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@reconpay.local",
                                  "password": "DevAdmin@2026"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.token");
    }

    public static String obtainAnalystToken(MockMvc mockMvc) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "analyst@reconpay.local",
                                  "password": "DevAnalyst@2026"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.token");
    }

    /**
     * Access is deny-by-default, so any analyst assertion against a merchant has
     * to be preceded by a grant. The grant endpoint replaces the whole set, so
     * this reads the current one first and keeps earlier tests working.
     */
    public static void grantAnalystAccess(MockMvc mockMvc, String adminToken, UUID merchantId) throws Exception {
        UUID analystId = analystId(mockMvc, adminToken);

        String current = mockMvc.perform(get("/api/users/{id}/merchants", analystId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Set<String> merchantIds = new LinkedHashSet<>(
                com.jayway.jsonpath.JsonPath.<List<String>>read(current, "$.merchantIds")
        );
        merchantIds.add(merchantId.toString());

        String body = merchantIds.stream()
                .map("\"%s\""::formatted)
                .collect(Collectors.joining(",", "{\"merchantIds\":[", "]}"));

        mockMvc.perform(put("/api/users/{id}/merchants", analystId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    public static UUID analystId(MockMvc mockMvc, String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/users/email")
                        .param("email", "analyst@reconpay.local")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));
    }
}
