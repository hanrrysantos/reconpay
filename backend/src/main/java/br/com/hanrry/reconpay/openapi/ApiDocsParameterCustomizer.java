package br.com.hanrry.reconpay.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Locale;

@Component
public class ApiDocsParameterCustomizer implements OperationCustomizer {

    static final String UUID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation == null || operation.getParameters() == null) {
            return operation;
        }
        String sortExample = sortExample(handlerMethod);
        for (Parameter parameter : operation.getParameters()) {
            if (hasExample(parameter)) {
                continue;
            }
            String name = parameter.getName();
            if ("page".equals(name)) {
                parameter.setExample("0");
            } else if ("size".equals(name)) {
                parameter.setExample("20");
            } else if ("sort".equals(name)) {
                if (sortExample != null) {
                    parameter.setExample(sortExample);
                }
            } else if (isUuidPath(parameter)) {
                parameter.setExample(UUID_EXAMPLE);
            }
        }
        return operation;
    }

    private static String sortExample(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }
        for (var methodParameter : handlerMethod.getMethodParameters()) {
            PageableDefault defaults = methodParameter.getParameterAnnotation(PageableDefault.class);
            if (defaults == null || defaults.sort().length == 0) {
                continue;
            }
            String direction = defaults.direction().name().toLowerCase(Locale.ROOT);
            return defaults.sort()[0] + "," + direction;
        }
        return null;
    }

    private static boolean hasExample(Parameter parameter) {
        if (parameter.getExample() != null) {
            return true;
        }
        if (parameter.getExamples() != null && !parameter.getExamples().isEmpty()) {
            return true;
        }
        Schema<?> schema = parameter.getSchema();
        return schema != null && schema.getExample() != null;
    }

    private static boolean isUuidPath(Parameter parameter) {
        if (!"path".equals(parameter.getIn())) {
            return false;
        }
        Schema<?> schema = parameter.getSchema();
        if (schema == null || !"uuid".equalsIgnoreCase(schema.getFormat())) {
            return false;
        }
        if ("string".equals(schema.getType())) {
            return true;
        }
        return schema.getTypes() != null && schema.getTypes().contains("string");
    }
}
