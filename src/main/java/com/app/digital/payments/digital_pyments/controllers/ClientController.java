package com.app.digital.payments.digital_pyments.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ClientRequestDto;
import com.app.digital.payments.digital_pyments.services.VentaQueryServices;
import com.app.digital.payments.digital_pyments.services.iservices.IClientServices;
import com.app.digital.payments.digital_pyments.utils.ApiResponse;
import com.app.digital.payments.digital_pyments.utils.ValidationEntities;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
public class ClientController {


    private final  ValidationEntities validationEntities;

    

    private final VentaQueryServices ventaQueryServices;


    private final IClientServices clientServices;

    ClientController(ValidationEntities validationEntities, VentaQueryServices ventaQueryServices, IClientServices clientServices) {
        this.validationEntities = validationEntities;
        this.ventaQueryServices = ventaQueryServices;
        this.clientServices = clientServices;
    }


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
    
    @GetMapping()
    public ResponseEntity<Page<ClientDto>> obtenerClientes(
        Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long sellerId,
        @RequestParam(required = false) Boolean withoutSeller
    ) {
        Page<ClientDto> clientes = clientServices.getAllClients(pageable, search, sellerId, withoutSeller);
        return ResponseEntity.ok(clientes);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> obtenerClientePorId(@PathVariable Long id) {
        ClientDto clienteDTO = clientServices.getClientById(id);
        return ResponseEntity.ok(clienteDTO);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCliente(
            @PathVariable Long id, 
            @Valid @RequestBody ClientRequestDto request, BindingResult result) {
        if(result.hasFieldErrors() || result.hasGlobalErrors()) {
            return ResponseEntity 
                    .badRequest()
                    .body(validationEntities.validation(result));
        }
        ClientDto clienteDTO = clientServices.updateClient(id, request);
        return ResponseEntity.ok(clienteDTO);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clientServices.deletedClient(id);
        return ResponseEntity.noContent().build();
    }

    // calcula la deuda total de un cliente para ventas
    @GetMapping("/{id}/deuda-ventas")
    public ResponseEntity<?> calcularDeudaTotalVentas(@PathVariable Long id) {
        BigDecimal deudaTotal = clientServices.calcularDeudaTotalVentas(id);
        return ResponseEntity.ok(deudaTotal);
    }

    // calcula el total de ventas pagadas de un cliente
    @GetMapping("/{id}/total-ventas-pagadas")
    public ResponseEntity<?> calcularTotalVentasPagadas(@PathVariable Long id) {
        BigDecimal totalVentasPagadas = clientServices.calcularTotalVentasPagadas(id);
        return ResponseEntity.ok(totalVentasPagadas)
        ;
    }

    // calcula la deuda total de un cliente para prestamos
    @GetMapping("/{id}/deuda-prestamos")
    public ResponseEntity<?> calcularDeudaTotalPrestamos(@PathVariable Long id)
    {
        BigDecimal deudaTotal = clientServices.calcularDeudaTotalPrestamos(id);
        return ResponseEntity.ok(deudaTotal);
    }

    // calcula el total de prestamos pagados de un cliente
    @GetMapping("/{id}/total-prestamos-pagados")    
    public ResponseEntity<?> calcularTotalPrestamosPagados(@PathVariable Long id) {
        BigDecimal totalPrestamosPagados = clientServices.calcularTotalPrestamosPagados(id);
        return ResponseEntity.ok(totalPrestamosPagados);
    }

    // habilitar cliente como vendedor
    @PutMapping("/{id}/habilitar")
    public ResponseEntity<?> habilitarCliente(@PathVariable Long id) {
        clientServices.habilitarClientVendedor(id);
        return ResponseEntity.ok("Cliente habilitado como vendedor");
    }

    // deshabilitar cliente como vendedor
    @PutMapping("/{id}/desabilitar")
    public ResponseEntity<?> deshabilitarCliente(@PathVariable Long id) {
        clientServices.deshabilitarClientVendedor(id);
        return ResponseEntity.ok("Cliente deshabilitado como vendedor");
    }

    // obtener todos los clientes que son vendedores
    @GetMapping("/vendedores")
    public ResponseEntity<List<ClientDto>> obtenerTodosLosVendedoreConClientesAsignados() {
        List<ClientDto> vendedores = clientServices.obtenerVendedoresConClientesAsignados();
        return ResponseEntity.ok(vendedores);
    }

    // obtener todos los vendedores activos 
    @GetMapping("/vendedores/activos")
    public ResponseEntity<List<ClientDto>> obtenerTodosLosVendedoresActivos() {
        List<ClientDto> vendedores = clientServices.obtenerTodosLosVendedoresConClientesActivos();
        return ResponseEntity.ok(vendedores);
    }

    // obtener clientes por vendedor
    @GetMapping("/vendedor/{sellerId}/clientes")
    public ResponseEntity<List<ClientDto>> getClientsBySellerId(@PathVariable Long sellerId) {
        List<ClientDto> clientes = clientServices.getClientsBySellerId(sellerId);
        return ResponseEntity.ok(clientes);
    }


    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getClientStats() {
        Map<String, Object> stats = ventaQueryServices.getClientStats();
        return ResponseEntity.ok().body(new ApiResponse("Venta obtenida correctamente", HttpStatus.OK, stats));
    }

    
}
