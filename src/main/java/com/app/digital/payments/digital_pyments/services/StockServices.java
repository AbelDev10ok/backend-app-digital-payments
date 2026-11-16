package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.StockMovement;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.StockAdjustmentRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockEntryLineDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockEntryRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.StockMovementDto;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.IStockMovementRepository;
import com.app.digital.payments.digital_pyments.utils.StockMovementType;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

/**
 * Inventario: libro de movimientos como fuente de verdad y stock cacheado en
 * la variante.
 *
 * <p>
 * Todas las mutaciones de {@code ProductVariant.stock} pasan por
 * {@link #registrarMovimiento}: el delta y el nuevo stock se escriben en la
 * misma transacción, así el invariante {@code SUM(delta) == stock} se puede
 * verificar y explicar. La única excepción es la carga inicial, que asienta un
 * saldo de apertura sin volver a sumarlo a la caché.
 * </p>
 */
@Service
public class StockServices {

    private final IProductVariantRepository variantRepository;
    private final IStockMovementRepository movementRepository;

    public StockServices(IProductVariantRepository variantRepository, IStockMovementRepository movementRepository) {
        this.variantRepository = variantRepository;
        this.movementRepository = movementRepository;
    }

    // ------------------------------------------------------------------
    // Casos de uso
    // ------------------------------------------------------------------

    /**
     * Ingreso de mercadería por lote. Cada línea crea un movimiento INGRESO con
     * su costo observado (que no reescribe el costo del catálogo).
     */
    @Transactional
    public List<StockMovementDto> registrarIngreso(StockEntryRequestDto request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("El ingreso debe tener al menos una línea");
        }
        Long negocioId = TenantContext.requireBusinessId();
        String motivo = limpiarTexto(request.getNote());
        if (motivo == null) {
            motivo = "Ingreso de mercadería";
        }

        List<StockMovementDto> resultado = new ArrayList<>();
        for (StockEntryLineDto linea : request.getLines()) {
            if (linea == null) {
                throw new IllegalArgumentException("El ingreso tiene una línea vacía");
            }
            if (linea.getVariantId() == null) {
                throw new IllegalArgumentException("Cada línea del ingreso debe indicar la variante");
            }
            if (linea.getQuantity() == null || linea.getQuantity() <= 0) {
                throw new IllegalArgumentException("La cantidad del ingreso debe ser mayor a 0");
            }
            if (linea.getUnitCost() != null && linea.getUnitCost().signum() < 0) {
                throw new IllegalArgumentException("El costo unitario no puede ser negativo");
            }
            ProductVariant variante = buscarVariante(negocioId, linea.getVariantId());
            StockMovement movimiento = registrarMovimiento(negocioId, variante, StockMovementType.INGRESO,
                    linea.getQuantity(), linea.getUnitCost(), motivo, null);
            resultado.add(aDto(movimiento, variante.getStock()));
        }
        return resultado;
    }

    /**
     * Ajuste por conteo: el delta sale de la diferencia contra el stock actual.
     * Un conteo que coincide no se registra (no hay movimiento que explicar).
     */
    @Transactional
    public StockMovementDto registrarAjuste(StockAdjustmentRequestDto request) {
        if (request == null || request.getVariantId() == null) {
            throw new IllegalArgumentException("El ajuste debe indicar la variante");
        }
        if (request.getCountedQuantity() == null || request.getCountedQuantity() < 0) {
            throw new IllegalArgumentException("El stock contado no puede ser negativo");
        }
        String motivo = limpiarTexto(request.getReason());
        if (motivo == null) {
            throw new IllegalArgumentException("El motivo del ajuste es obligatorio");
        }

        Long negocioId = TenantContext.requireBusinessId();
        ProductVariant variante = buscarVariante(negocioId, request.getVariantId());
        int actual = variante.getStock() != null ? variante.getStock() : 0;
        int delta = request.getCountedQuantity() - actual;
        if (delta == 0) {
            throw new IllegalArgumentException(
                    "El stock contado coincide con el actual: no hay ajuste que registrar");
        }
        StockMovement movimiento = registrarMovimiento(negocioId, variante, StockMovementType.AJUSTE, delta, null,
                motivo, null);
        return aDto(movimiento, variante.getStock());
    }

    /** Historial de movimientos del negocio, filtrable por variante, rango y tipo. */
    @Transactional(readOnly = true)
    public Page<StockMovementDto> historial(Long variantId, LocalDate from, LocalDate to, StockMovementType type,
            Pageable pageable) {
        Long negocioId = TenantContext.requireBusinessId();
        Specification<StockMovement> spec = (root, query, cb) -> {
            var predicados = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicados.add(cb.equal(root.get("businessId"), negocioId));
            if (variantId != null) {
                predicados.add(cb.equal(root.get("variantId"), variantId));
            }
            if (type != null) {
                predicados.add(cb.equal(root.get("type"), type));
            }
            if (from != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicados.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicados.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return movementRepository.findAll(spec, pageable).map(movimiento -> aDto(movimiento, null));
    }

    // ------------------------------------------------------------------
    // Escritura del libro (usada también por ventas y catálogo)
    // ------------------------------------------------------------------

    /**
     * Registra un movimiento y actualiza la caché del stock en la misma
     * transacción. Rechaza un movimiento sin efecto o que dejaría stock
     * negativo.
     */
    @Transactional
    public StockMovement registrarMovimiento(Long businessId, ProductVariant variante, StockMovementType tipo,
            int delta, BigDecimal unitCost, String reason, Long saleId) {
        if (businessId == null) {
            throw new IllegalArgumentException("El movimiento de stock necesita un negocio");
        }
        if (variante == null || variante.getId() == null) {
            throw new IllegalArgumentException("El movimiento de stock necesita una variante guardada");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El movimiento de stock necesita un tipo");
        }
        if (delta == 0) {
            throw new IllegalArgumentException("Un movimiento de stock no puede tener cantidad 0");
        }

        int actual = variante.getStock() != null ? variante.getStock() : 0;
        int nuevo = actual + delta;
        if (nuevo < 0) {
            throw new IllegalArgumentException("Stock insuficiente de " + variante.getSku() + ": quedan " + actual
                    + " unidades");
        }

        variante.setStock(nuevo);
        variantRepository.save(variante);

        StockMovement movimiento = new StockMovement();
        movimiento.setBusinessId(businessId);
        movimiento.setVariantId(variante.getId());
        movimiento.setSku(variante.getSku());
        movimiento.setType(tipo);
        movimiento.setDelta(delta);
        movimiento.setUnitCost(unitCost);
        movimiento.setReason(limpiarTexto(reason));
        movimiento.setSaleId(saleId);
        movimiento.setCreatedAt(LocalDateTime.now());
        movimiento.setCreatedBy(usuarioActual());
        return movementRepository.save(movimiento);
    }

    /**
     * Asienta el saldo de apertura de una variante que todavía no tiene
     * movimientos (datos previos al libro o alta de producto). No toca la caché:
     * el stock ya está en la variante y el movimiento sólo lo explica. Un stock
     * 0 no genera movimiento (la suma vacía ya da 0).
     */
    @Transactional
    public void registrarCargaInicialSiFalta(ProductVariant variante) {
        if (variante == null || variante.getId() == null) {
            return;
        }
        if (movementRepository.existsByVariantId(variante.getId())) {
            return;
        }
        int stock = variante.getStock() != null ? variante.getStock() : 0;
        if (stock <= 0) {
            return;
        }
        Long negocioId = variante.getBusiness() != null ? variante.getBusiness().getId() : null;
        if (negocioId == null) {
            negocioId = TenantContext.requireBusinessId();
        }

        StockMovement movimiento = new StockMovement();
        movimiento.setBusinessId(negocioId);
        movimiento.setVariantId(variante.getId());
        movimiento.setSku(variante.getSku());
        movimiento.setType(StockMovementType.INGRESO);
        movimiento.setDelta(stock);
        movimiento.setReason("Carga inicial");
        movimiento.setCreatedAt(LocalDateTime.now());
        movimiento.setCreatedBy(usuarioActual());
        movementRepository.save(movimiento);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ProductVariant buscarVariante(Long negocioId, Long variantId) {
        ProductVariant variante = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("La variante indicada no existe"));
        TenantGuard.verificar(variante.getBusiness() != null ? variante.getBusiness().getId() : null);
        return variante;
    }

    private static StockMovementDto aDto(StockMovement movimiento, Integer stockAfter) {
        StockMovementDto dto = new StockMovementDto();
        dto.setId(movimiento.getId());
        dto.setVariantId(movimiento.getVariantId());
        dto.setSku(movimiento.getSku());
        dto.setType(movimiento.getType());
        dto.setDelta(movimiento.getDelta());
        dto.setUnitCost(movimiento.getUnitCost());
        dto.setReason(movimiento.getReason());
        dto.setSaleId(movimiento.getSaleId());
        dto.setCreatedAt(movimiento.getCreatedAt());
        dto.setCreatedBy(movimiento.getCreatedBy());
        dto.setStockAfter(stockAfter);
        return dto;
    }

    /** Email del usuario autenticado, o null si no hay sesión (tests o backfill). */
    private static String usuarioActual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return null;
    }

    private static String limpiarTexto(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
