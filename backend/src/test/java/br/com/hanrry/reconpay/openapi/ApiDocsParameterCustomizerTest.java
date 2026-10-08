package br.com.hanrry.reconpay.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiDocsParameterCustomizerTest {

    private static final String UUID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    private final ApiDocsParameterCustomizer customizer = new ApiDocsParameterCustomizer();

    @Test
    void fillsPageSizeAndSortFromTheFixedDefaultsAndPageableDefault() throws Exception {
        Operation operation = operation(
                query("page"),
                query("size"),
                query("sort")
        );

        customizer.customize(operation, handler("sorted", Pageable.class));

        assertThat(example(operation, "page")).isEqualTo("0");
        assertThat(example(operation, "size")).isEqualTo("20");
        assertThat(example(operation, "sort")).isEqualTo("movementDate,desc");
    }

    @Test
    void leavesSortWithoutExampleWhenPageableDefaultHasNoSort() throws Exception {
        Operation operation = operation(query("page"), query("size"), query("sort"));

        customizer.customize(operation, handler("unsorted", Pageable.class));

        assertThat(example(operation, "page")).isEqualTo("0");
        assertThat(example(operation, "size")).isEqualTo("20");
        assertThat(example(operation, "sort")).isNull();
    }

    @Test
    void leavesSortWithoutExampleWhenMethodHasNoPageableDefault() throws Exception {
        Operation operation = operation(query("page"), query("size"), query("sort"));

        customizer.customize(operation, handler("plain", Pageable.class));

        assertThat(example(operation, "page")).isEqualTo("0");
        assertThat(example(operation, "size")).isEqualTo("20");
        assertThat(example(operation, "sort")).isNull();
    }

    @Test
    void fillsMissingUuidPathExampleAndLeavesOtherParametersAlone() throws Exception {
        Operation operation = operation(
                uuidPath("merchantId"),
                new Parameter().name("code").in("path").schema(new StringSchema()),
                query("layout")
        );

        customizer.customize(operation, handler("plain", Pageable.class));

        assertThat(example(operation, "merchantId")).isEqualTo(UUID_EXAMPLE);
        assertThat(example(operation, "code")).isNull();
        assertThat(example(operation, "layout")).isNull();
    }

    @Test
    void keepsExamplesThatAreAlreadySet() throws Exception {
        Parameter page = query("page").example("4");
        Parameter size = query("size").example("50");
        Parameter sort = query("sort").example("name,asc");
        Parameter id = uuidPath("id").example("11111111-1111-1111-1111-111111111111");
        Operation operation = operation(page, size, sort, id);

        customizer.customize(operation, handler("sorted", Pageable.class));

        assertThat(example(operation, "page")).isEqualTo("4");
        assertThat(example(operation, "size")).isEqualTo("50");
        assertThat(example(operation, "sort")).isEqualTo("name,asc");
        assertThat(example(operation, "id")).isEqualTo("11111111-1111-1111-1111-111111111111");

        Parameter pageOnSchema = query("page");
        pageOnSchema.setSchema(new StringSchema().example("9"));
        Operation schemaExample = operation(pageOnSchema);
        customizer.customize(schemaExample, handler("sorted", Pageable.class));

        assertThat(example(schemaExample, "page")).isNull();
        assertThat(schemaExample.getParameters().getFirst().getSchema().getExample()).isEqualTo("9");
    }

    private static Operation operation(Parameter... parameters) {
        return new Operation().parameters(List.of(parameters));
    }

    private static Parameter query(String name) {
        return new Parameter().name(name).in("query");
    }

    private static Parameter uuidPath(String name) {
        return new Parameter().name(name).in("path").schema(new StringSchema().format("uuid"));
    }

    private static Object example(Operation operation, String name) {
        return operation.getParameters().stream()
                .filter(parameter -> name.equals(parameter.getName()))
                .findFirst()
                .orElseThrow()
                .getExample();
    }

    private static HandlerMethod handler(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = Samples.class.getDeclaredMethod(name, parameterTypes);
        return new HandlerMethod(new Samples(), method);
    }

    static final class Samples {

        @SuppressWarnings("unused")
        public void sorted(
                @PageableDefault(page = 2, size = 50, sort = "movementDate", direction = Sort.Direction.DESC)
                Pageable pageable) {
        }

        @SuppressWarnings("unused")
        public void unsorted(@PageableDefault(size = 20) Pageable pageable) {
        }

        @SuppressWarnings("unused")
        public void plain(Pageable pageable) {
        }
    }
}
