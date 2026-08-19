package com.vetclinic.api.appointment;

import com.vetclinic.api.appointment.dto.AppointmentRequest;
import com.vetclinic.api.appointment.dto.AppointmentResponse;
import com.vetclinic.api.appointment.dto.UpdateStatusRequest;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.medicalrecord.MedicalRecordRepository;
import com.vetclinic.api.pet.Pet;
import com.vetclinic.api.pet.PetService;
import com.vetclinic.api.service.ClinicService;
import com.vetclinic.api.service.ClinicServiceManager;
import com.vetclinic.api.user.Role;
import com.vetclinic.api.user.User;
import com.vetclinic.api.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final PetService petService;
    private final UserService userService;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicServiceManager clinicServiceManager;

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Pet pet = petService.getOrThrow(request.petId());
        User vet = requireVet(request.vetId());
        ClinicService service = resolveService(request.serviceId());
        int durationMin = request.durationMin() != null ? request.durationMin() : 30;

        assertNoConflict(vet.getId(), request.scheduledAt(), durationMin, null);

        Appointment appointment = Appointment.builder()
                .scheduledAt(request.scheduledAt())
                .durationMin(durationMin)
                .reason(request.reason())
                .pet(pet)
                .vet(vet)
                .service(service)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public Page<AppointmentResponse> findAll(UUID vetId, UUID petId, Pageable pageable) {
        Page<Appointment> page;
        if (vetId != null) {
            page = appointmentRepository.findByVetId(vetId, pageable);
        } else if (petId != null) {
            page = appointmentRepository.findByPetId(petId, pageable);
        } else {
            page = appointmentRepository.findAll(pageable);
        }
        return page.map(AppointmentResponse::from);
    }

    public AppointmentResponse findById(UUID id) {
        return AppointmentResponse.from(getOrThrow(id));
    }

    @Transactional
    public AppointmentResponse reschedule(UUID id, AppointmentRequest request) {
        Appointment appointment = getOrThrow(id);
        Pet pet = petService.getOrThrow(request.petId());
        User vet = requireVet(request.vetId());
        ClinicService service = resolveService(request.serviceId());
        int durationMin = request.durationMin() != null ? request.durationMin() : 30;

        assertNoConflict(vet.getId(), request.scheduledAt(), durationMin, appointment.getId());

        appointment.setScheduledAt(request.scheduledAt());
        appointment.setDurationMin(durationMin);
        appointment.setReason(request.reason());
        appointment.setPet(pet);
        appointment.setVet(vet);
        appointment.setService(service);

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID id, UpdateStatusRequest request) {
        Appointment appointment = getOrThrow(id);
        assertValidTransition(appointment.getStatus(), request.status());
        appointment.setStatus(request.status());
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public void delete(UUID id) {
        if (!appointmentRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Consulta", id);
        }

        if (medicalRecordRepository.existsByAppointmentId(id)) {
            throw new ConflictException(
                    "Não é possível excluir: esta consulta já possui um prontuário registrado."
            );
        }

        appointmentRepository.deleteById(id);
    }

    public Appointment getOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Consulta", id));
    }

    private ClinicService resolveService(UUID serviceId) {
        return serviceId != null ? clinicServiceManager.getOrThrow(serviceId) : null;
    }

    private User requireVet(UUID vetId) {
        User user = userService.getOrThrow(vetId);
        if (user.getRole() != Role.VET) {
            throw new IllegalArgumentException("O usuário informado não possui o papel VET.");
        }
        return user;
    }

    /**
     * Garante que o veterinário não tenha outro agendamento ativo (não cancelado)
     * cujo intervalo de horário se sobreponha ao novo agendamento.
     */
    private void assertNoConflict(UUID vetId, LocalDateTime scheduledAt, int durationMin, UUID ignoreAppointmentId) {
        LocalDateTime endsAt = scheduledAt.plusMinutes(durationMin);
        LocalDateTime dayStart = scheduledAt.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Appointment> sameDayAppointments = appointmentRepository
                .findByVetIdAndStatusNotAndScheduledAtBetween(vetId, AppointmentStatus.CANCELED, dayStart, dayEnd);

        boolean hasConflict = sameDayAppointments.stream()
                .filter(existing -> !existing.getId().equals(ignoreAppointmentId))
                .anyMatch(existing -> existing.overlaps(scheduledAt, endsAt));

        if (hasConflict) {
            throw new ConflictException(
                    "O veterinário já possui uma consulta agendada que conflita com o horário "
                            + scheduledAt.format(DISPLAY_FORMAT) + "."
            );
        }
    }

    private void assertValidTransition(AppointmentStatus current, AppointmentStatus next) {
        boolean isFinal = current == AppointmentStatus.COMPLETED || current == AppointmentStatus.CANCELED;
        if (isFinal && current != next) {
            throw new ConflictException(
                    "Não é possível alterar o status de uma consulta " + current + " para " + next + "."
            );
        }
    }
}
