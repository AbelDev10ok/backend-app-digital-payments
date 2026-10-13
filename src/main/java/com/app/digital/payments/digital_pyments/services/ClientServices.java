package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IClientServices;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;



@Service
public class ClientServices implements IClientServices {

    private final IClientRepository clientRepository;

    private final IBusinessRepository businessRepository;

    ClientServices(IClientRepository clientRepository, IBusinessRepository businessRepository) {
        this.clientRepository = clientRepository;
        this.businessRepository = businessRepository;
    }


    @Override
    @Transactional
    public ClientDto createClient(ClientRequestDto request) {
        // Para asegurar la consistencia de los datos y evitar duplicados por mayúsculas/minúsculas,
        // es una buena práctica normalizar ciertos campos, como el nombre y el email.
        Long negocioId = TenantContext.requireBusinessId();

        // VALIDAMOS QUE EL DNI SEA UNICO DENTRO DEL NEGOCIO
        if (request.getDni() != null && !request.getDni().isBlank()) {
            clientRepository.findByBusinessIdAndDni(negocioId, request.getDni()).ifPresent(c -> {
                throw new RuntimeException("El DNI ya está en uso por otro cliente.");
            });
        }

        // VALIDAMOS EMAIL DEL CLIENTE REPETIDO (dentro del negocio)
        if(request.getEmail() != null && !request.getEmail().isEmpty()){
            Client clienteConEmail = clientRepository.findByBusinessIdAndEmail(negocioId, request.getEmail()).orElse(null);
            if(clienteConEmail != null){
                throw new RuntimeException("El email del cliente ya está en uso por otro cliente.");
            }
        }

        Client cliente = new Client();
        // mapeo el cliente, normalizando los datos a minúsculas para consistencia
        cliente.setName(request.getName() != null ? request.getName().toLowerCase() : null);
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        cliente.setDni(request.getDni());
        cliente.setBusiness(businessRepository.getReferenceById(negocioId));

        // asigno un vendedor a este nuevo cliente si el Id fue proporcionado
        if (request.getSellerId() != null) {
            Client vendedor = clientRepository.findById(request.getSellerId())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado con ID: " + request.getSellerId()));
            // el vendedor tiene que pertenecer al mismo negocio
            TenantGuard.verificar(vendedor.getBusiness() != null ? vendedor.getBusiness().getId() : null);
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
    
    @Override
    @Transactional
    public void habilitarClientVendedor(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        TenantGuard.verificar(cliente.getBusiness() != null ? cliente.getBusiness().getId() : null);
        
        // Verificamos si el cliente ya es un vendedor
        if (cliente.isSeller()) {
            throw new RuntimeException("El cliente con ID: " + id + " ya es un vendedor.");
        }
        
        // Habilitamos al cliente como vendedor
        cliente.setSeller(true);
        clientRepository.save(cliente);
    }

    @Override
    @Transactional
    public void deshabilitarClientVendedor(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        TenantGuard.verificar(cliente.getBusiness() != null ? cliente.getBusiness().getId() : null);
        
        // Verificamos si el cliente no es un vendedor
        if (!cliente.isSeller()) {
            throw new RuntimeException("El cliente con ID: " + id + " no es un vendedor.");
        }

        // validacion : impedir deshabilitar si tiene clientes asociados
        if (clientRepository.countBySellerId(id) > 0) {
            throw new RuntimeException("El vendedor aún tiene clientes asignados. Reasigna o desasigna sus clientes antes de deshabilitarlo.");
        }

        // Deshabilitamos al cliente como vendedor
        cliente.setSeller(false);
        clientRepository.save(cliente);
    }

    @Transactional
    public void asignarVendedorACliente(Long clienteId, Long vendedorId) {
        Client cliente = clientRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + clienteId));
        TenantGuard.verificar(cliente.getBusiness() != null ? cliente.getBusiness().getId() : null);

        Client vendedor = clientRepository.findById(vendedorId)
            .orElseThrow(() -> new RuntimeException("Vendedor no encontrado con ID: " + vendedorId));
        TenantGuard.verificar(vendedor.getBusiness() != null ? vendedor.getBusiness().getId() : null);
        
        // Verificamos que el vendedor sea un vendedor válido
        if (!vendedor.isSeller()) {
            throw new RuntimeException("El cliente con ID: " + vendedorId + " no es un vendedor válido.");
        }
        
        // Asignamos el vendedor al cliente
        cliente.setSeller(vendedor);
        clientRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Page<ClientDto> getAllClients(Pageable pageable, String searchTerm, Long vendorId, Boolean noVendedora) {
        Specification<Client> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchTerm != null && !searchTerm.isBlank()) {
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + searchTerm.toLowerCase() + "%"
                    ),
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("dni")),
                        "%" + searchTerm.toLowerCase() + "%"
                    )
                ));
            }

            if (vendorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("seller").get("id"), vendorId));
            }

            if (noVendedora != null && noVendedora) {
                predicates.add(criteriaBuilder.isNull(root.get("seller")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Client> clientesPage = clientRepository.findAll(spec, pageable);

        // Enriquecer con deuda total por cliente en una sola query
        List<Long> clientIds = clientesPage.getContent().stream().map(Client::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> deudaMap = new HashMap<>();
        if (!clientIds.isEmpty()) {
            for (Object[] row : clientRepository.sumRemainingAmountByClientIds(clientIds, SaleStatus.ACTIVE)) {
                deudaMap.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
            }
        }

        return clientesPage.map(c -> {
            ClientDto dto = convertirADTO(c);
            dto.setDeudaTotal(deudaMap.getOrDefault(c.getId(), BigDecimal.ZERO));
            return dto;
        });
    }

    @Override
    public ClientDto getClientById(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        TenantGuard.verificar(cliente.getBusiness() != null ? cliente.getBusiness().getId() : null);
        return convertirADTO(cliente);
    }
    
    @Override
    @Transactional
    public ClientDto updateClient(Long id, ClientRequestDto request) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        TenantGuard.verificar(cliente.getBusiness() != null ? cliente.getBusiness().getId() : null);

        Long negocioId = TenantContext.requireBusinessId();

        // VALIDAMOS QUE EL DNI SEA UNICO DENTRO DEL NEGOCIO
        if (request.getDni() != null && !request.getDni().isBlank()) {
            clientRepository.findByBusinessIdAndDni(negocioId, request.getDni()).ifPresent(existingClient -> {
                if (!existingClient.getId().equals(id)) {
                    throw new RuntimeException("El DNI ya está en uso por otro cliente.");
                }
            });
        }
 
        // VALIDAMOS NOMBRE DEL CLIENTE REPETIDO (insensible a mayúsculas/minúsculas)
        if (request.getName() != null && !request.getName().isBlank()) {
            clientRepository.findByName(request.getName().toLowerCase()).ifPresent(existingClient -> {
                if (!existingClient.getId().equals(id)) {
                    throw new RuntimeException("El nombre del cliente ya está en uso por otro cliente.");
                }
            });
        }
 
        // VALIDAMOS EMAIL DEL CLIENTE REPETIDO (dentro del negocio)
        if(request.getEmail() != null && !request.getEmail().isEmpty()){
            clientRepository.findByBusinessIdAndEmail(negocioId, request.getEmail()).ifPresent(existingClient -> {
                if (!existingClient.getId().equals(id)) {
                    throw new RuntimeException("El email del cliente ya está en uso por otro cliente.");
                }
            });
        }

        // Asigno un vendedor a este cliente si el Id fue proporcionado
        if (request.getSellerId() != null) {
            Client vendedor = clientRepository.findById(request.getSellerId())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado con ID: " + request.getSellerId()));
            // el vendedor tiene que pertenecer al mismo negocio
            TenantGuard.verificar(vendedor.getBusiness() != null ? vendedor.getBusiness().getId() : null);
            // valido que el vendedor tenga el rol de vendedor
            if (!vendedor.isSeller()) {
                throw new RuntimeException("El cliente con ID: " + request.getSellerId() + " no es un vendedor válido.");
            }
            // asignamos el vendedor al cliente
            cliente.setSeller(vendedor);
        } else {
            // Si no se proporciona sellerId, desasignar el vendedor
            cliente.setSeller(null);
        }

 
        // mapeo el cliente, normalizando los datos a minúsculas para consistencia
        cliente.setName(request.getName() != null ? request.getName().toLowerCase() : null);
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        cliente.setDireccion(request.getDireccion());
        cliente.setDni(request.getDni());
        
        Client clienteActualizado = clientRepository.save(cliente);
        return convertirADTO(clienteActualizado);
    }

    @Override
    @Transactional
    public void deletedClient(Long id) {
        Client cliente = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
        TenantGuard.verificar(cliente.getBusiness() != null ? cliente.getBusiness().getId() : null);

        // si el cliente es vendedor, verificar que no tenga clientes asignados
            if (clientRepository.countBySellerId(id) > 0) {
                throw new RuntimeException("No se puede eliminar el cliente porque es un vendedor con clientes asignados.");
        }

        // si el cliente tiene ventas o prestamos asociados, no se puede eliminar
        if (clientRepository.existenClientesConTransaccionesAsociadas(id)) {
            throw new RuntimeException("No se puede eliminar el cliente porque tiene ventas asociadas.");
        }

        clientRepository.delete(cliente);
    }

    // calcula la deuda total de un cliente para ventas 
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularDeudaTotalVentas(Long clientId) {
        BigDecimal deudaTotal = clientRepository.sumRemainingAmountByClientIdAndSalesNotCompleted(
                clientId, SaleStatus.COMPLETED, SaleKind.PRESTAMO);
        return deudaTotal != null ? deudaTotal : BigDecimal.ZERO;
    }   
    // calcula el total cobrado en ventas de un cliente (suma de cuotas pagadas)
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalVentasPagadas(Long clientId) {
        BigDecimal total = clientRepository.sumCollectedAmountByClientIdExcludingKind(
                clientId, SaleKind.PRESTAMO);
        return total != null ? total : BigDecimal.ZERO;
    }

    // calcula la deuda total de un cliente para prestamos
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularDeudaTotalPrestamos(Long clientId) {
        BigDecimal deudaTotal = clientRepository.sumRemainingAmountByClientIdAndLoansNotCompleted(
                clientId, SaleStatus.COMPLETED, SaleKind.PRESTAMO);
        return deudaTotal != null ? deudaTotal : BigDecimal.ZERO;
    }

    // calcula el total cobrado en prestamos de un cliente (suma de cuotas pagadas)
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalPrestamosPagados(Long clientId) {
        BigDecimal total = clientRepository.sumCollectedAmountByClientIdByKind(
                clientId, SaleKind.PRESTAMO);
        return total != null ? total : BigDecimal.ZERO;
    }

    // obtener todos los vendedores que puedan vender a los clientes
    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> obtenerTodosLosVendedoresConClientesActivos() {
        List<Client> vendedores = clientRepository.findAllSellersActivos(TenantContext.requireBusinessId());
        return vendedores.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // obtener vendedores que sigan asignados a clientes
    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> obtenerVendedoresConClientesAsignados() {
        List<Client> vendedores = clientRepository.findSellersWithAssignedClients(TenantContext.requireBusinessId());
        return vendedores.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // obtener clientes por vendedor
    @Override
    @Transactional(readOnly = true)
    public List<ClientDto> getClientsBySellerId(Long sellerId) {
        List<Client> clientes = clientRepository.findBySellerId(sellerId);
        return clientes.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    private ClientDto convertirADTO(Client cliente) {
        return new ClientDto(
            cliente.getId(),
            cliente.getName(),
            cliente.getTelefono(),
            cliente.getEmail(),
            cliente.getDireccion(),
            cliente.isSeller(),
            cliente.getSeller() != null ? cliente.getSeller().getName() : null,
            cliente.getDni()
        );
    }

}
