package com.vetclinic.api.pet;

import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.client.Client;
import com.vetclinic.api.client.ClientService;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.pet.dto.PetRequest;
import com.vetclinic.api.pet.dto.PetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final ClientService clientService;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public PetResponse create(PetRequest request) {
        Client client = clientService.getOrThrow(request.clientId());

        Pet pet = Pet.builder()
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .weightKg(request.weightKg())
                .notes(request.notes())
                .client(client)
                .build();

        return PetResponse.from(petRepository.save(pet));
    }

    public Page<PetResponse> findAll(UUID clientId, Pageable pageable) {
        Page<Pet> page = clientId != null
                ? petRepository.findByClientId(clientId, pageable)
                : petRepository.findAll(pageable);
        return page.map(PetResponse::from);
    }

    public PetResponse findById(UUID id) {
        return PetResponse.from(getOrThrow(id));
    }

    @Transactional
    public PetResponse update(UUID id, PetRequest request) {
        Pet pet = getOrThrow(id);
        Client client = clientService.getOrThrow(request.clientId());

        pet.setName(request.name());
        pet.setSpecies(request.species());
        pet.setBreed(request.breed());
        pet.setBirthDate(request.birthDate());
        pet.setWeightKg(request.weightKg());
        pet.setNotes(request.notes());
        pet.setClient(client);

        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public void delete(UUID id) {
        if (!petRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Pet", id);
        }

        long appointments = appointmentRepository.countByPetId(id);
        if (appointments > 0) {
            throw new ConflictException(
                    "Não é possível excluir: este pet possui " + appointments
                            + " consulta(s) vinculada(s). Remova ou cancele as consultas primeiro."
            );
        }

        petRepository.deleteById(id);
    }

    public Pet getOrThrow(UUID id) {
        return petRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Pet", id));
    }
}
