package com.vetclinic.api.security;

import com.vetclinic.api.AbstractIntegrationTest;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import com.vetclinic.api.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que um JWT emitido antes de uma mudança de papel/senha do usuário
 * pare de valer na hora (ver {@code User.tokensInvalidBefore}).
 */
class TokenInvalidationTest extends AbstractIntegrationTest {

    @Test
    void tokenDeixaDeValerQuandoOAdminMudaOPapelDoUsuario() throws Exception {
        User admin = createUser(Role.ADMIN);
        User vet = createUser(Role.VET);

        String vetToken = tokenFor(vet);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isOk());

        UpdateUserRequest change = new UpdateUserRequest(vet.getName(), vet.getEmail(), Role.RECEPTIONIST);
        mockMvc.perform(put("/api/users/" + vet.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(change)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + vetToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trocaDeSenhaInvalidaOTokenAtual() throws Exception {
        User user = createUser(Role.RECEPTIONIST);
        String token = tokenFor(user);

        String body = """
                {"currentPassword":"senha123","newPassword":"nova-senha-forte-123"}
                """;
        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
