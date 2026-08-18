package com.vetclinic.api.config;

import com.vetclinic.api.client.Client;
import com.vetclinic.api.client.ClientRepository;
import com.vetclinic.api.pet.Pet;
import com.vetclinic.api.pet.PetRepository;
import com.vetclinic.api.service.ClinicService;
import com.vetclinic.api.service.ClinicServiceRepository;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import com.vetclinic.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Popula o banco com dados iniciais (usuários de exemplo, um cliente/pet e
 * serviços do catálogo) para facilitar testar a API logo após subir a aplicação.
 *
 * Controlado por `app.seed.enabled` (true por padrão em dev, false em prod).
 * É idempotente: só roda se não houver nenhum usuário cadastrado ainda.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PetRepository petRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin-email:admin@vetclinic.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        log.info("Nenhum usuário encontrado — populando banco com dados iniciais de demonstração...");

        User admin = userRepository.save(User.builder()
                .name("Administrador")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build());

        User vet = userRepository.save(User.builder()
                .name("Dra. Ana Souza")
                .email("vet@vetclinic.com")
                .passwordHash(passwordEncoder.encode("vet12345"))
                .role(Role.VET)
                .build());

        userRepository.save(User.builder()
                .name("Carlos Recepção")
                .email("recepcao@vetclinic.com")
                .passwordHash(passwordEncoder.encode("recep1234"))
                .role(Role.RECEPTIONIST)
                .build());

        Client tutor = clientRepository.save(Client.builder()
                .name("Pedro Linard")
                .email("tutor.demo@vetclinic.com")
                .phone("(85) 99999-0000")
                .address("Fortaleza, CE")
                .build());

        petRepository.save(Pet.builder()
                .name("Rex")
                .species("Cachorro")
                .breed("Labrador")
                .birthDate(LocalDate.now().minusYears(2))
                .weightKg(28.5)
                .client(tutor)
                .build());

        clinicServiceRepository.save(ClinicService.builder()
                .name("Consulta de rotina")
                .description("Avaliação clínica geral do animal")
                .priceCents(15000)
                .durationMin(30)
                .build());

        clinicServiceRepository.save(ClinicService.builder()
                .name("Vacinação")
                .description("Aplicação de vacina (valor não inclui a dose)")
                .priceCents(5000)
                .durationMin(15)
                .build());

        log.info("Seed concluído. Login de admin: {} / senha: {}", adminEmail, adminPassword);
        log.info("Login de veterinário de exemplo: {} / senha: vet12345", vet.getEmail());
    }
}
