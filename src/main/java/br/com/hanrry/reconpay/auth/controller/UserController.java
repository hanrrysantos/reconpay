package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAllUsers() {
        List<UserResponseDTO> users = userService.findAllUsers();

        return ResponseEntity.ok().body(users);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<UserResponseDTO> findUserById(
            @PathVariable UUID id) {
        UserResponseDTO user = userService.findUserById(id);

        return ResponseEntity.ok().body(user);
    }

    @GetMapping(value = "/email")
    public ResponseEntity<UserResponseDTO> findUserByEmail(
            @RequestBody String email) {
        UserResponseDTO user = userService.findUserByEmail(email);

        return ResponseEntity.ok().body(user);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<UserResponseDTO> updateUserNameById(
            @PathVariable UUID id, @RequestBody UpdateUserRequestDTO request) {
        UserResponseDTO user = userService.updateUserNameById(id, request);

        return ResponseEntity.ok().body(user);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteUserById(@PathVariable UUID id) {
        userService.deleteUserById(id);

        return ResponseEntity.noContent().build();
    }
}
