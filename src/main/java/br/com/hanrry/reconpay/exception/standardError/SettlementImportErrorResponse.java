package br.com.hanrry.reconpay.exception.standardError;

import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

public record SettlementImportErrorResponse(

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<ImportRowErrorDTO> rowErrors
) {
}
