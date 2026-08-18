package com.vetclinic.api.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ClinicServiceRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        String description,

        @NotNull(message = "Preço é obrigatório (em centavos).")
        @PositiveOrZero(message = "Preço não pode ser negativo.")
        Integer priceCents,

        @Positive(message = "Duração deve ser positiva.")
        Integer durationMin,

        Boolean active
) {
}
