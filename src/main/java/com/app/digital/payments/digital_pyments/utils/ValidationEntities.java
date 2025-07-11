package com.app.digital.payments.digital_pyments.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class ValidationEntities {
    
    public ResponseEntity<?> validation(BindingResult result){
        Map<String, String> errores = new HashMap<>();
        result.getFieldErrors().forEach(err -> {
            errores.put(err.getField(), err.getDefaultMessage());
        });
        // Devuelve el Map directamente, no como String
        return ResponseEntity.badRequest().body(errores);
    }
}
