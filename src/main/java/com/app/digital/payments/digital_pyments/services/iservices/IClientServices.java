package com.app.digital.payments.digital_pyments.services.iservices;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;


public interface IClientServices {

    public ClientDto createClient(ClientRequestDto request);
    
    public Page<ClientDto> getAllClients(Pageable pageable, String searchTerm, Long vendorId, Boolean noVendedora);    

    // public List<ClientDto> getAllClientsWithoutSeller();
    
    public ClientDto getClientById(Long id);
    
    ClientDto updateClient(Long id, ClientRequestDto request);
    
    public void deletedClient(Long id);

    public Map<String, BigDecimal> calcularDeudaTotalVentas(Long clientId);

    public Map<String, BigDecimal> calcularTotalVentasPagadas(Long clientId);

    public Map<String, BigDecimal> calcularDeudaTotalPrestamos(Long clientId);

    public Map<String, BigDecimal> calcularTotalPrestamosPagados(Long clientId);

    void habilitarClientVendedor(Long id);
    
    void deshabilitarClientVendedor(Long id);

    List<ClientDto> obtenerTodosLosVendedoresConClientesActivos();

    List<ClientDto> getClientsBySellerId(Long sellerId);

    public List<ClientDto> obtenerVendedoresConClientesAsignados();

}
