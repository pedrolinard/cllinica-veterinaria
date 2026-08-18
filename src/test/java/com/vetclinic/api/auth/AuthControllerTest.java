package com.vetclinic.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetclinic.api.auth.dto.LoginRequest;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import com.vetclinic.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createUser(String email, String rawPassword, Role role) {
        return userRepository.save(User.builder()
                .name("Usuário de Teste")
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());
    }

    @Test
    void loginComCredenciaisValidasRetornaToken() throws Exception {
        createUser("admin.test@vetclinic.com", "senha123", Role.ADMIN);

        LoginRequest request = new LoginRequest("admin.test@vetclinic.com", "senha123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin.test@vetclinic.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void loginComSenhaInvalidaRetorna401() throws Exception {
        createUser("outro.test@vetclinic.com", "senhaCorreta", Role.RECEPTIONIST);

        LoginRequest request = new LoginRequest("outro.test@vetclinic.com", "senhaErrada");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meComTokenValidoRetornaPerfil() throws Exception {
        createUser("perfil.test@vetclinic.com", "senha123", Role.VET);

        LoginRequest loginRequest = new LoginRequest("perfil.test@vetclinic.com", "senha123");

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil.test@vetclinic.com"))
                .andExpect(jsonPath("$.role").value("VET"));
    }
}
