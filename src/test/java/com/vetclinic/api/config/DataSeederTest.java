package com.vetclinic.api.config;

import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o seed nunca cria as contas de demonstração (VET/RECEPTIONIST)
 * com senha se {@code app.seed.demo-password} não for definida — caso contrário
 * um deploy real nasceria com credenciais conhecidas e públicas.
 */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "app.seed.admin-password=admin-forte-de-teste-123",
        "app.seed.demo-password=",
        // Banco isolado: o seed comita usuários, então não pode compartilhar o
        // H2 em memória com os demais testes de integração.
        "spring.datasource.url=jdbc:h2:mem:seedtest;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DataSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void semDemoPasswordApenasOAdminEhCriado() {
        assertThat(userRepository.findAll())
                .extracting(u -> u.getRole())
                .containsExactly(Role.ADMIN);
    }
}
