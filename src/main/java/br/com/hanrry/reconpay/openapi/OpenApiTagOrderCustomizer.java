package br.com.hanrry.reconpay.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class OpenApiTagOrderCustomizer implements GlobalOpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        openApi.setTags(new ArrayList<>(OpenApiTags.ordered()));
    }
}
