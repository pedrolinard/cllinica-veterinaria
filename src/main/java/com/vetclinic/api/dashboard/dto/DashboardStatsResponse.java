package com.vetclinic.api.dashboard.dto;

import java.util.Map;

public record DashboardStatsResponse(
        long clients,
        long pets,
        long appointments,
        long services,
        Map<String, Long> appointmentsByStatus,
        Map<String, Long> petsBySpecies
) {
}
