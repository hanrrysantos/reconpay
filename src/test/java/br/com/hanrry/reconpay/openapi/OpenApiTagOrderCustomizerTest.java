package br.com.hanrry.reconpay.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiTagOrderCustomizerTest {

    private static final List<String> OPERATIONAL_ORDER = List.of(
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

    @Test
    void customiseReplacesOutOfOrderTagsWithOperationalOrderAndDescriptions() {
        OpenAPI openApi = new OpenAPI().tags(List.of(
                new Tag().name("Reconciliations").description("fora de ordem"),
                new Tag().name("Authentication").description("")
        ));

        new OpenApiTagOrderCustomizer().customise(openApi);

        assertThat(openApi.getTags()).extracting(Tag::getName).containsExactlyElementsOf(OPERATIONAL_ORDER);
        assertThat(openApi.getTags()).extracting(Tag::getDescription).allSatisfy(description ->
                assertThat(description).isNotBlank());
        assertThat(openApi.getTags()).extracting(Tag::getDescription)
                .containsExactlyElementsOf(OpenApiTags.ordered().stream().map(Tag::getDescription).toList());
        assertThat(openApi.getTags()).extracting(Tag::getDescription).doesNotContain("fora de ordem", "");
    }
}
