package com.vetclinic.api.user.dto;

import com.vetclinic.api.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @NotBlank(message = "Email é obrigatório.")
        @Email(message = "Email inválido.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres.")
        String password,

        @NotNull(message = "Papel (role) é obrigatório.")
        Role role
) {
}
