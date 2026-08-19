package com.vetclinic.api;

import com.vetclinic.api.appointment.Appointment;
import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.appointment.AppointmentStatus;
import com.vetclinic.api.client.Client;
import com.vetclinic.api.client.ClientRepository;
import com.vetclinic.api.medicalrecord.MedicalRecord;
import com.vetclinic.api.medicalrecord.MedicalRecordRepository;
import com.vetclinic.api.pet.Pet;
import com.vetclinic.api.pet.PetRepository;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão para o bug encontrado na revisão de 2026-08-19: excluir um pet, um
 * veterinário, um cliente ou uma consulta que ainda tem registros vinculados
 * derrubava a API com 500 (violação de FK não tratada). Agora deve responder 409
 * com uma mensagem explicando o vínculo, e o caminho feliz (sem vínculo) continua
 * funcionando normalmente.
 */
class DeletionConflictTest extends AbstractIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    private User admin;
    private User vet;
    private Client client;
    private Pet pet;

    @BeforeEach
    void setUp() {
        admin = createUser(Role.ADMIN);
        vet = createUser(Role.VET);

        client = clientRepository.save(Client.builder()
                .name("Tutor de Teste")
                .phone("(85) 90000-1111")
                .build());

        pet = petRepository.save(Pet.builder()
                .name("Bidu")
                .species("Cachorro")
                .client(client)
                .build());
    }

    private Appointment saveAppointment(AppointmentStatus status) {
        return appointmentRepository.save(Appointment.builder()
                .scheduledAt(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES))
                .durationMin(30)
                .reason("Consulta de teste")
                .pet(pet)
                .vet(vet)
                .status(status)
                .build());
    }

    @Test
    void excluirPetComConsultaVinculadaRetorna409() throws Exception {
        saveAppointment(AppointmentStatus.SCHEDULED);
        String token = tokenFor(admin);

        mockMvc.perform(delete("/api/pets/" + pet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito"));
    }

    @Test
    void excluirPetSemVinculoRetorna204() throws Exception {
        String token = tokenFor(admin);

        mockMvc.perform(delete("/api/pets/" + pet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluirVeterinarioComConsultaVinculadaRetorna409() throws Exception {
        saveAppointment(AppointmentStatus.SCHEDULED);
        String token = tokenFor(admin);

        mockMvc.perform(delete("/api/users/" + vet.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito"));
    }

    @Test
    void excluirClienteComPetQueTemConsultaRetorna409() throws Exception {
        saveAppointment(AppointmentStatus.SCHEDULED);
        String token = tokenFor(admin);

        mockMvc.perform(delete("/api/clients/" + client.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito"));
    }

    @Test
    void excluirConsultaComProntuarioRegistradoRetorna409() throws Exception {
        Appointment appointment = saveAppointment(AppointmentStatus.COMPLETED);
        medicalRecordRepository.save(MedicalRecord.builder()
                .diagnosis("Tudo bem")
                .appointment(appointment)
                .pet(pet)
                .build());
        String token = tokenFor(admin);

        mockMvc.perform(delete("/api/appointments/" + appointment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflito"));
    }
}
