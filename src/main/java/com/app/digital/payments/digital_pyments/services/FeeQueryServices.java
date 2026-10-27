package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.CobrosHoyResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.CobrosHoyResponseDto.Resumen;
import com.app.digital.payments.digital_pyments.models.dtos.CuotaListItemDto;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IFeeQueryServices;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
import com.app.digital.payments.digital_pyments.utils.FeeStatus;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

import jakarta.persistence.criteria.Predicate;

@Service
public class FeeQueryServices implements IFeeQueryServices {

    private static final int MAX_ITEMS_COBROS_HOY = 200;

    private final IFeeRepository feeRepository;

    public FeeQueryServices(IFeeRepository feeRepository) {
        this.feeRepository = feeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CuotaListItemDto> getCuotas(FeeStatus estado, LocalDate desde, LocalDate hasta,
            Long clientId, Pageable pageable) {
        // POSTPONED no tiene representacion en el modelo (posponer ya mueve la fecha)
        if (estado == FeeStatus.POSTPONED) {
            throw new IllegalArgumentException("El filtro de cuotas pospuestas no esta disponible");
        }
        Pageable effective = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "expirationDate"))
                : pageable;
        return feeRepository.findAll(buildSpecification(estado, desde, hasta, clientId), effective)
                .map(fee -> toDto(fee, LocalDate.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public CobrosHoyResponseDto getCobrosDeHoy(LocalDate fecha) {
        LocalDate hoy = fecha != null ? fecha : LocalDate.now();
        CobrosHoyResponseDto response = new CobrosHoyResponseDto();
        response.setFecha(hoy);

        aplicarResumen(response.getHoy(), feeRepository.sumPendingFeesByExpirationDateGrouped(hoy));
        aplicarResumen(response.getVencidas(), feeRepository.sumOverdueFeesBeforeGrouped(hoy));

        Specification<Fee> spec = (root, query, cb) -> cb.and(
                cb.isFalse(root.get("paid")),
                cb.lessThanOrEqualTo(root.get("expirationDate"), hoy),
                cb.notEqual(root.get("sale").get("status"), SaleStatus.CANCELED));
        Page<CuotaListItemDto> page = feeRepository.findAll(spec,
                PageRequest.of(0, MAX_ITEMS_COBROS_HOY, Sort.by(Sort.Direction.ASC, "expirationDate")))
                .map(fee -> toDto(fee, hoy));
        response.setItems(page.getContent());
        return response;
    }

    private Specification<Fee> buildSpecification(FeeStatus estado, LocalDate desde, LocalDate hasta,
            Long clientId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDate hoy = LocalDate.now();

            // Una venta anulada es terminal: sus cuotas no se cobran ni se listan
            // (el backend rechaza registrarlas), así que no entran en la vista.
            predicates.add(cb.notEqual(root.get("sale").get("status"), SaleStatus.CANCELED));

            if (estado != null) {
                switch (estado) {
                    case PAID -> predicates.add(cb.isTrue(root.get("paid")));
                    case PENDING -> predicates.add(cb.isFalse(root.get("paid")));
                    case TODAY -> {
                        predicates.add(cb.isFalse(root.get("paid")));
                        predicates.add(cb.equal(root.get("expirationDate"), hoy));
                    }
                    case DELAYED -> {
                        predicates.add(cb.isFalse(root.get("paid")));
                        predicates.add(cb.lessThan(root.get("expirationDate"), hoy));
                    }
                    case UPCOMING -> {
                        predicates.add(cb.isFalse(root.get("paid")));
                        predicates.add(cb.greaterThan(root.get("expirationDate"), hoy));
                    }
                }
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expirationDate"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expirationDate"), hasta));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("sale").get("client").get("id"), clientId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CuotaListItemDto toDto(Fee fee, LocalDate hoy) {
        CuotaListItemDto dto = new CuotaListItemDto();
        Sale sale = fee.getSale();

        dto.setId(fee.getId());
        dto.setSaleId(sale.getId());

        Client client = sale.getClient();
        if (client != null) {
            dto.setClientId(client.getId());
            dto.setClientName(client.getName());
            dto.setClientPhone(client.getTelefono());
        }

        dto.setSaleDescription(sale.getDescriptionProduct());
        dto.setSaleKind(sale.getKind() == null ? null : sale.getKind().name());
        dto.setCurrency(sale.getCurrency());
        dto.setNumeroCuota(fee.getNumberFee());
        dto.setTotalCuotas(sale.getQuiantityFees());

        // Monto efectivo: sin monto fijado se usa el valor de cuota acordado
        BigDecimal monto = fee.getAmount();
        if (monto == null || monto.signum() <= 0) {
            monto = sale.getAmountFee();
        }
        dto.setMonto(monto);

        // Tope de lo cobrable en esta cuota: la deuda restante de la venta
        dto.setDeudaVenta(sale.getRemainingAmount());

        dto.setFechaVencimiento(fee.getExpirationDate());
        dto.setPagada(Boolean.TRUE.equals(fee.getPaid()));
        dto.setFechaPago(fee.getLastPaymentAt() != null ? fee.getLastPaymentAt().toLocalDate() : null);
        dto.setFechaPagoHora(fee.getLastPaymentAt());
        dto.setPaymentMethod(fee.getPaymentMethod() != null ? fee.getPaymentMethod().name() : null);

        if (Boolean.TRUE.equals(fee.getPaid())) {
            dto.setEstado("PAID");
            long atrasoAlPagar = fee.getLastPaymentAt() == null ? 0
                    : ChronoUnit.DAYS.between(fee.getExpirationDate(), fee.getLastPaymentAt().toLocalDate());
            dto.setDiasAtraso(Math.max(atrasoAlPagar, 0));
        } else {
            long atraso = ChronoUnit.DAYS.between(fee.getExpirationDate(), hoy);
            dto.setDiasAtraso(Math.max(atraso, 0));
            if (atraso > 0) {
                dto.setEstado("DELAYED");
            } else if (atraso == 0) {
                dto.setEstado("TODAY");
            } else {
                dto.setEstado("UPCOMING");
            }
        }
        return dto;
    }

    private void aplicarResumen(Resumen resumen, List<Object[]> filas) {
        if (filas == null) {
            return;
        }
        // Filas agrupadas por moneda: (moneda, cantidad, monto). Nunca se suman
        // montos de monedas distintas; la cantidad sí es global.
        for (Object[] fila : filas) {
            resumen.setCantidad(resumen.getCantidad() + (fila[1] != null ? ((Number) fila[1]).longValue() : 0));
            CurrencyUtils.acumular(resumen.getMonto(), (String) fila[0],
                    fila[2] instanceof BigDecimal monto ? monto : null);
        }
    }
}
