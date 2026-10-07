package br.com.hanrry.reconpay.openapi;

import br.com.hanrry.reconpay.auth.controller.AuthController;
import br.com.hanrry.reconpay.auth.controller.MeController;
import br.com.hanrry.reconpay.auth.controller.UserController;
import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.auth.dto.CreateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.MerchantAccessRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.VerifyEmailRequestDTO;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.feerule.controller.FeeRuleController;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.merchant.controller.MerchantController;
import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.transaction.controller.TransactionController;
import br.com.hanrry.reconpay.transaction.dto.CreateTransactionRequestDTO;
import br.com.hanrry.reconpay.transaction.dto.UpdateTransactionStatusRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final Pattern PORTUGUESE = Pattern.compile("[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]");
    private static final String UUID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String BEARER = "Bearer Authentication";
    private static final Map<String, String> ERROR_CODES = Map.of(
            "400", "VALIDATION_ERROR",
            "401", "UNAUTHORIZED",
            "403", "FORBIDDEN",
            "404", "NOT_FOUND",
            "409", "CONFLICT"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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

    @Test
    void publicAuthDocumentsCodesExamplesAndOpenSecurity() throws Exception {
        JsonNode document = apiDocs();

        assertOperation(document, new Op(
                "post",
                "/api/auth/login",
                Set.of("200", "400", "401"),
                false,
                AuthRequestDTO.class,
                Map.of("email", "admin@reconpay.local", "password", "DevAdmin@2026"),
                "200",
                List.of("token", "type", "expiresIn"),
                false,
                Set.of(),
                Set.of(),
                List.of()
        ));
        assertOperation(document, new Op(
                "post",
                "/api/auth/register",
                Set.of("201", "400", "409"),
                false,
                UserRequestDTO.class,
                Map.of(),
                "201",
                List.of("id", "name", "email", "role", "active", "createdAt"),
                false,
                Set.of(),
                Set.of(),
                List.of()
        ));
        assertOperation(document, new Op(
                "post",
                "/api/auth/verify-email",
                Set.of("204", "400"),
                false,
                VerifyEmailRequestDTO.class,
                Map.of(),
                null,
                List.of(),
                false,
                Set.of("204"),
                Set.of(),
                List.of()
        ));
        assertOperation(document, new Op(
                "get",
                "/api/auth/verify-email",
                Set.of("200", "400"),
                false,
                null,
                Map.of(),
                null,
                List.of(),
                false,
                Set.of(),
                Set.of("200", "400"),
                List.of()
        ));

        assertNoMappingAnnotations(AuthController.class, Operation.class, GetMapping.class, PostMapping.class);
    }

    @Test
    void sessionEndpointsDocumentCodesExamplesAndBearer() throws Exception {
        JsonNode document = apiDocs();

        assertOperation(document, new Op(
                "get",
                "/api/me",
                Set.of("200", "401"),
                true,
                null,
                Map.of(),
                "200",
                List.of("id", "name", "email", "role", "active"),
                false,
                Set.of(),
                Set.of(),
                List.of()
        ));
        assertOperation(document, new Op(
                "get",
                "/api/me/merchants",
                Set.of("200", "400", "401"),
                true,
                null,
                Map.of(),
                "200",
                List.of("id", "name", "document"),
                true,
                Set.of(),
                Set.of(),
                List.of("OPERATOR", "grants", "ADMIN", "ativos")
        ));
        assertQueryExample(document, "get", "/api/me/merchants", "page", "0");
        assertQueryExample(document, "get", "/api/me/merchants", "size", "20");
        assertQueryExample(document, "get", "/api/me/merchants", "sort", "name,asc");

        assertNoMappingAnnotations(MeController.class, Operation.class, GetMapping.class, RequestMapping.class);
    }

    @Test
    void userAdministrationDocumentsTheResponseTable() throws Exception {
        JsonNode document = apiDocs();
        List<String> userFields = List.of("id", "name", "email", "role", "active", "createdAt");
        List<String> idOrigin = List.of("criação", "listagem");

        assertOperation(document, new Op(
                "post", "/api/users",
                Set.of("201", "400", "401", "403", "409"),
                true, CreateUserRequestDTO.class, Map.of(),
                "201", userFields, false, Set.of(), Set.of(), List.of()
        ));
        assertOperation(document, new Op(
                "get", "/api/users",
                Set.of("200", "400", "401", "403"),
                true, null, Map.of(),
                "200", userFields, true, Set.of(), Set.of(), List.of()
        ));
        assertQueryExample(document, "get", "/api/users", "page", "0");
        assertQueryExample(document, "get", "/api/users", "size", "20");
        assertQueryExample(document, "get", "/api/users", "sort", "name,asc");
        assertOperation(document, new Op(
                "get", "/api/users/{id}",
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", userFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "get", "/api/users/email",
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", userFields, false, Set.of(), Set.of(), List.of()
        ));
        assertQueryExample(document, "get", "/api/users/email", "email", "ana.operadora@reconpay.local");
        assertOperation(document, new Op(
                "patch", "/api/users/{id}/activation",
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", userFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "get", "/api/users/{id}/merchants",
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", List.of("userId", "merchantIds"), false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "put", "/api/users/{id}/merchants",
                Set.of("200", "400", "401", "403", "404"),
                true, MerchantAccessRequestDTO.class, Map.of(),
                "200", List.of("userId", "merchantIds"), false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "put", "/api/users/{id}",
                Set.of("200", "400", "401", "403", "404"),
                true, UpdateUserRequestDTO.class, Map.of(),
                "200", userFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "delete", "/api/users/{id}",
                Set.of("204", "400", "401", "403", "404"),
                true, null, Map.of(),
                null, List.of(), false, Set.of("204"), Set.of(), idOrigin
        ));

        assertNoMappingAnnotations(
                UserController.class,
                Operation.class,
                GetMapping.class,
                PostMapping.class,
                PutMapping.class,
                PatchMapping.class,
                DeleteMapping.class,
                RequestMapping.class
        );
    }

    @Test
    void merchantEndpointsDocumentTheResponseTable() throws Exception {
        JsonNode document = apiDocs();
        List<String> merchantFields = List.of("id", "name", "document", "active", "createdAt");
        List<String> idOrigin = List.of("criação", "listagem");

        assertOperation(document, new Op(
                "post", "/api/merchants",
                Set.of("201", "400", "401", "403", "409"),
                true, MerchantRequestDTO.class, Map.of(),
                "201", merchantFields, false, Set.of(), Set.of(),
                List.of("auto-grant", "criador")
        ));
        assertOperation(document, new Op(
                "get", "/api/merchants",
                Set.of("200", "400", "401", "403"),
                true, null, Map.of(),
                "200", merchantFields, true, Set.of(), Set.of(), List.of()
        ));
        assertQueryExample(document, "get", "/api/merchants", "page", "0");
        assertQueryExample(document, "get", "/api/merchants", "size", "20");
        assertQueryExample(document, "get", "/api/merchants", "sort", "name,asc");
        assertOperation(document, new Op(
                "get", "/api/merchants/{id}",
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", merchantFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "put", "/api/merchants/{id}",
                Set.of("200", "400", "401", "403", "404"),
                true, UpdateMerchantRequestDTO.class, Map.of(),
                "200", merchantFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "delete", "/api/merchants/{id}",
                Set.of("204", "400", "401", "403", "404"),
                true, null, Map.of(),
                null, List.of(), false, Set.of("204"), Set.of(), idOrigin
        ));

        assertNoMappingAnnotations(
                MerchantController.class,
                Operation.class,
                GetMapping.class,
                PostMapping.class,
                PutMapping.class,
                DeleteMapping.class,
                RequestMapping.class
        );
    }

    @Test
    void feeRuleEndpointsDocumentTheResponseTable() throws Exception {
        JsonNode document = apiDocs();
        List<String> feeRuleFields = List.of(
                "id", "merchantId", "merchantName", "paymentMethod", "installments",
                "feePercentage", "fixedFee", "active", "createdAt");
        List<String> idOrigin = List.of("criação", "listagem");
        String list = "/api/merchants/{merchantId}/fee-rules";
        String byId = "/api/merchants/{merchantId}/fee-rules/{id}";

        assertOperation(document, new Op(
                "get", list,
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", feeRuleFields, true, Set.of(), Set.of(), idOrigin
        ));
        assertQueryExample(document, "get", list, "page", "0");
        assertQueryExample(document, "get", list, "size", "20");
        assertQueryExample(document, "get", list, "sort", "createdAt,desc");
        assertOperation(document, new Op(
                "get", byId,
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", feeRuleFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "post", list,
                Set.of("201", "400", "401", "403", "404", "409"),
                true, FeeRuleRequestDTO.class, Map.of(),
                "201", feeRuleFields, false, Set.of(), Set.of(),
                List.of("criação", "listagem", "regra repetida")
        ));
        assertOperation(document, new Op(
                "put", byId,
                Set.of("200", "400", "401", "403", "404", "409"),
                true, UpdateFeeRuleRequestDTO.class, Map.of(),
                "200", feeRuleFields, false, Set.of(), Set.of(),
                List.of("criação", "listagem", "regra repetida")
        ));
        assertOperation(document, new Op(
                "delete", byId,
                Set.of("204", "400", "401", "403", "404"),
                true, null, Map.of(),
                null, List.of(), false, Set.of("204"), Set.of(), idOrigin
        ));

        assertNoMappingAnnotations(
                FeeRuleController.class,
                Operation.class,
                GetMapping.class,
                PostMapping.class,
                PutMapping.class,
                DeleteMapping.class,
                RequestMapping.class
        );
    }

    @Test
    void transactionEndpointsDocumentTheResponseTable() throws Exception {
        JsonNode document = apiDocs();
        List<String> transactionFields = List.of(
                "id", "merchantId", "externalReference", "amount", "expectedNetAmount",
                "paymentMethod", "installments", "status", "transactionDate", "createdAt", "updatedAt");
        List<String> idOrigin = List.of("criação", "listagem");
        String list = "/api/merchants/{merchantId}/transactions";
        String byId = "/api/merchants/{merchantId}/transactions/{id}";
        String status = "/api/merchants/{merchantId}/transactions/{id}/status";

        assertOperation(document, new Op(
                "get", list,
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", transactionFields, true, Set.of(), Set.of(), idOrigin
        ));
        assertQueryExample(document, "get", list, "page", "0");
        assertQueryExample(document, "get", list, "size", "20");
        assertQueryExample(document, "get", list, "sort", "transactionDate,desc");
        assertQueryExample(document, "get", list, "status", "APPROVED");
        assertQueryExample(document, "get", list, "paymentMethod", "CREDIT_CARD");
        assertQueryExample(document, "get", list, "fromDate", "2026-07-01");
        assertQueryExample(document, "get", list, "toDate", "2026-07-31");
        assertOperation(document, new Op(
                "get", byId,
                Set.of("200", "400", "401", "403", "404"),
                true, null, Map.of(),
                "200", transactionFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertOperation(document, new Op(
                "post", list,
                Set.of("201", "400", "401", "403", "404", "409"),
                true, CreateTransactionRequestDTO.class, Map.of(),
                "201", transactionFields, false, Set.of(), Set.of(), idOrigin
        ));
        assertResponseDescriptionContains(document, "post", list, "409", "referência repetida", "taxa ativa ausente");
        assertOperation(document, new Op(
                "patch", status,
                Set.of("200", "400", "401", "403", "404"),
                true, UpdateTransactionStatusRequestDTO.class, Map.of(),
                "200", transactionFields, false, Set.of(), Set.of(), idOrigin
        ));

        assertNoMappingAnnotations(
                TransactionController.class,
                Operation.class,
                GetMapping.class,
                PostMapping.class,
                PutMapping.class,
                PatchMapping.class,
                DeleteMapping.class,
                RequestMapping.class
        );
    }

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private void assertOperation(JsonNode document, Op op) throws Exception {
        JsonNode operation = document.path("paths").path(op.path).path(op.method);
        assertThat(operation.isMissingNode())
                .as("%s %s", op.method, op.path)
                .isFalse();
        assertThat(operation.path("summary").asText()).containsPattern(PORTUGUESE);
        assertThat(operation.path("description").asText()).containsPattern(PORTUGUESE);
        for (String part : op.descriptionContains) {
            assertThat(operation.path("description").asText()).contains(part);
        }

        JsonNode responses = operation.path("responses");
        assertThat(fieldNames(responses)).containsExactlyInAnyOrderElementsOf(op.codes);
        for (String code : op.codes) {
            JsonNode response = responses.path(code);
            String description = response.path("description").asText();
            assertThat(description).as("%s %s %s", op.method, op.path, code).isNotBlank();
            assertThat(description).isNotEqualTo(HttpStatus.valueOf(Integer.parseInt(code)).getReasonPhrase());
            if (op.htmlCodes.contains(code)) {
                assertHtml(response);
            } else if (op.noSchemaCodes.contains(code)) {
                assertNoSchema(response);
            } else if (ERROR_CODES.containsKey(code)) {
                assertErrorExample(response, code);
            } else {
                assertThat(code).isEqualTo(op.successCode);
                assertSuccessExample(response, op);
            }
        }

        if (op.bearer) {
            assertBearer(operation);
        } else {
            JsonNode security = operation.get("security");
            assertThat(security).isNotNull();
            assertThat(security.isArray()).isTrue();
            assertThat(security).isEmpty();
        }

        if (op.requestType != null) {
            JsonNode example = requestExample(operation);
            assertValidRequest(example, op.requestType);
            op.requestFields.forEach((name, value) ->
                    assertThat(example.path(name).asText()).isEqualTo(value));
        } else {
            assertThat(operation.has("requestBody")).isFalse();
        }

        JsonNode parameters = operation.get("parameters");
        if (parameters != null) {
            for (JsonNode parameter : parameters) {
                String example = parameterExample(parameter);
                if ("path".equals(parameter.path("in").asText())) {
                    assertThat(example).isEqualTo(UUID_EXAMPLE);
                } else if ("query".equals(parameter.path("in").asText())) {
                    assertThat(example).as(parameter.path("name").asText()).isNotBlank();
                }
            }
        }
    }

    private void assertHtml(JsonNode response) {
        JsonNode content = response.path("content");
        assertThat(content.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactly("text/html");
        assertThat(content.path("text/html").toString()).doesNotContain("StandardError");
    }

    private void assertNoSchema(JsonNode response) {
        JsonNode content = response.get("content");
        if (content == null || content.isNull() || content.isEmpty()) {
            return;
        }
        content.properties().forEach(entry ->
                assertThat(entry.getValue().has("schema"))
                        .as(entry.getKey())
                        .isFalse());
    }

    private void assertErrorExample(JsonNode response, String code) throws Exception {
        JsonNode media = response.path("content").path("application/json");
        assertThat(media.isMissingNode()).isFalse();
        List<JsonNode> examples = examples(media);
        assertThat(examples).isNotEmpty();
        for (JsonNode example : examples) {
            assertThat(example.path("status").asInt()).isEqualTo(Integer.parseInt(code));
            assertThat(example.path("error").asText()).isEqualTo(ERROR_CODES.get(code));
        }
    }

    private void assertSuccessExample(JsonNode response, Op op) throws Exception {
        JsonNode media = response.path("content").path("application/json");
        List<JsonNode> examples = examples(media);
        assertThat(examples).isNotEmpty();
        JsonNode example = examples.getFirst();
        JsonNode target = op.page ? example.path("content").path(0) : example;
        for (String field : op.successFields) {
            assertThat(target.has(field)).as(field).isTrue();
            assertThat(target.get(field).isNull()).isFalse();
        }
    }

    private void assertBearer(JsonNode operation) {
        JsonNode security = operation.path("security");
        assertThat(security.isArray()).isTrue();
        assertThat(security).isNotEmpty();
        boolean found = false;
        for (JsonNode requirement : security) {
            if (requirement.has(BEARER)) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    private JsonNode requestExample(JsonNode operation) throws Exception {
        JsonNode media = operation.path("requestBody").path("content").path("application/json");
        List<JsonNode> examples = examples(media);
        assertThat(examples).hasSize(1);
        return examples.getFirst();
    }

    private void assertValidRequest(JsonNode example, Class<?> type) {
        for (var component : type.getRecordComponents()) {
            if (isRequired(type, component.getName())) {
                assertThat(example.has(component.getName())).as(component.getName()).isTrue();
                assertThat(example.get(component.getName()).isNull()).isFalse();
            }
        }
        Object dto = objectMapper.convertValue(example, type);
        assertThat(validator.validate(dto)).isEmpty();
    }

    private static boolean isRequired(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            for (Annotation annotation : field.getAnnotations()) {
                String simple = annotation.annotationType().getSimpleName();
                if (simple.equals("NotNull") || simple.equals("NotBlank") || simple.equals("NotEmpty")) {
                    return true;
                }
            }
            return false;
        } catch (NoSuchFieldException exception) {
            return false;
        }
    }

    private List<JsonNode> examples(JsonNode media) throws Exception {
        List<JsonNode> values = new ArrayList<>();
        if (media.has("example")) {
            values.add(asTree(media.get("example")));
        }
        JsonNode examples = media.get("examples");
        if (examples != null && examples.isObject()) {
            Iterator<JsonNode> iterator = examples.values();
            while (iterator.hasNext()) {
                values.add(asTree(iterator.next().path("value")));
            }
        }
        return values;
    }

    private JsonNode asTree(JsonNode node) throws Exception {
        if (node != null && node.isTextual()) {
            return objectMapper.readTree(node.asText());
        }
        return node;
    }

    private void assertResponseDescriptionContains(
            JsonNode document, String method, String path, String code, String... parts) {
        String description = document.path("paths").path(path).path(method)
                .path("responses").path(code).path("description").asText();
        for (String part : parts) {
            assertThat(description).as("%s %s %s", method, path, code).contains(part);
        }
    }

    private void assertQueryExample(JsonNode document, String method, String path, String name, String expected) {
        JsonNode parameters = document.path("paths").path(path).path(method).path("parameters");
        List<String> actual = new ArrayList<>();
        for (JsonNode parameter : parameters) {
            if (name.equals(parameter.path("name").asText()) && "query".equals(parameter.path("in").asText())) {
                actual.add(parameterExample(parameter));
            }
        }
        assertThat(actual).as("%s %s %s", method, path, name).containsExactly(expected);
    }

    private static String parameterExample(JsonNode parameter) {
        if (parameter.hasNonNull("example")) {
            return parameter.get("example").asText();
        }
        JsonNode schemaExample = parameter.path("schema").get("example");
        if (schemaExample != null && !schemaExample.isNull()) {
            return schemaExample.asText();
        }
        return null;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.properties().forEach(entry -> names.add(entry.getKey()));
        return names;
    }

    @SafeVarargs
    private static void assertNoMappingAnnotations(Class<?> controller, Class<? extends Annotation>... forbidden) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isSynthetic()) {
                continue;
            }
            for (Class<? extends Annotation> annotation : forbidden) {
                assertThat(method.isAnnotationPresent(annotation))
                        .as("%s#%s @%s", controller.getSimpleName(), method.getName(), annotation.getSimpleName())
                        .isFalse();
            }
        }
    }

    private record Op(
            String method,
            String path,
            Set<String> codes,
            boolean bearer,
            Class<?> requestType,
            Map<String, String> requestFields,
            String successCode,
            List<String> successFields,
            boolean page,
            Set<String> noSchemaCodes,
            Set<String> htmlCodes,
            List<String> descriptionContains
    ) {
    }
}
