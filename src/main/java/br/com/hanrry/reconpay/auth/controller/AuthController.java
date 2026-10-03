package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.dto.VerifyEmailRequestDTO;
import br.com.hanrry.reconpay.auth.openapi.AuthControllerApi;
import br.com.hanrry.reconpay.auth.email.EmailVerificationPages;
import br.com.hanrry.reconpay.auth.service.AuthService;
import br.com.hanrry.reconpay.exception.InvalidEmailVerificationTokenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponseDTO> login(@Valid AuthRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    public ResponseEntity<UserResponseDTO> register(@Valid UserRequestDTO request) {
        UserResponseDTO user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Override
    public ResponseEntity<Void> verifyEmail(@Valid VerifyEmailRequestDTO request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<String> verifyEmailFromLink(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(EmailVerificationPages.SUCCESS);
        } catch (InvalidEmailVerificationTokenException ex) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(EmailVerificationPages.INVALID);
        }
    }
}
