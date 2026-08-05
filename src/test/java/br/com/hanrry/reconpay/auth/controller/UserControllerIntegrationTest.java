package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.service.UserService;
import br.com.hanrry.reconpay.config.SecurityConfig;
import br.com.hanrry.reconpay.exception.EmailAlreadyExistsException;
import br.com.hanrry.reconpay.exception.UserNotFoundException;
import br.com.hanrry.reconpay.exception.handler.GlobalExceptionHandler;
import br.com.hanrry.reconpay.security.JwtAuthenticationFilter;
import br.com.hanrry.reconpay.security.RestAccessDeniedHandler;
import br.com.hanrry.reconpay.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void configureJwtFilterPassThrough() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUserShouldReturnCreatedWhenAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponseDTO response = buildUserResponse(userId, UserRole.ADMIN, "novo-admin@test.local");
        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Admin",
                                  "email": "novo-admin@test.local",
                                  "password": "Admin@456",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "FINANCIAL_ANALYST")
    void createUserShouldReturnForbiddenWhenAnalyst() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Admin",
                                  "email": "novo-admin@test.local",
                                  "password": "Admin@456",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(userService, never()).createUser(any());
    }

    @Test
    void createUserShouldReturnUnauthorizedWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Admin",
                                  "email": "novo-admin@test.local",
                                  "password": "Admin@456",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token inválido ou ausente"));

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUserShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": "weak",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(userService, never()).createUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUserShouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new EmailAlreadyExistsException("Email já cadastrado: novo-admin@test.local"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Novo Admin",
                                  "email": "novo-admin@test.local",
                                  "password": "Admin@456",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllUsersShouldReturnPaginatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponseDTO user = buildUserResponse(userId, UserRole.ADMIN, "admin@test.local");
        when(userService.findAllUsers(any())).thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByIdShouldReturnUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponseDTO user = buildUserResponse(userId, UserRole.ADMIN, "admin@test.local");
        when(userService.findById(userId)).thenReturn(user);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByIdShouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId))
                .thenThrow(new UserNotFoundException("Usuário não encontrado com id: " + userId));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByEmailShouldReturnUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponseDTO user = buildUserResponse(userId, UserRole.ADMIN, "admin@test.local");
        when(userService.findByEmail("admin@test.local")).thenReturn(user);

        mockMvc.perform(get("/api/users/email").param("email", "admin@test.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.local"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNameShouldReturnUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponseDTO user = buildUserResponse(userId, UserRole.ADMIN, "admin@test.local");
        user = new UserResponseDTO(userId, "Nome Atualizado", "admin@test.local", UserRole.ADMIN, true, user.createdAt());
        when(userService.updateName(eq(userId), any())).thenReturn(user);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nome Atualizado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome Atualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNameShouldReturnBadRequestWhenNameIsBlank() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(userService, never()).updateName(eq(userId), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteShouldReturnNoContent() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteById(userId);
    }

    private UserResponseDTO buildUserResponse(UUID id, UserRole role, String email) {
        return new UserResponseDTO(
                id,
                "Usuario",
                email,
                role,
                true,
                Instant.parse("2026-08-05T12:00:00Z")
        );
    }
}
