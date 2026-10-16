package br.com.hanrry.reconpay.exception;

import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class TransactionImportValidationException extends RuntimeException {

    private final List<ImportRowErrorDTO> rowErrors;

    public TransactionImportValidationException(String message, List<ImportRowErrorDTO> rowErrors) {
        super(message);
        this.rowErrors = List.copyOf(rowErrors);
    }
}
