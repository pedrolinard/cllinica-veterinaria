package com.vetclinic.api.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @Email(message = "Email inválido.")
        String email,

        @NotBlank(message = "Telefone é obrigatório.")
        String phone,

        String address
) {
}
