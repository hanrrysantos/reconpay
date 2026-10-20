package br.com.hanrry.reconpay.demo.controller;

import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.demo.service.DemoAccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final DemoAccountService demoAccountService;

    // The remote address is the visitor's; behind the proxy, Tomcat resolves it from X-Forwarded-For.
    @PostMapping("/api/auth/demo")
    public ResponseEntity<AuthResponseDTO> create(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demoAccountService.create(request.getRemoteAddr()));
    }
}
