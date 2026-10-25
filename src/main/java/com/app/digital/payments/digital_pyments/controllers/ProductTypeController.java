package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.ProductTypeDto;
import com.app.digital.payments.digital_pyments.services.ProductTypeServices;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/product-types")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')") // Se aplica a todos los métodos de esta clas
public class ProductTypeController {    

    private final ProductTypeServices productTypeServices;

    public ProductTypeController(ProductTypeServices productTypeServices) {
        this.productTypeServices = productTypeServices;
    }

    @GetMapping("/all")
    public ResponseEntity<?> obtenerTodosProductType() {
        return  ResponseEntity.ok(productTypeServices.getAllProductTypes());
    }
    
    @PostMapping()
    public ResponseEntity<?> crearProductType(@RequestBody ProductTypeDto entity) {
        boolean creado = productTypeServices.createProductType(entity.getName(), entity.getParentId(), entity.getSlug());
        if (creado) {
            return ResponseEntity.ok(java.util.Map.of("created", true));
        }
        return ResponseEntity.badRequest().body(java.util.Map.of("created", false));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarProductType(@PathVariable Long id, @RequestBody ProductTypeDto entity) {
        boolean actualizado = productTypeServices.updateProductType(id, entity.getName(), entity.getParentId(),
                entity.getSlug());
        if (actualizado) {
            return ResponseEntity.ok(java.util.Map.of("updated", true));
        }
        return ResponseEntity.badRequest().body(java.util.Map.of("updated", false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProductType(@PathVariable Long id) {
        boolean eliminado = productTypeServices.deleteProductType(id);
        if (eliminado) {
            return ResponseEntity.ok(java.util.Map.of("deleted", true));
        }
        return ResponseEntity.badRequest().body(java.util.Map.of("deleted", false));
    }

}
