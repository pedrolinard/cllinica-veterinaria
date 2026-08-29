package com.vetclinic.api.appointment;

import com.vetclinic.api.appointment.dto.AppointmentRequest;
import com.vetclinic.api.appointment.dto.AppointmentResponse;
import com.vetclinic.api.appointment.dto.UpdateStatusRequest;
import com.vetclinic.api.audit.AuditService;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.medicalrecord.MedicalRecordRepository;
import com.vetclinic.api.pet.Pet;
import com.vetclinic.api.pet.PetService;
import com.vetclinic.api.service.ClinicServiceManager;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import com.vetclinic.api.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários da regra de agenda do {@link AppointmentService}, com o
 * repositório e as dependências mockados — complementa os testes de integração
 * ({@code AppointmentConflictTest}) exercitando os caminhos de erro sem subir
 * o contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PetService petService;
    @Mock private UserService userService;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private ClinicServiceManager clinicServiceManager;
    @Mock private AuditService auditService;

    @InjectMocks private AppointmentService service;

    private Pet pet;
    private User vet;
    private final LocalDateTime slot = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

    @BeforeEach
    void setUp() {
        pet = Pet.builder().id(UUID.randomUUID()).name("Bidu").species("Cachorro").build();
        vet = User.builder().id(UUID.randomUUID()).name("Dra. Ana").email("ana@vetclinic.com").role(Role.VET).build();
    }

    private AppointmentRequest request(LocalDateTime at, int durationMin) {
        return new AppointmentRequest(at, durationMin, "Consulta", pet.getId(), vet.getId(), null);
    }

    private Appointment appointment(UUID id, LocalDateTime at, int durationMin) {
        return Appointment.builder()
                .id(id).scheduledAt(at).durationMin(durationMin)
                .pet(pet).vet(vet).status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    void criarComHorarioSobrepostoLancaConflito() {
        when(petService.getOrThrow(pet.getId())).thenReturn(pet);
        when(userService.getOrThrow(vet.getId())).thenReturn(vet);
        when(appointmentRepository.findByVetIdAndStatusNotAndScheduledAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of(appointment(UUID.randomUUID(), slot.plusMinutes(15), 30)));

        assertThatThrownBy(() -> service.create(request(slot, 30)))
                .isInstanceOf(ConflictException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void criarSemConflitoSalvaERetornaResposta() {
        when(petService.getOrThrow(pet.getId())).thenReturn(pet);
        when(userService.getOrThrow(vet.getId())).thenReturn(vet);
        when(appointmentRepository.findByVetIdAndStatusNotAndScheduledAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AppointmentResponse response = service.create(request(slot, 30));

        assertThat(response.vetId()).isEqualTo(vet.getId());
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void criarComUsuarioQueNaoEVetLancaIllegalArgument() {
        User admin = User.builder().id(UUID.randomUUID()).name("Chefe").email("chefe@vetclinic.com").role(Role.ADMIN).build();
        when(petService.getOrThrow(pet.getId())).thenReturn(pet);
        when(userService.getOrThrow(admin.getId())).thenReturn(admin);

        AppointmentRequest req = new AppointmentRequest(slot, 30, "x", pet.getId(), admin.getId(), null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void mudarStatusDeConsultaFinalizadaLancaConflito() {
        UUID id = UUID.randomUUID();
        Appointment completed = appointment(id, slot, 30);
        completed.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.updateStatus(id, new UpdateStatusRequest(AppointmentStatus.SCHEDULED)))
                .isInstanceOf(ConflictException.class);

        verify(appointmentRepository, never()).save(any());
        verify(auditService, never()).record(anyString(), any(), any(), anyString());
    }

    @Test
    void excluirConsultaComProntuarioLancaConflito() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment(id, slot, 30)));
        when(medicalRecordRepository.existsByAppointmentId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ConflictException.class);

        verify(appointmentRepository, never()).delete(any());
    }

    @Test
    void reagendarNaoConflitaComOProprioAgendamento() {
        UUID id = UUID.randomUUID();
        Appointment existing = appointment(id, slot, 30);
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(petService.getOrThrow(pet.getId())).thenReturn(pet);
        when(userService.getOrThrow(vet.getId())).thenReturn(vet);
        // O único agendamento do dia é o próprio que está sendo reagendado.
        when(appointmentRepository.findByVetIdAndStatusNotAndScheduledAtBetween(any(), any(), any(), any()))
                .thenReturn(List.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.reschedule(id, request(slot.plusMinutes(10), 30));

        assertThat(response.scheduledAt()).isEqualTo(slot.plusMinutes(10));
        verify(appointmentRepository).save(existing);
    }
}
