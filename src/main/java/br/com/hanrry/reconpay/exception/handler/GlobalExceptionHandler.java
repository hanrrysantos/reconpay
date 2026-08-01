package br.com.hanrry.reconpay.exception.handler;

import br.com.hanrry.reconpay.exception.*;
import br.com.hanrry.reconpay.exception.standardError.ApiErrorCode;
import br.com.hanrry.reconpay.exception.standardError.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            UserNotFoundException.class,
            MerchantNotFoundException.class,
            FeeRuleNotFoundException.class,
            TransactionNotFoundException.class
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
        return buildError(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "Credenciais inválidas", request);
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

    @ExceptionHandler({
            InvalidTransactionStatusTransitionException.class,
            InvalidInstallmentsForPaymentMethodException.class
    })
    public ResponseEntity<StandardError> handleBusinessValidation(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildError(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
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
        StandardError standardError = new StandardError(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(standardError);
    }
}
