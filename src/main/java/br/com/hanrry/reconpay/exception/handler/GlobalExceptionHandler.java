package br.com.hanrry.reconpay.exception.handler;

import br.com.hanrry.reconpay.exception.*;
import br.com.hanrry.reconpay.exception.standardError.ApiErrorCode;
import br.com.hanrry.reconpay.exception.standardError.DuplicateExternalSettlementErrorResponse;
import br.com.hanrry.reconpay.exception.standardError.SettlementImportErrorResponse;
import br.com.hanrry.reconpay.exception.standardError.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            UserNotFoundException.class,
            MerchantNotFoundException.class,
            FeeRuleNotFoundException.class,
            TransactionNotFoundException.class,
            ExternalSettlementNotFoundException.class,
            SettlementImportNotFoundException.class
    })
    public ResponseEntity<StandardError> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<StandardError> handleAuthenticationFailure(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.UNAUTHORIZED,
                "Credenciais inválidas",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage(),
                request
        );
    }

    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            MerchantAlreadyExistsException.class,
            FeeRuleAlreadyExistsException.class,
            DuplicateExternalReferenceException.class,
            MissingActiveFeeRuleException.class
    })
    public ResponseEntity<StandardError> handleConflict(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateExternalSettlementException.class)
    public ResponseEntity<DuplicateExternalSettlementErrorResponse> handleDuplicateExternalSettlement(
            DuplicateExternalSettlementException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Requisição rejeitada | code={} | status={} | path={} | message={}",
                ApiErrorCode.CONFLICT,
                HttpStatus.CONFLICT.value(),
                request.getRequestURI(),
                ex.getMessage()
        );

        DuplicateExternalSettlementErrorResponse errorResponse = new DuplicateExternalSettlementErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                ApiErrorCode.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                ex.getConflictingReferences()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardError> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ApiErrorCode.VALIDATION_ERROR,
                "Arquivo CSV excede o tamanho máximo permitido de 5MB",
                request
        );
    }

    @ExceptionHandler({
            InvalidTransactionStatusTransitionException.class,
            InvalidInstallmentsForPaymentMethodException.class,
            InvalidSettlementImportException.class
    })
    public ResponseEntity<StandardError> handleBusinessValidation(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(SettlementImportValidationException.class)
    public ResponseEntity<SettlementImportErrorResponse> handleSettlementImportValidation(
            SettlementImportValidationException ex,
            HttpServletRequest request
    ) {
        log.debug(
                "Requisição rejeitada | code={} | status={} | path={} | message={}",
                ApiErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                ex.getMessage()
        );

        SettlementImportErrorResponse errorResponse = new SettlementImportErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                ApiErrorCode.VALIDATION_ERROR,
                ex.getMessage(),
                request.getRequestURI(),
                ex.getRowErrors()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Erro interno | code={} | path={} | message={}",
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                ex.getMessage(),
                ex
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor",
                request
        );
    }

    private ResponseEntity<StandardError> buildError(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        logHandledError(status, error, message, request.getRequestURI());

        StandardError standardError = new StandardError(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(standardError);
    }

    private void logHandledError(HttpStatus status, String errorCode, String message, String path) {
        String logMessage = "Requisição rejeitada | code={} | status={} | path={} | message={}";

        if (status.is5xxServerError()) {
            log.error(logMessage, errorCode, status.value(), path, message);
            return;
        }

        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.CONFLICT) {
            log.warn(logMessage, errorCode, status.value(), path, message);
            return;
        }

        log.debug(logMessage, errorCode, status.value(), path, message);
    }
}
