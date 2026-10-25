package com.app.digital.payments.digital_pyments.models.dtos;

import java.math.BigDecimal;
import java.util.Map;

public class ClientDto {
    private Long id;
    private String name;
    private String telefono;
    private String email;
    private String direccion;
    private boolean isSeller;
    private String sellerName;
    private String dni;
    // Deuda por moneda ISO 4217: las monedas distintas nunca se suman
    private Map<String, BigDecimal> deudaTotal;

    public ClientDto() {
    }

    public ClientDto(Long id, String name, String telefono, String email, String direccion, boolean isSeller, String sellerName, String dni) {
        this.id = id;
        this.name = name;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.isSeller = isSeller;
        this.sellerName = sellerName;
        this.dni = dni;
    }

    public boolean isSeller() {
        return isSeller;
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

    public void setSeller(boolean isSeller) {
        this.isSeller = isSeller;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Map<String, BigDecimal> getDeudaTotal() {
        return deudaTotal;
    }

    public void setDeudaTotal(Map<String, BigDecimal> deudaTotal) {
        this.deudaTotal = deudaTotal;
    }
}
