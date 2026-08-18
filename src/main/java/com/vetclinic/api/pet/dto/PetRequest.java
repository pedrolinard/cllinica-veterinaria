package com.vetclinic.api.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record PetRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @NotBlank(message = "Espécie é obrigatória.")
        String species,

        String breed,

        @PastOrPresent(message = "Data de nascimento não pode ser no futuro.")
        LocalDate birthDate,

        @Positive(message = "Peso deve ser positivo.")
        Double weightKg,

        String notes,

        @NotNull(message = "Cliente (tutor) é obrigatório.")
        UUID clientId
) {
}
