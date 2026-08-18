package com.vetclinic.api.pet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    Page<Pet> findByClientId(UUID clientId, Pageable pageable);
}
