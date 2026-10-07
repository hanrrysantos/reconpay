package br.com.hanrry.reconpay.openapi;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ApiDocsIntegrationTest extends AbstractIntegrationTest {

    private static final List<String> TAG_ORDER = List.of(
            "Authentication",
            "Session",
            "Users",
            "Merchants",
            "Fee Rules",
            "Transactions",
            "External Settlements",
            "Bank Statements",
            "Reconciliations"
    );

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void apiDocsListsOperationalTagsAndNamesBothRoles() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode document = objectMapper.readTree(body);

        JsonNode tags = document.get("tags");
        assertThat(tags).isNotNull();
        assertThat(tags).hasSize(TAG_ORDER.size());
        for (int index = 0; index < TAG_ORDER.size(); index++) {
            JsonNode tag = tags.get(index);
            assertThat(tag.get("name").asText()).isEqualTo(TAG_ORDER.get(index));
            assertThat(tag.get("description").asText()).isNotBlank();
        }

        String description = document.path("info").path("description").asText();
        assertThat(description).contains("ADMIN");
        assertThat(description).contains("OPERATOR");
        assertThat(description).containsPattern("[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]");
        assertThat(description).doesNotContainPattern("\\d+\\.\\s*(GET|POST|PUT|PATCH|DELETE)\\b");
    }

    @Test
    void applicationYamlDoesNotSortSwaggerTags() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/application.yaml")) {
            assertThat(in).isNotNull();
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(yaml).doesNotContain("tags-sorter");
        }
    }
}
