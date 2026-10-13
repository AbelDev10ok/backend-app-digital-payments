package com.app.digital.payments.digital_pyments.models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "role", uniqueConstraints = {
		@UniqueConstraint(name = "uk_role_name", columnNames = { "name" })
})
public class Role {

    public static final String ROLE_USER = "ROLE_USER";

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "role")
    private List<Usuario> usuario;

    public Role() {
    }

    public Role(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
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

    public List<Usuario> getUsuario() {
        return usuario;
    }   

    public void setUsuario(List<Usuario> usuario) {
        this.usuario = usuario;
    }

    
}
