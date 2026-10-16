package br.com.hanrry.reconpay.security.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * MerchantAccessAspect checks access only for a controller parameter named merchantId, so a route that
 * renames it, or a new route that forgets it, silently lets any operator read another merchant. These
 * tests read every route from Spring itself, so a route added later is covered without editing them.
 */
@AutoConfigureMockMvc
class MerchantRouteAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String MERCHANT_SCOPED_PREFIX = "/api/merchants/{merchantId}";
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}]+)}");

    @Autowired MockMvc mockMvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping;

    private record Route(RequestMethod method, String pattern, Method handler) {
    }

    private List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            for (String pattern : entry.getKey().getPatternValues()) {
                for (RequestMethod method : entry.getKey().getMethodsCondition().getMethods()) {
                    routes.add(new Route(method, pattern, entry.getValue().getMethod()));
                }
            }
        }
        return routes;
    }

    @Test
    void everyMerchantScopedRouteHandsTheAspectAUuidParameterNamedMerchantId() {
        List<Route> scoped = routes().stream().filter(route -> route.pattern().startsWith(MERCHANT_SCOPED_PREFIX)).toList();

        assertThat(scoped).extracting(Route::pattern)
                .as("the enumeration found the merchant-scoped routes")
                .contains(
                        "/api/merchants/{merchantId}/transactions/imports/{importId}",
                        "/api/merchants/{merchantId}/fee-rules/{id}",
                        "/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}",
                        "/api/merchants/{merchantId}/periods/summary",
                        "/api/merchants/{merchantId}/bank-statements/import");

        StandardReflectionParameterNameDiscoverer names = new StandardReflectionParameterNameDiscoverer();
        List<String> unguarded = scoped.stream().filter(route -> {
            String[] parameterNames = names.getParameterNames(route.handler());
            Class<?>[] types = route.handler().getParameterTypes();
            for (int index = 0; parameterNames != null && index < parameterNames.length; index++) {
                if ("merchantId".equals(parameterNames[index]) && types[index] == UUID.class) {
                    return false;
                }
            }
            return true;
        }).map(route -> route.method() + " " + route.pattern() + " -> " + route.handler()).toList();

        assertThat(unguarded).isEmpty();
    }

    @Test
    void everyGetRouteOfAMerchantAnswersForbiddenToAnOperatorWithoutAccess() throws Exception {
        String adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        String operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        String foreignMerchant = JsonPath.read(mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Not granted\", \"document\": \"%s\"}".formatted(TestCnpj.random())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        List<Route> gets = routes().stream()
                .filter(route -> route.method() == RequestMethod.GET && route.pattern().startsWith("/api/merchants/{"))
                .toList();
        assertThat(gets).extracting(Route::pattern)
                .contains("/api/merchants/{id}", "/api/merchants/{merchantId}/transactions/imports/{importId}");

        List<String> notForbidden = new ArrayList<>();
        for (Route route : gets) {
            String path = expand(route.pattern(), foreignMerchant);
            int status = mockMvc.perform(get(path)
                            .header("Authorization", "Bearer " + operatorToken)
                            .param("fromDate", "2026-07-01")
                            .param("toDate", "2026-07-31"))
                    .andReturn().getResponse().getStatus();
            if (status != 403) {
                notForbidden.add(route.pattern() + " -> " + status);
            }
        }

        assertThat(notForbidden).isEmpty();
    }

    /** The first path variable is the merchant; any other one gets a random id. */
    private static String expand(String pattern, String merchantId) {
        Matcher matcher = PATH_VARIABLE.matcher(pattern);
        StringBuilder path = new StringBuilder();
        boolean first = true;
        while (matcher.find()) {
            matcher.appendReplacement(path, first ? merchantId : UUID.randomUUID().toString());
            first = false;
        }
        matcher.appendTail(path);
        return path.toString();
    }
}
