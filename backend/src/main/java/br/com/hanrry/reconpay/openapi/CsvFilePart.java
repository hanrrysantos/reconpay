package br.com.hanrry.reconpay.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Arquivo CSV enviado na parte file")
public class CsvFilePart {

    @Schema(
            type = "string",
            format = "binary",
            description = "Arquivo CSV. O Try it out não pré-carrega o arquivo; use o CSV mínimo da descrição.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    public String file;
}
