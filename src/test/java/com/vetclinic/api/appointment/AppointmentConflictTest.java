package com.vetclinic.api.appointment;

import com.vetclinic.api.AbstractIntegrationTest;
import com.vetclinic.api.appointment.dto.AppointmentRequest;
import com.vetclinic.api.client.Client;
import com.vetclinic.api.client.ClientRepository;
import com.vetclinic.api.pet.Pet;
import com.vetclinic.api.pet.PetRepository;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante a regra de negócio central do módulo de agendamento: um mesmo
 * veterinário não pode ter duas consultas com horários sobrepostos.
 */
class AppointmentConflictTest extends AbstractIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PetRepository petRepository;

    private User admin;
    private User vet;
    private Pet pet;
    private Pet otherPet;

    @BeforeEach
    void setUp() {
        admin = createUser(Role.ADMIN);
        vet = createUser(Role.VET);

        Client client = clientRepository.save(Client.builder()
                .name("Tutor de Teste")
                .phone("(85) 90000-1111")
                .build());

        pet = petRepository.save(Pet.builder()
                .name("Bidu")
                .species("Cachorro")
                .client(client)
                .build());

        otherPet = petRepository.save(Pet.builder()
                .name("Mimi")
                .species("Gato")
                .client(client)
                .build());
    }

    @Test
    void agendarDuasConsultasComOMesmoVetEmHorariosSobrepostosRetorna409() throws Exception {
        String token = tokenFor(admin);
        LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);

        AppointmentRequest first = new AppointmentRequest(start, 30, "Consulta de rotina", pet.getId(), vet.getId(), null);
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Começa 15 minutos depois do início da primeira consulta (que dura 30min) -> sobrepõe.
        AppointmentRequest overlapping = new AppointmentRequest(
                start.plusMinutes(15), 30, "Outra consulta", otherPet.getId(), vet.getId(), null
        );

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapping)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito"));
    }

    @Test
    void agendarConsultasComOMesmoVetEmHorariosDiferentesFunciona() throws Exception {
        String token = tokenFor(admin);
        LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);

        AppointmentRequest first = new AppointmentRequest(start, 30, "Consulta 1", pet.getId(), vet.getId(), null);
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Começa exatamente quando a primeira termina -> não sobrepõe.
        AppointmentRequest sequential = new AppointmentRequest(
                start.plusMinutes(30), 30, "Consulta 2", otherPet.getId(), vet.getId(), null
        );

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sequential)))
                .andExpect(status().isCreated());
    }

    @Test
    void agendarComUsuarioQueNaoEVeterinarioRetorna400() throws Exception {
        String token = tokenFor(admin);
        LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);

        // "admin" tem role ADMIN, não VET.
        AppointmentRequest request = new AppointmentRequest(start, 30, "Inválida", pet.getId(), admin.getId(), null);

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
