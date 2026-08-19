package com.vetclinic.api.dashboard;

import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.client.ClientRepository;
import com.vetclinic.api.dashboard.dto.DashboardStatsResponse;
import com.vetclinic.api.pet.PetRepository;
import com.vetclinic.api.service.ClinicServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agrega estatísticas no banco (COUNT/GROUP BY) em vez do dashboard precisar
 * carregar todas as consultas/pets no navegador para contar — o que ficava
 * incompleto e cada vez mais lento à medida que a clínica crescia.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ClientRepository clientRepository;
    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicServiceRepository clinicServiceRepository;

    public DashboardStatsResponse stats() {
        Map<String, Long> appointmentsByStatus = new LinkedHashMap<>();
        appointmentRepository.countGroupedByStatus()
                .forEach(row -> appointmentsByStatus.put(row.getStatus().name(), row.getCount()));

        Map<String, Long> petsBySpecies = new LinkedHashMap<>();
        petRepository.countGroupedBySpecies()
                .forEach(row -> petsBySpecies.put(row.getSpecies(), row.getCount()));

        return new DashboardStatsResponse(
                clientRepository.count(),
                petRepository.count(),
                appointmentRepository.count(),
                clinicServiceRepository.count(),
                appointmentsByStatus,
                petsBySpecies
        );
    }
}
