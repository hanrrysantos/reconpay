package br.com.hanrry.reconpay.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class DuplicateExternalSettlementException extends RuntimeException {

    private final List<String> conflictingReferences;

    public DuplicateExternalSettlementException(String message, List<String> conflictingReferences) {
        super(message);
        this.conflictingReferences = List.copyOf(conflictingReferences);
    }
}
