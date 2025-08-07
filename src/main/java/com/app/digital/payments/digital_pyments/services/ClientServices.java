package com.app.digital.payments.digital_pyments.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;



@Service
public class ClientServices implements IClientServices {

    @Autowired
    private IClientRepository clientRepository;

    @Override
    @Transactional
    public ClientDto createClient(ClientRequestDto request) {
        Client cliente = new Client();
        // mapeo el cliente
        cliente.setName(request.getName());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());

        // asigno un vendedor a este nuevo cliente si el Id fue proporcionado
        if (request.getSellerId() != null) {
            Client vendedor = clientRepository.findById(request.getSellerId())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado con ID: " + request.getSellerId()));
            cliente.setSeller(vendedor);
            // valido que el vendedor tenga el rol de vendedor
            if (!vendedor.isSeller()) {
                throw new RuntimeException("El cliente con ID: " + request.getSellerId() + " no es un vendedor válido.");
            }
            // asignamos el vendedor al nuevo cliente
            cliente.setSeller(vendedor);
        }
        Client clienteGuardado = clientRepository.save(cliente);
        return convertirADTO(clienteGuardado);
    }
    

    @Transactional
    public void habilitarClientVendedor(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        
        // Verificamos si el cliente ya es un vendedor
        if (cliente.isSeller()) {
            throw new RuntimeException("El cliente con ID: " + id + " ya es un vendedor.");
        }
        
        // Habilitamos al cliente como vendedor
        cliente.setSeller(true);
        clientRepository.save(cliente);
    }

    @Transactional
    public void deshabilitarClientVendedor(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        
        // Verificamos si el cliente no es un vendedor
        if (!cliente.isSeller()) {
            throw new RuntimeException("El cliente con ID: " + id + " no es un vendedor.");
        }
        
        // Deshabilitamos al cliente como vendedor
        cliente.setSeller(false);
        clientRepository.save(cliente);
    }

    @Transactional
    public void asignarVendedorACliente(Long clienteId, Long vendedorId) {
        Client cliente = clientRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));
        
        Client vendedor = clientRepository.findById(vendedorId)
            .orElseThrow(() -> new RuntimeException("Vendedor no encontrado con ID: " + vendedorId));
        
        // Verificamos que el vendedor sea un vendedor válido
        if (!vendedor.isSeller()) {
            throw new RuntimeException("El cliente con ID: " + vendedorId + " no es un vendedor válido.");
        }
        
        // Asignamos el vendedor al cliente
        cliente.setSeller(vendedor);
        clientRepository.save(cliente);
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
