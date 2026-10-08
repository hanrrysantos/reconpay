package br.com.hanrry.reconpay.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    public static String obtainOperatorToken(MockMvc mockMvc) throws Exception {
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
     * Access is deny-by-default, so any operator assertion against a merchant has
     * to be preceded by a grant. The grant endpoint replaces the whole set, so
     * this reads the current one first and keeps earlier tests working.
     */
    public static void grantOperatorAccess(MockMvc mockMvc, String adminToken, UUID merchantId) throws Exception {
        UUID operatorId = operatorId(mockMvc, adminToken);

        String current = mockMvc.perform(get("/api/users/{id}/merchants", operatorId)
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

        mockMvc.perform(put("/api/users/{id}/merchants", operatorId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    public static UUID operatorId(MockMvc mockMvc, String adminToken) throws Exception {
        String response = mockMvc.perform(get("/api/users/email")
                        .param("email", "analyst@reconpay.local")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));
    }

    public static byte[] csvLargerThanFiveMegabytes() {
        return new byte[Math.toIntExact(DataSize.ofMegabytes(5).toBytes()) + 1];
    }

    public static HttpResponse<String> postMultipartFile(
            int port,
            String path,
            String bearerToken,
            String fileName,
            byte[] content) throws Exception {
        String boundary = "----ReconPayBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: text/csv\r\n"
                + "\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header(HttpHeaders.AUTHORIZATION, BEARER + bearerToken)
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(
                        HttpRequest.BodyPublishers.ofByteArray(prefix),
                        HttpRequest.BodyPublishers.ofByteArray(content),
                        HttpRequest.BodyPublishers.ofByteArray(suffix)))
                .build();

        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }
}
