package com.app.digital.payments.digital_pyments.controllers;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.services.ProductServices;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasRole('" + Role.ROLE_USER + "')") // Se aplica a todos los métodos de esta clase
public class ProductController {

    private final ProductServices productServices;

    public ProductController(ProductServices productServices) {
        this.productServices = productServices;
    }

    @GetMapping("/all")
    public ResponseEntity<?> obtenerTodosProductos() {
        return ResponseEntity.ok(productServices.getAllProducts());
    }

    @PostMapping()
    public ResponseEntity<?> crearProducto(@RequestBody ProductDto productDto) {
        BigDecimal price = productDto.getPrice();
        boolean creado = productServices.createProduct(productDto.getName(), price, productDto.getStock(),
                productDto.getProductTypeId());
        if (creado) {
            return ResponseEntity.ok(Map.of("created", true));
        }
        return ResponseEntity.badRequest().body(Map.of("created", false));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificarProducto(@PathVariable Long id, @RequestBody ProductDto productDto) {
        BigDecimal price = productDto.getPrice();
        boolean actualizado = productServices.updateProduct(id, productDto.getName(), price, productDto.getStock(),
                productDto.getProductTypeId());
        if (actualizado) {
            return ResponseEntity.ok(Map.of("updated", true));
        }
        return ResponseEntity.badRequest().body(Map.of("updated", false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        boolean eliminado = productServices.deleteProduct(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.badRequest().body(Map.of("deleted", false));
    }

}