package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.CreateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.MerchantAccessRequestDTO;
import br.com.hanrry.reconpay.auth.dto.MerchantAccessResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.openapi.UserControllerApi;
import br.com.hanrry.reconpay.auth.service.UserMerchantAccessService;
import br.com.hanrry.reconpay.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

    private final UserService userService;
    private final UserMerchantAccessService userMerchantAccessService;

    @Override
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO user = userService.createUser(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.id())
                .toUri();
        return ResponseEntity.created(uri).body(user);
    }

    @Override
    public ResponseEntity<Page<UserResponseDTO>> findAllUsers(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.findAllUsers(pageable));
    }

    @Override
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Override
    public ResponseEntity<UserResponseDTO> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    @Override
    public ResponseEntity<UserResponseDTO> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.activate(id));
    }

    @Override
    public ResponseEntity<MerchantAccessResponseDTO> findMerchantAccess(@PathVariable UUID id) {
        return ResponseEntity.ok(userMerchantAccessService.findByUser(id));
    }

    @Override
    public ResponseEntity<MerchantAccessResponseDTO> replaceMerchantAccess(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantAccessRequestDTO request) {
        return ResponseEntity.ok(userMerchantAccessService.replace(id, request));
    }

    @Override
    public ResponseEntity<UserResponseDTO> updateName(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequestDTO request) {
        return ResponseEntity.ok(userService.updateName(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
