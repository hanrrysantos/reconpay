package br.com.hanrry.reconpay.merchant.exception.handler;

import br.com.hanrry.reconpay.merchant.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.merchant.exception.MerchantNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MerchantAlreadyExistsException.class)
    public ResponseEntity<StandardError> handlerMerchantAlreadyExistsException(MerchantAlreadyExistsException ex, HttpServletRequest request){
        String error = "MerchantAlreadyExistsException";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<StandardError> handlerMerchantNotFoundException(MerchantNotFoundException ex, HttpServletRequest request){
        String error = "MerchantNotFoundException";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handlerMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request){
        String error = "MethodArgumentNotValidException";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handlerAllException(Exception ex, HttpServletRequest request){
        String error = "Exception";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}
