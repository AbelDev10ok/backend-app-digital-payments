package com.app.digital.payments.digital_pyments.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;


@Service
public class ClientServices implements IClientServices {

    @Autowired
    private IClientRepository clientRepository;

    @Override
    public ClientDto createClient(ClientRequestDto request) {
        Client cliente = new Client();
        cliente.setName(request.getName());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        
        Client clienteGuardado = clientRepository.save(cliente);
        return convertirADTO(clienteGuardado);
    }
    
    @Override
    public List<ClientDto> getAllClients() {
        return clientRepository.findAll().stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public ClientDto getClientById(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        return convertirADTO(cliente);
    }
    
    @Override
    public ClientDto updateClient(Long id, ClientRequestDto request) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        
        cliente.setName(request.getName());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        
        Client clienteActualizado = clientRepository.save(cliente);
        return convertirADTO(clienteActualizado);
    }

    @Override
    public void deletedClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con ID: " + id);
        }
        clientRepository.deleteById(id);
    }
    
    private ClientDto convertirADTO(Client cliente) {
        return new ClientDto(
            cliente.getId(),
            cliente.getName(),
            cliente.getTelefono(),
            cliente.getEmail(),
            cliente.getDireccion()
        );
    }


}
