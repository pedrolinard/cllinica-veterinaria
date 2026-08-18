package com.vetclinic.api.client.dto;

import com.vetclinic.api.client.Client;

import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String address,
        int petsCount
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getPets() == null ? 0 : client.getPets().size()
        );
    }
}
