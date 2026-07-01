package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.service.UserService;
import br.com.hanrry.reconpay.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping(value = "/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid
            @RequestBody AuthRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtService.generateToken(request.email());

        return ResponseEntity.ok().body(new AuthResponseDTO(token));
    }

    @PostMapping(value = "/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid
            @RequestBody UserRequestDTO request) {
        UserResponseDTO user = userService.register(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(user.id()).toUri();

        return ResponseEntity.created(uri).body(user);
    }
}
