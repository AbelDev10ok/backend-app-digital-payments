package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.StockMovement;
import com.app.digital.payments.digital_pyments.models.dtos.StockAdjustmentRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockEntryLineDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockEntryRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockMovementDto;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.IStockMovementRepository;
import com.app.digital.payments.digital_pyments.utils.StockMovementType;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Libro de inventario: el movimiento es la verdad y el stock cacheado tiene que
 * quedar siempre igual a la suma de los deltas.
 */
class StockServicesTest {

    private static final Long NEGOCIO_ID = 1L;
    private static final Long VARIANTE_ID = 10L;

    @Mock
    private IProductVariantRepository variantRepository;

    @Mock
    private IStockMovementRepository movementRepository;

    private StockServices stockServices;
    private ProductVariant variante;
    private List<StockMovement> libro;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContext.setBusinessId(NEGOCIO_ID);

        variante = new ProductVariant();
        variante.setId(VARIANTE_ID);
        variante.setSku("HEL-INO");
        variante.setStock(5);
        Business negocio = new Business();
        negocio.setId(NEGOCIO_ID);
        variante.setBusiness(negocio);

        when(variantRepository.findById(VARIANTE_ID)).thenReturn(Optional.of(variante));
        libro = new ArrayList<>();
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(inv -> {
            StockMovement movimiento = inv.getArgument(0);
            libro.add(movimiento);
            return movimiento;
        });

        stockServices = new StockServices(variantRepository, movementRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private StockEntryRequestDto ingreso(Integer cantidad, BigDecimal costo) {
        StockEntryLineDto linea = new StockEntryLineDto();
        linea.setVariantId(VARIANTE_ID);
        linea.setQuantity(cantidad);
        linea.setUnitCost(costo);
        StockEntryRequestDto request = new StockEntryRequestDto();
        request.setNote("Compra a proveedor");
        request.setLines(List.of(linea));
        return request;
    }

    private StockAdjustmentRequestDto ajuste(Integer contado, String motivo) {
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto();
        request.setVariantId(VARIANTE_ID);
        request.setCountedQuantity(contado);
        request.setReason(motivo);
        return request;
    }

    @Test
    void elIngresoSumaAlStockYRegistraElMovimiento() {
        List<StockMovementDto> movimientos = stockServices.registrarIngreso(ingreso(10, new BigDecimal("250.50")));

        assertEquals(15, variante.getStock());
        assertEquals(1, movimientos.size());
        StockMovementDto movimiento = movimientos.get(0);
        assertEquals(StockMovementType.INGRESO, movimiento.getType());
        assertEquals(10, movimiento.getDelta());
        assertEquals(15, movimiento.getStockAfter());
        assertEquals(new BigDecimal("250.50"), movimiento.getUnitCost());
        assertEquals("Compra a proveedor", movimiento.getReason());
        assertEquals("HEL-INO", movimiento.getSku());
    }

    @Test
    void elIngresoRechazaCantidadNoPositiva() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> stockServices.registrarIngreso(ingreso(0, null)));

        assertEquals("La cantidad del ingreso debe ser mayor a 0", error.getMessage());
        assertEquals(5, variante.getStock());
        assertTrue(libro.isEmpty());
    }

    @Test
    void elAjusteCalculaElDeltaContraElStockActual() {
        StockMovementDto movimiento = stockServices.registrarAjuste(ajuste(2, "Faltante de conteo"));

        assertEquals(2, variante.getStock());
        assertEquals(-3, movimiento.getDelta());
        assertEquals(StockMovementType.AJUSTE, movimiento.getType());
        assertEquals("Faltante de conteo", movimiento.getReason());
        assertEquals(2, movimiento.getStockAfter());
    }

    @Test
    void elAjusteSinDiferenciaNoRegistraNada() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> stockServices.registrarAjuste(ajuste(5, "Conteo mensual")));

        assertEquals("El stock contado coincide con el actual: no hay ajuste que registrar", error.getMessage());
        assertEquals(5, variante.getStock());
        assertTrue(libro.isEmpty());
    }

    @Test
    void elAjusteExigeMotivo() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> stockServices.registrarAjuste(ajuste(3, "   ")));

        assertEquals("El motivo del ajuste es obligatorio", error.getMessage());
        assertEquals(5, variante.getStock());
    }

    @Test
    void unMovimientoQueDejaStockNegativoSeRechazaSinMutacion() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> stockServices.registrarMovimiento(NEGOCIO_ID, variante, StockMovementType.VENTA, -11,
                        null, null, 99L));

        assertEquals("Stock insuficiente de HEL-INO: quedan 5 unidades", error.getMessage());
        assertEquals(5, variante.getStock());
        verify(movementRepository, never()).save(any(StockMovement.class));
        verify(variantRepository, never()).save(any(ProductVariant.class));
    }

    @Test
    void unMovimientoSinCantidadSeRechaza() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> stockServices.registrarMovimiento(NEGOCIO_ID, variante, StockMovementType.AJUSTE, 0,
                        null, "sin efecto", null));

        assertEquals("Un movimiento de stock no puede tener cantidad 0", error.getMessage());
        assertEquals(5, variante.getStock());
    }

    @Test
    void laSumaDeLosDeltasSiempreIgualaElStock() {
        // Variante sin saldo previo: todo su stock viene del libro.
        variante.setStock(0);

        stockServices.registrarIngreso(ingreso(10, null));
        stockServices.registrarMovimiento(NEGOCIO_ID, variante, StockMovementType.VENTA, -3, null, null, 7L);
        stockServices.registrarAjuste(ajuste(2, "Merma"));

        int suma = libro.stream().mapToInt(StockMovement::getDelta).sum();
        assertEquals(variante.getStock(), suma);
        assertEquals(2, variante.getStock());
    }

    @Test
    void laCargaInicialAsientaElSaldoSinVolverASumarlo() {
        when(movementRepository.existsByVariantId(VARIANTE_ID)).thenReturn(false);

        stockServices.registrarCargaInicialSiFalta(variante);

        assertEquals(5, variante.getStock());
        assertEquals(1, libro.size());
        StockMovement movimiento = libro.get(0);
        assertEquals(StockMovementType.INGRESO, movimiento.getType());
        assertEquals(5, movimiento.getDelta());
        assertEquals("Carga inicial", movimiento.getReason());
        assertNotNull(movimiento.getVariantId());
    }

    @Test
    void laCargaInicialNoSeDuplicaSiLaVarianteYaTieneMovimientos() {
        when(movementRepository.existsByVariantId(VARIANTE_ID)).thenReturn(true);

        stockServices.registrarCargaInicialSiFalta(variante);

        assertTrue(libro.isEmpty());
        assertEquals(5, variante.getStock());
    }

    @Test
    void laCargaInicialDeStockCeroNoGeneraMovimiento() {
        variante.setStock(0);
        when(movementRepository.existsByVariantId(VARIANTE_ID)).thenReturn(false);

        stockServices.registrarCargaInicialSiFalta(variante);

        assertTrue(libro.isEmpty());
    }
}
