package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class VentaServicesTest {

    @Mock
    private ISaleRepository salesRepository;

    @Mock
    private IClientRepository clientRepository;

    @Mock
    private IFeeRepository feeRepository;

    @Mock
    private IProductTypeRepository productTypeRepository;

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IBusinessRepository businessRepository;

    private VentaServices ventaServices;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ventaServices = new VentaServices(salesRepository, clientRepository, feeRepository, productTypeRepository,
                productRepository, businessRepository);
        TenantContext.setBusinessId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private SaleRequestDto dtoVenta(String kindName, String descripcion) {
        ProductType productType = new ProductType();
        productType.setId(1L);
        productType.setName("TV");
        when(productTypeRepository.findById(1L)).thenReturn(Optional.of(productType));

        Client client = new Client();
        client.setId(1L);
        client.setName("Juan");
        client.setTelefono("1144556677");
        client.setEmail("juan@example.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        SaleRequestDto dto = new SaleRequestDto();
        dto.setClientId(1L);
        dto.setKind(kindName);
        dto.setProductType(1L);
        dto.setAmountFee(new BigDecimal("100"));
        dto.setQuantityFees(2);
        dto.setDescriptionProduct(descripcion);
        dto.setCost(new BigDecimal("50"));
        dto.setDateSale(LocalDate.now());
        dto.setFirstFeeDate(LocalDate.now());
        dto.setPayments("MENSUAL");
        dto.setPayFirstFee(false);
        return dto;
    }

    private void prepararProducto(Long id, String nombre, BigDecimal precio) {
        prepararProducto(id, nombre, precio, true);
    }

    private void prepararProducto(Long id, String nombre, BigDecimal precio, boolean conCategoria) {
        Business business = new Business();
        business.setId(1L);
        Product product = new Product();
        product.setId(id);
        product.setName(nombre);
        product.setPrice(precio);
        product.setBusiness(business);
        if (conCategoria) {
            ProductType pt = new ProductType();
            pt.setId(1L);
            pt.setName("TV");
            pt.setBusiness(business);
            product.setProductType(pt);
        }
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
    }

    private void prepararProductoConStock(Long id, String nombre, BigDecimal precio, Integer stock) {
        Business business = new Business();
        business.setId(1L);
        Product product = new Product();
        product.setId(id);
        product.setName(nombre);
        product.setPrice(precio);
        product.setStock(stock);
        product.setBusiness(business);
        ProductType pt = new ProductType();
        pt.setId(1L);
        pt.setName("TV");
        pt.setBusiness(business);
        product.setProductType(pt);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
    }

    @Test
    void createVentaCalculaTotalesYEstadoActivo() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(1L);

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("200"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, saved.getRemainingAmount().compareTo(new BigDecimal("200")));
        assertEquals(1, saved.getFees().size());
        assertEquals("Televisor", saved.getDescriptionProduct());
    }

    @Test
    void createVentaDeTipoPrestamoFijaDescripcion() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("200"));

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals("Préstamo personal", captor.getValue().getDescriptionProduct());
    }

    @Test
    void createPrestamoConInteresCalculaTotalYCouta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setQuantityFees(4);
        dto.setInterestRate(new BigDecimal("10"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        // total = 1000 * 1.10 = 1100, cuota = 1100 / 4 = 275
        assertEquals(0, new BigDecimal("1100").compareTo(result.getPriceTotal()));
        assertEquals(0, new BigDecimal("275").compareTo(result.getAmountFee()));

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, new BigDecimal("1100").compareTo(saved.getRemainingAmount()));
        assertEquals(0, new BigDecimal("10").compareTo(saved.getInterestRate()));
    }

    @Test
    void createPrestamoConInteresNoExigeCostIgualATotal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setQuantityFees(4);
        dto.setAmountFee(new BigDecimal("999"));
        dto.setInterestRate(new BigDecimal("10"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(0, new BigDecimal("1100").compareTo(result.getPriceTotal()));
    }

    @Test
    void createPrestamoConInteresNegativoEsRechazado() {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("1000"));
        dto.setInterestRate(new BigDecimal("-5"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaRechazaCantidadDeCuotasInvalida() {
        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(1L);
        dto.setQuantityFees(0);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaRechazaVentaSinProducto() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dtoVenta("VENTA", "Televisor")));
    }

    @Test
    void createVentaUsaCostoDelProductoIgnorandoElEnviado() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(1L);
        dto.setCost(new BigDecimal("1"));

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("150").compareTo(captor.getValue().getCost()));
        assertEquals("Televisor", captor.getValue().getDescriptionProduct());
    }

    @Test
    void createVentaRechazaProductoSinPrecioValido() {
        prepararProducto(1L, "Televisor", null);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(1L);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaUsaLaCategoriaDelProducto() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(1L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(1L);
        dto.setProductType(99L);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getProductType().getId());
    }

    @Test
    void createVentaRechazaProductoSinCategoria() {
        prepararProducto(1L, "Televisor", new BigDecimal("150"), false);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(1L);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void updateSaleUsaLaCategoriaDelProducto() throws Exception {
        Business business = new Business();
        business.setId(1L);

        ProductType pt = new ProductType();
        pt.setId(1L);
        pt.setName("TV");
        pt.setBusiness(business);

        Product product = new Product();
        product.setId(5L);
        product.setName("TV");
        product.setPrice(new BigDecimal("150"));
        product.setBusiness(business);
        product.setProductType(pt);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        ProductType ptRequest = new ProductType();
        ptRequest.setId(99L);
        ptRequest.setName("Mueble");
        ptRequest.setBusiness(business);
        when(productTypeRepository.findById(99L)).thenReturn(Optional.of(ptRequest));

        Sale existingSale = new Sale();
        existingSale.setId(1L);
        existingSale.setBusiness(business);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(existingSale));

        com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto update = new SaleUpdateRequestDto();
        update.setKind("VENTA");
        update.setProduct(5L);
        update.setProductType(99L);
        update.setDescriptionProduct("Televisor actualizado");

        ventaServices.updateSale(update, 1L);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getProductType().getId());
    }

    @Test
    void createVentaConProductoAsociaProductoALaVenta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "TV", new BigDecimal("150"));

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getProduct().getId());
    }

    @Test
    void createVentaRechazaProductoDeOtroNegocio() {
        Business business = new Business();
        business.setId(999L);
        Product product = new Product();
        product.setId(5L);
        product.setName("TV");
        product.setBusiness(business);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaConProductoInexistenteLanzaExcepcion() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(99L);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaConCantidadMultiplicaCostoYDescripcion() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);
        dto.setQuantity(3);
        dto.setAmountFee(new BigDecimal("250"));

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(3, saved.getQuantity());
        assertEquals(0, new BigDecimal("450").compareTo(saved.getCost()));
        assertEquals("3 x Televisor", saved.getDescriptionProduct());
    }

    @Test
    void createVentaConCantidadPorDefectoEsUno() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);

        ventaServices.createVenta(dto);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(1, saved.getQuantity());
        assertEquals(0, new BigDecimal("150").compareTo(saved.getCost()));
        assertEquals("Televisor", saved.getDescriptionProduct());
    }

    @Test
    void createVentaRechazaCantidadCeroONegativa() {
        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);
        dto.setQuantity(0);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));

        dto.setQuantity(-2);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.createVenta(dto));
        verify(salesRepository, never()).save(any(Sale.class));
    }

    @Test
    void createVentaDescuentaStockDelProducto() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProductoConStock(5L, "Televisor", new BigDecimal("150"), 10);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);
        dto.setQuantity(3);
        dto.setAmountFee(new BigDecimal("250"));

        ventaServices.createVenta(dto);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getStock());
    }

    @Test
    void createVentaConStockInsuficienteNoBloqueaYNoDescuenta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProductoConStock(5L, "Televisor", new BigDecimal("150"), 2);
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);
        dto.setQuantity(3); // mayor al stock
        dto.setAmountFee(new BigDecimal("250"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());
        verify(salesRepository).save(any(Sale.class));
        // Stock no se descuenta porque quantity > stock
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createVentaConProductoSinControlDeInventarioNoDescuenta() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        prepararProducto(5L, "Televisor", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Televisor");
        dto.setProduct(5L);
        dto.setQuantity(3);
        dto.setAmountFee(new BigDecimal("250"));

        ventaServices.createVenta(dto);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteVentaReponeStockDelProducto() throws Exception {
        Business business = new Business();
        business.setId(1L);
        Product product = new Product();
        product.setId(5L);
        product.setStock(7);
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setProduct(product);
        sale.setQuantity(3);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.deleteVenta(1L);

        assertEquals(10, product.getStock());
        verify(productRepository).save(product);
        verify(salesRepository).delete(sale);
    }

    @Test
    void deleteVentaConProductoSinControlDeInventarioNoTocaStock() throws Exception {
        Business business = new Business();
        business.setId(1L);
        Product product = new Product();
        product.setId(5L);
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setProduct(product);
        sale.setQuantity(3);
        sale.setFees(new java.util.ArrayList<>());
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.deleteVenta(1L);

        verify(productRepository, never()).save(any(Product.class));
        verify(salesRepository).delete(sale);
    }

    private Sale ventaConCuotaImpaga(BigDecimal remaining, BigDecimal amountCuota) {
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setRemainingAmount(remaining);
        sale.setStatus(SaleStatus.ACTIVE);
        Business business = new Business();
        business.setId(1L);
        sale.setBusiness(business);
        Fee cuota = new Fee();
        cuota.setId(1L);
        cuota.setSale(sale);
        cuota.setNumberFee(1);
        cuota.setAmount(amountCuota);
        cuota.setPaid(false);
        sale.setFees(java.util.List.of(cuota));
        return sale;
    }

    @Test
    void markPrestamoAsCompletedPermiteMontoDistintoAlValorDeLaCuota() {
        Sale sale = ventaConCuotaImpaga(new BigDecimal("200"), new BigDecimal("100"));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        ventaServices.markPrestamoAsCompleted(1L, new BigDecimal("150"));

        assertEquals(0, new BigDecimal("50").compareTo(sale.getRemainingAmount()));
        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
        assertEquals(0, new BigDecimal("150").compareTo(sale.getFees().get(0).getAmount()));
    }

    @Test
    void markPrestamoAsCompletedRechazaMontoMayorAlSaldoRestante() {
        Sale sale = ventaConCuotaImpaga(new BigDecimal("100"), new BigDecimal("100"));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.markPrestamoAsCompleted(1L, new BigDecimal("150")));

        assertEquals(0, new BigDecimal("100").compareTo(sale.getRemainingAmount()));
        assertEquals(Boolean.FALSE, sale.getFees().get(0).getPaid());
    }

    private Fee cuotaPaga(BigDecimal monto) {
        Fee fee = new Fee();
        fee.setId(1L);
        fee.setNumberFee(1);
        fee.setAmount(monto);
        fee.setPaid(true);
        fee.setExpirationDate(LocalDate.now());
        fee.setDatePayment(LocalDate.now());
        return fee;
    }

    private Fee cuotaImpaga(BigDecimal monto) {
        Fee fee = new Fee();
        fee.setId(2L);
        fee.setNumberFee(2);
        fee.setAmount(monto);
        fee.setPaid(false);
        fee.setExpirationDate(LocalDate.now().plusMonths(1));
        return fee;
    }

    private Sale ventaParaAnular(BigDecimal remaining, Product product, List<Fee> cuotas) {
        Business business = new Business();
        business.setId(1L);
        Client client = new Client();
        client.setId(1L);
        client.setName("Juan");
        client.setTelefono("1144556677");
        Sale sale = new Sale();
        sale.setId(1L);
        sale.setBusiness(business);
        sale.setClient(client);
        sale.setKind(SaleKind.VENTA);
        sale.setDescriptionProduct("Televisor");
        sale.setPriceTotal(remaining);
        sale.setCost(new BigDecimal("100"));
        sale.setAmountFee(new BigDecimal("100"));
        sale.setQuiantityFees(2);
        sale.setTypePayments(Payments.MENSUAL);
        sale.setDateCreation(LocalDate.now());
        sale.setRemainingAmount(remaining);
        sale.setStatus(SaleStatus.ACTIVE);
        sale.setProduct(product);
        sale.setQuantity(3);
        java.util.ArrayList<Fee> lista = new java.util.ArrayList<>(cuotas);
        lista.forEach(fee -> fee.setSale(sale));
        sale.setFees(lista);
        return sale;
    }

    @Test
    void cancelVentaMarcaEstadoCanceladoYDevuelveDto() throws Exception {
        Product product = new Product();
        product.setId(5L);
        Sale sale = ventaParaAnular(new BigDecimal("200"), product, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true);

        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
        verify(salesRepository).save(sale);
    }

    @Test
    void cancelVentaReponeStockDelProducto() throws Exception {
        Product product = new Product();
        product.setId(5L);
        product.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("200"), product, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServices.cancelVenta(1L, true);

        assertEquals(10, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    void cancelVentaConProductoSinControlDeInventarioNoTocaStock() throws Exception {
        Product product = new Product();
        product.setId(5L);
        Sale sale = ventaParaAnular(new BigDecimal("200"), product, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServices.cancelVenta(1L, true);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void cancelVentaCalculaRefundDeCuotasPagadasYDejaRemainingEnCero() throws Exception {
        Product product = new Product();
        product.setId(5L);
        Sale sale = ventaParaAnular(new BigDecimal("150"), product,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaServices.cancelVenta(1L, true);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, new BigDecimal("100").compareTo(saved.getRefundAmount()));
        assertEquals(0, saved.getRemainingAmount().signum());
        assertEquals(LocalDate.now(), saved.getRealFinalDate());
        assertEquals(SaleStatus.CANCELED, saved.getStatus());
    }

    @Test
    void cancelVentaConRefundTrueDevuelveMontoCobradoEnElDto() throws Exception {
        Product product = new Product();
        product.setId(5L);
        product.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), product,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true);

        assertEquals(0, new BigDecimal("100").compareTo(result.getRefundAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(result.getCollectedAmount()));
        assertEquals(0, new BigDecimal("0").compareTo(result.getRemainingAmount()));
        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
    }

    @Test
    void cancelVentaConRefundFalseRetieneElMontoCobrado() throws Exception {
        Product product = new Product();
        product.setId(5L);
        product.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("150"), product,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, false);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(0, saved.getRefundAmount().signum());
        assertEquals(0, saved.getRemainingAmount().signum());
        assertEquals(LocalDate.now(), saved.getRealFinalDate());
        assertEquals(SaleStatus.CANCELED, saved.getStatus());
        assertEquals(0, new BigDecimal("100").compareTo(result.getCollectedAmount()));
    }

    @Test
    void cancelVentaConRefundFalseNoRegistraDevolucionYDejaDeudaEnCero() throws Exception {
        Product product = new Product();
        product.setId(5L);
        Sale sale = ventaParaAnular(new BigDecimal("150"), product,
                List.of(cuotaPaga(new BigDecimal("100")), cuotaImpaga(new BigDecimal("50"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, false);

        // Sin devolución y sin deuda pendiente: el registro anulado no aporta a la
        // deuda del cliente (remaining = 0) ni a los reembolsos (refundAmount = 0).
        assertEquals(0, result.getRefundAmount().signum());
        assertEquals(0, result.getRemainingAmount().signum());
        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
    }

    @Test
    void cancelVentaSinCuotasPagadasDevuelveRefundEnCero() throws Exception {
        Product product = new Product();
        product.setId(5L);
        Sale sale = ventaParaAnular(new BigDecimal("200"), product, List.of(cuotaImpaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true);

        assertEquals(0, result.getRefundAmount().signum());
        assertEquals(0, new BigDecimal("0").compareTo(result.getCollectedAmount()));
        assertEquals(0, result.getRemainingAmount().signum());
    }

    @Test
    void cancelVentaYaCanceladaEsIdempotente() throws Exception {
        Product product = new Product();
        product.setId(5L);
        product.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("0"), product, List.of(cuotaPaga(new BigDecimal("200"))));
        sale.setStatus(SaleStatus.CANCELED);
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        SaleResponseDto result = ventaServices.cancelVenta(1L, true);

        assertEquals(SaleStatus.CANCELED.name(), result.getStatus());
        verify(salesRepository, never()).save(any(Sale.class));
        verify(productRepository, never()).save(any(Product.class));
        assertEquals(7, product.getStock());
    }

    @Test
    void deleteVentaRechazaSiTieneCuotasPagadas() {
        Product product = new Product();
        product.setId(5L);
        product.setStock(7);
        Sale sale = ventaParaAnular(new BigDecimal("100"), product, List.of(cuotaPaga(new BigDecimal("100"))));
        when(salesRepository.findById(1L)).thenReturn(Optional.of(sale));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ventaServices.deleteVenta(1L));

        verify(salesRepository, never()).delete(any(Sale.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    // --- Tests de tipos de pago no MENSUAL ---

    @Test
    void createVentaSemanal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setProduct(1L);
        dto.setPayments("SEMANAL");
        dto.setQuantityFees(4);

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("400"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(Payments.SEMANAL, saved.getTypePayments());
        assertEquals(Integer.valueOf(4), saved.getQuiantityFees());
    }

    @Test
    void createVentaQuincenal() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setProduct(1L);
        dto.setPayments("QUINCENAL");
        dto.setQuantityFees(6);

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("600"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(Payments.QUINCENAL, saved.getTypePayments());
        assertEquals(Integer.valueOf(6), saved.getQuiantityFees());
    }

    @Test
    void createVentaContado() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("30"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setProduct(1L);
        dto.setPayments("CONTADO");
        dto.setQuantityFees(4); // debe forzarse a 1

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("100"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        assertEquals(Payments.CONTADO, saved.getTypePayments());
        assertEquals(Integer.valueOf(1), saved.getQuiantityFees());
        assertEquals(1, saved.getFees().size());
    }

    @Test
    void createPrestamoContadoRechazado() {
        SaleRequestDto dto = dtoVenta("PRESTAMO", null);
        dto.setCost(new BigDecimal("200"));
        dto.setPayments("CONTADO");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ventaServices.createVenta(dto));
    }

    @Test
    void createVentaConPayFirstFee() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("150"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setProduct(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(3);
        dto.setFirstFeeAmount(new BigDecimal("200"));

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(new BigDecimal("300"), result.getPriceTotal());
        assertEquals(SaleStatus.ACTIVE.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        // 2 fees: [siguienteCuota (impaga), primeraCuota (pagada)]
        assertEquals(2, saved.getFees().size());
        // La primera en la lista es la siguiente (impaga, numero 2)
        Fee siguiente = saved.getFees().get(0);
        assertEquals(Boolean.FALSE, siguiente.getPaid());
        assertEquals(Integer.valueOf(2), siguiente.getNumberFee());
        // La segunda es la primera cuota pagada
        Fee primera = saved.getFees().get(1);
        assertEquals(Boolean.TRUE, primera.getPaid());
        assertEquals(Integer.valueOf(1), primera.getNumberFee());
        assertEquals(0, new BigDecimal("200").compareTo(primera.getAmount()));
        // Saldo restante = 300 - 200 = 100
        assertEquals(0, new BigDecimal("100").compareTo(saved.getRemainingAmount()));
    }

    @Test
    void createVentaConPayFirstFeeCubreTodoElSaldo() throws Exception {
        when(salesRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        prepararProducto(1L, "Producto", new BigDecimal("30"));
        SaleRequestDto dto = dtoVenta("VENTA", "Producto");
        dto.setProduct(1L);
        dto.setPayFirstFee(true);
        dto.setQuantityFees(1);
        dto.setAmountFee(new BigDecimal("400"));
        dto.setFirstFeeAmount(new BigDecimal("400")); // paga todo

        SaleResponseDto result = ventaServices.createVenta(dto);

        assertEquals(SaleStatus.COMPLETED.name(), result.getStatus());

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(salesRepository).save(captor.capture());
        Sale saved = captor.getValue();
        // Solo 1 fee (pagada) porque remaining = 0, no se genera segunda
        assertEquals(1, saved.getFees().size());
        assertEquals(Boolean.TRUE, saved.getFees().get(0).getPaid());
        assertEquals(Integer.valueOf(1), saved.getFees().get(0).getNumberFee());
    }
}
