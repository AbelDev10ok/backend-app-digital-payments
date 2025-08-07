package com.app.digital.payments.digital_pyments.models.dtos;

import com.app.digital.payments.digital_pyments.models.Role;

public class UsuarioResponse {
    private Long id;
    private String email;
    private Role role;
    private boolean isEnabled;


    public UsuarioResponse() {
    }


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getEmail() {
        return email;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public Role getRole() {
        return role;
    }


    public void setRole(Role role) {
        this.role = role;
    }


    public boolean isEnabled() {
        return isEnabled;
    }


    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    
}
