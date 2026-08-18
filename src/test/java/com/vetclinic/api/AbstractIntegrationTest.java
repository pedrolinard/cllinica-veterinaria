package com.vetclinic.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import com.vetclinic.api.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para testes de integração: sobe o contexto Spring completo com o
 * perfil "test" (H2 em memória) e disponibiliza um helper para autenticar
 * um usuário de um determinado papel e obter o Bearer token correspondente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User createUser(Role role) {
        String uniqueEmail = role.name().toLowerCase() + "." + UUID.randomUUID() + "@vetclinic.com";
        return userRepository.save(User.builder()
                .name("Usuário " + role)
                .email(uniqueEmail)
                .passwordHash(passwordEncoder.encode("senha123"))
                .role(role)
                .build());
    }

    protected String tokenFor(User user) throws Exception {
        String body = """
                {"email":"%s","password":"senha123"}
                """.formatted(user.getEmail());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
