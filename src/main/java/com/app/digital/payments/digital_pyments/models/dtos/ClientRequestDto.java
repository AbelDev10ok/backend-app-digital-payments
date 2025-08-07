package com.app.digital.payments.digital_pyments.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClientRequestDto {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 15, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "El teléfono debe contener solo números y tener entre 10 y 15 dígitos")
    private String telefono;
    @Email(message = "El email debe tener un formato válido")
    private String email;
    @Size(max = 20, message = "La dirección no puede exceder los 50 caracteres")
    private String direccion;

    // El ID del cliente que será el vendedor de este nuevo cliente.
    // Usamos Long para el ID. Puede ser nulo si no tiene un vendedor asignado.
    // No usamos @NotNull ya que un cliente puede no tener vendedor.
    private Long sellerId;

    public ClientRequestDto() {
    }   

    public ClientRequestDto(
            @NotBlank(message = "El nombre es obligatorio") @Size(min = 2, max = 15, message = "El nombre debe tener entre 2 y 100 caracteres") String name,
            @NotBlank(message = "El teléfono es obligatorio") @Pattern(regexp = "^[0-9]{10,15}$", message = "El teléfono debe contener solo números y tener entre 10 y 15 dígitos") String telefono,
            @Email(message = "El email debe tener un formato válido") String email,
            @Size(max = 20, message = "La dirección no puede exceder los 50 caracteres") String direccion,
            @NotNull(message = "El campo 'isSeller' es obligatorio") boolean isSeller, Long sellerId) {
        this.name = name;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }


    
}
