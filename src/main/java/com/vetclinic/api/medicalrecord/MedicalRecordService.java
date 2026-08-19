package com.vetclinic.api.medicalrecord;

import com.vetclinic.api.appointment.Appointment;
import com.vetclinic.api.appointment.AppointmentService;
import com.vetclinic.api.appointment.AppointmentStatus;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.medicalrecord.dto.MedicalRecordRequest;
import com.vetclinic.api.medicalrecord.dto.MedicalRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentService appointmentService;

    @Transactional
    public MedicalRecordResponse create(MedicalRecordRequest request) {
        Appointment appointment = appointmentService.getOrThrow(request.appointmentId());

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException(
                    "Só é possível registrar o prontuário de uma consulta com status COMPLETED. "
                            + "Atualize o status da consulta antes de continuar."
            );
        }

        if (medicalRecordRepository.existsByAppointmentId(appointment.getId())) {
            throw new ConflictException("Esta consulta já possui um prontuário registrado.");
        }

        MedicalRecord record = MedicalRecord.builder()
                .diagnosis(request.diagnosis())
                .notes(request.notes())
                .weightKg(request.weightKg())
                .vaccines(request.vaccines())
                .prescriptions(request.prescriptions())
                .appointment(appointment)
                .pet(appointment.getPet())
                .build();

        // saveAndFlush (não save): força o INSERT agora, para que o @CreationTimestamp
        // já esteja preenchido no objeto retornado — com save() simples, o flush só
        // acontece no commit da transação, e a resposta sairia com createdAt nulo.
        return MedicalRecordResponse.from(medicalRecordRepository.saveAndFlush(record));
    }

    public Page<MedicalRecordResponse> findByPet(UUID petId, Pageable pageable) {
        return medicalRecordRepository.findByPetId(petId, pageable).map(MedicalRecordResponse::from);
    }

    public MedicalRecordResponse findById(UUID id) {
        return MedicalRecordResponse.from(getOrThrow(id));
    }

    @Transactional
    public MedicalRecordResponse update(UUID id, MedicalRecordRequest request) {
        MedicalRecord record = getOrThrow(id);
        record.setDiagnosis(request.diagnosis());
        record.setNotes(request.notes());
        record.setWeightKg(request.weightKg());
        record.setVaccines(request.vaccines());
        record.setPrescriptions(request.prescriptions());
        return MedicalRecordResponse.from(medicalRecordRepository.save(record));
    }

    @Transactional
    public void delete(UUID id) {
        if (!medicalRecordRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Prontuário", id);
        }
        medicalRecordRepository.deleteById(id);
    }

    private MedicalRecord getOrThrow(UUID id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Prontuário", id));
    }
}
