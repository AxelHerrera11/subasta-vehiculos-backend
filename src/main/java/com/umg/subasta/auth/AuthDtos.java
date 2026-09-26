package com.umg.subasta.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Size(max = 80) String apellido,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") @Size(max = 150) String correo,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = "^\\+?[0-9 -]{8,20}$", message = "Teléfono no válido (mínimo 8 dígitos)") String telefono,
            @NotBlank(message = "La contraseña es obligatoria")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
                    message = "Mínimo 8 caracteres con mayúscula, minúscula, número y símbolo") String password) {}

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String password) {}

    public record UsuarioDto(int id, String nombre, String apellido, String correo, String telefono) {}

    public record AuthResponse(String token, UsuarioDto usuario) {}
}
