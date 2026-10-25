package com.app.digital.payments.digital_pyments.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeDto;
import com.app.digital.payments.digital_pyments.models.dtos.AttributeValueDto;
import com.app.digital.payments.digital_pyments.services.AttributeServices;

@RestController
@RequestMapping("/api/attributes")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')")
public class AttributeController {

    private final AttributeServices attributeServices;

    public AttributeController(AttributeServices attributeServices) {
        this.attributeServices = attributeServices;
    }

    @GetMapping("/all")
    public ResponseEntity<?> obtenerTodos() {
        return ResponseEntity.ok(attributeServices.getAll());
    }

    @PostMapping()
    public ResponseEntity<?> crear(@RequestBody AttributeDto attributeDto) {
        return ResponseEntity.ok(attributeServices.create(attributeDto));
    }

    @PostMapping("/{id}/values")
    public ResponseEntity<?> agregarValor(@PathVariable Long id, @RequestBody AttributeValueDto valueDto) {
        return ResponseEntity.ok(attributeServices.addValue(id, valueDto != null ? valueDto.getValue() : null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        boolean eliminado = attributeServices.delete(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.badRequest().body(Map.of("deleted", false));
    }
}
