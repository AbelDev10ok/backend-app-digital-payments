package com.app.digital.payments.digital_pyments.controllers;

import java.util.List;

import javax.naming.Binding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;
import com.app.digital.payments.digital_pyments.services.IClientServices;

import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ValidationEntities validationEntities;

    @Autowired
    private IClientServices clientServices;

     @PostMapping
    public ResponseEntity<?> crearCliente(@Valid @RequestBody ClientRequestDto request, BindingResult result) {
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        ClientDto clienteDTO = clientServices.createClient(request);
        return new ResponseEntity<>(clienteDTO, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<ClientDto>> obtenerTodosLosClientes() {
        List<ClientDto> clientes = clientServices.getAllClients();
        return ResponseEntity.ok(clientes);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> obtenerClientePorId(@PathVariable Long id) {
        ClientDto clienteDTO = clientServices.getClientById(id);
        return ResponseEntity.ok(clienteDTO);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> actualizarCliente(
            @PathVariable Long id, 
            @Valid @RequestBody ClientRequestDto request) {
        ClientDto clienteDTO = clientServices.updateClient(id, request);
        return ResponseEntity.ok(clienteDTO);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clientServices.deletedClient(id);
        return ResponseEntity.noContent().build();
    }
}
