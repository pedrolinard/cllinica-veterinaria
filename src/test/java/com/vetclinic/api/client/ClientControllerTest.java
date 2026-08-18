package com.vetclinic.api.client;

import com.vetclinic.api.AbstractIntegrationTest;
import com.vetclinic.api.client.dto.ClientRequest;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientControllerTest extends AbstractIntegrationTest {

    @Test
    void adminConsegueCriarEListarCliente() throws Exception {
        User admin = createUser(Role.ADMIN);
        String token = tokenFor(admin);

        ClientRequest request = new ClientRequest("Maria Tutora", "maria@email.com", "(85) 98888-7777", "Rua A, 123");

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Tutora"))
                .andExpect(jsonPath("$.petsCount").value(0));

        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void criarClienteSemTokenRetorna401() throws Exception {
        ClientRequest request = new ClientRequest("Sem Token", null, "(85) 90000-0000", null);

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void veterinarioNaoPodeCriarCliente() throws Exception {
        User vet = createUser(Role.VET);
        String token = tokenFor(vet);

        ClientRequest request = new ClientRequest("Não Deveria Criar", null, "(85) 90000-0000", null);

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void criarClienteComDadosInvalidosRetorna422() throws Exception {
        User admin = createUser(Role.ADMIN);
        String token = tokenFor(admin);

        // nome em branco e telefone ausente
        String invalidBody = """
                {"name":"","email":"invalido"}
                """;

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
