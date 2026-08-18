package com.vetclinic.api.client;

import com.vetclinic.api.client.dto.ClientRequest;
import com.vetclinic.api.client.dto.ClientResponse;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
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
        if (!clientRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Cliente", id);
        }
        clientRepository.deleteById(id);
    }

    public Client getOrThrow(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }
}
