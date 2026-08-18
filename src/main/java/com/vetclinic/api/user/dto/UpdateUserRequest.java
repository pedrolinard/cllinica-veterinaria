package com.vetclinic.api.user.dto;

import com.vetclinic.api.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @NotBlank(message = "Email é obrigatório.")
        @Email(message = "Email inválido.")
        String email,

        @NotNull(message = "Papel (role) é obrigatório.")
        Role role
) {
}
