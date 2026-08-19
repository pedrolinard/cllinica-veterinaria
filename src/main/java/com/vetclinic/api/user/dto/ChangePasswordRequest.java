package com.vetclinic.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Senha atual é obrigatória.")
        String currentPassword,

        @NotBlank(message = "Nova senha é obrigatória.")
        @Size(min = 6, message = "Nova senha deve ter ao menos 6 caracteres.")
        String newPassword
) {
}
