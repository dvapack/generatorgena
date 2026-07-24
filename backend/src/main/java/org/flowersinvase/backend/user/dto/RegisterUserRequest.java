package org.flowersinvase.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record RegisterUserRequest(
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        @Size(max = 320, message = "Email не может быть длиннее 320 символов")
        String email,

        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String password
) {

    public RegisterUserRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
}