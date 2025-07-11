package com.app.digital.payments.digital_pyments.models.dtos;

public class ClientDto {
    private Long id;
    private String name;
    private String telefono;
    private String email;
    private String direccion;

    public ClientDto() {
    }

    public ClientDto(Long id, String name, String telefono, String email, String direccion) {
        this.id = id;
        this.name = name;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    
}
