package com.vetclinic.api.pet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    interface SpeciesCount {
        String getSpecies();
        long getCount();
    }

    Page<Pet> findByClientId(UUID clientId, Pageable pageable);

    Page<Pet> findByClientIdAndNameContainingIgnoreCase(UUID clientId, String name, Pageable pageable);

    Page<Pet> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("select p.species as species, count(p) as count from Pet p group by p.species")
    List<SpeciesCount> countGroupedBySpecies();
}
