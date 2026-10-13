package com.app.digital.payments.digital_pyments.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UsuarioAuth {
    @NotBlank(message = "email is required")
    @Email(message = "Email is not valid", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    private String email;
    @NotBlank(message = "password is required")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String password;

    // Nombre del negocio (tenant). Opcional: si no viene se usa uno por defecto.
    private String nombreNegocio;

    UsuarioAuth() {
    }
    public UsuarioAuth(String email, String password) {
        this.email = email;
        this.password = password;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }
    public void setNombreNegocio(String nombreNegocio) {
        this.nombreNegocio = nombreNegocio;
    }


}
