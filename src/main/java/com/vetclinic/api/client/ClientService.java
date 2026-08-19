package com.vetclinic.api.client;

import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.client.dto.ClientRequest;
import com.vetclinic.api.client.dto.ClientResponse;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.pet.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = Client.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .build();

        return ClientResponse.from(clientRepository.save(client));
    }

    public Page<ClientResponse> findAll(Pageable pageable) {
        return clientRepository.findAll(pageable).map(ClientResponse::from);
    }

    public ClientResponse findById(UUID id) {
        return ClientResponse.from(getOrThrow(id));
    }

    @Transactional
    public ClientResponse update(UUID id, ClientRequest request) {
        Client client = getOrThrow(id);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID id) {
        Client client = getOrThrow(id);

        // Consulta os pets diretamente (em vez de client.getPets()) para não depender do
        // estado da coleção em memória, que pode não refletir pets criados depois que o
        // Client já estava carregado no contexto de persistência da transação atual.
        long appointments = petRepository.findByClientId(id, Pageable.unpaged()).stream()
                .mapToLong(pet -> appointmentRepository.countByPetId(pet.getId()))
                .sum();
        if (appointments > 0) {
            throw new ConflictException(
                    "Não é possível excluir: os pets deste cliente possuem " + appointments
                            + " consulta(s) vinculada(s). Remova as consultas primeiro."
            );
        }

        clientRepository.delete(client);
    }

    public Client getOrThrow(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }
}
