package com.vetclinic.api.pet.dto;

import com.vetclinic.api.pet.Pet;

import java.time.LocalDate;
import java.util.UUID;

public record PetResponse(
        UUID id,
        String name,
        String species,
        String breed,
        LocalDate birthDate,
        Double weightKg,
        String notes,
        UUID clientId,
        String clientName
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getBirthDate(),
                pet.getWeightKg(),
                pet.getNotes(),
                pet.getClient().getId(),
                pet.getClient().getName()
        );
    }
}
