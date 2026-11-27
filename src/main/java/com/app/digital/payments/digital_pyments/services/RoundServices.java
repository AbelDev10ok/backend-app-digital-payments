package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.CollectionRound;
import com.app.digital.payments.digital_pyments.models.CollectionRoundItem;
import com.app.digital.payments.digital_pyments.models.PaymentPromise;
import com.app.digital.payments.digital_pyments.models.dtos.CollectionRoundDto;
import com.app.digital.payments.digital_pyments.models.dtos.CollectionRoundItemDto;
import com.app.digital.payments.digital_pyments.models.dtos.CuotaListItemDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.ICollectionRoundItemRepository;
import com.app.digital.payments.digital_pyments.repositories.ICollectionRoundRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentPromiseRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IFeeQueryServices;
import com.app.digital.payments.digital_pyments.services.iservices.IRoundServices;
import com.app.digital.payments.digital_pyments.utils.PromiseStatus;
import com.app.digital.payments.digital_pyments.utils.RoundItemStatus;

import jakarta.persistence.EntityNotFoundException;

/**
 * Arma la ronda de cobro del dia: una sola lista por negocio, ordenada por
 * riesgo y con las paradas agrupadas por cliente + moneda (nunca se mezclan
 * monedas).
 *
 * <p>
 * El orden no es por monto: las monedas no se suman entre si. Primero van los
 * clientes que prometieron pagar y no cumplieron; despues, los de mayor atraso;
 * a igual atraso, por nombre (orden estable y explicable).
 * </p>
 */
@Service
public class RoundServices implements IRoundServices {

    private final ICollectionRoundRepository roundRepository;
    private final ICollectionRoundItemRepository itemRepository;
    private final IFeeQueryServices feeQueryServices;
    private final IPaymentPromiseRepository promiseRepository;
    private final IBusinessRepository businessRepository;

    RoundServices(ICollectionRoundRepository roundRepository,
            ICollectionRoundItemRepository itemRepository,
            IFeeQueryServices feeQueryServices,
            IPaymentPromiseRepository promiseRepository,
            IBusinessRepository businessRepository) {
        this.roundRepository = roundRepository;
        this.itemRepository = itemRepository;
        this.feeQueryServices = feeQueryServices;
        this.promiseRepository = promiseRepository;
        this.businessRepository = businessRepository;
    }

    @Override
    @Transactional
    public CollectionRoundDto getRondaDelDia(Long businessId, LocalDate fecha) {
        return construirRonda(businessId, fecha);
    }

    @Override
    @Transactional
    public CollectionRoundDto construirRonda(Long businessId, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        CollectionRound round = roundRepository.findByBusinessIdAndRoundDate(businessId, dia)
                .orElseGet(() -> crearRonda(businessId, dia));
        return toDto(round, dia);
    }

    @Override
    @Transactional
    public CollectionRoundDto marcarVisita(Long businessId, Long itemId, Boolean noEstaba, String note) {
        CollectionRoundItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Parada de ronda no encontrada con ID: " + itemId));
        CollectionRound round = item.getRound();
        // El tenant lo resuelve el controller con TenantContext; aca se compara
        // explicito para que el servicio no dependa del thread-local.
        if (businessId != null && round.getBusiness() != null && !businessId.equals(round.getBusiness().getId())) {
            throw new RuntimeException("Recurso no encontrado");
        }
        item.setNoEstaba(Boolean.TRUE.equals(noEstaba));
        item.setNote(normalizarNota(note));
        item.setUpdatedAt(LocalDateTime.now());
        return toDto(round, round.getRoundDate());
    }

    /**
     * Arma la ronda si todavia no existe. Bloquea la fila del negocio para que
     * dos procesos (job y consulta) no creen dos rondas para el mismo dia: el
     * segundo espera el lock y encuentra la que ya guardo el primero.
     */
    private CollectionRound crearRonda(Long businessId, LocalDate dia) {
        Business business = businessRepository.findByIdForUpdate(businessId)
                .orElseThrow(() -> new EntityNotFoundException("Negocio no encontrado con ID: " + businessId));
        return roundRepository.findByBusinessIdAndRoundDate(businessId, dia)
                .orElseGet(() -> guardarRonda(business, businessId, dia));
    }

    private CollectionRound guardarRonda(Business business, Long businessId, LocalDate dia) {
        List<CuotaListItemDto> cuotas = feeQueryServices.cuotasPendientesDelNegocio(businessId, dia);
        List<Grupo> grupos = agrupar(cuotas);
        Map<String, PaymentPromise> promesas = promesasPendientes(grupos);

        CollectionRound round = new CollectionRound(business, dia, LocalDateTime.now());
        int posicion = 1;
        for (Grupo grupo : ordenar(grupos, promesas, dia)) {
            round.agregarItem(new CollectionRoundItem(round, posicion++, grupo.clientId, grupo.clientName,
                    grupo.clientPhone, grupo.clientAddress, grupo.currency, grupo.montoCobrable,
                    grupo.diasAtraso, promesas.get(grupo.clave())));
        }
        return roundRepository.save(round);
    }

    private CollectionRoundDto toDto(CollectionRound round, LocalDate dia) {
        Map<String, BigDecimal> pendientes = pendientesPorGrupo(round.getBusiness().getId(), dia);

        CollectionRoundDto dto = new CollectionRoundDto();
        dto.setId(round.getId());
        dto.setFecha(round.getRoundDate());
        dto.setCreatedAt(round.getCreatedAt());
        for (CollectionRoundItem item : round.getItems()) {
            dto.getItems().add(toItemDto(item, pendientes.get(clave(item.getClientId(), item.getCurrency())), dia));
        }
        return dto;
    }

    /** Monto que sigue pendiente hoy por grupo: lo que ya no aparece esta cobrado. */
    private Map<String, BigDecimal> pendientesPorGrupo(Long businessId, LocalDate dia) {
        Map<String, BigDecimal> pendientes = new HashMap<>();
        for (Grupo grupo : agrupar(feeQueryServices.cuotasPendientesDelNegocio(businessId, dia))) {
            pendientes.put(grupo.clave(), grupo.montoCobrable);
        }
        return pendientes;
    }

    private CollectionRoundItemDto toItemDto(CollectionRoundItem item, BigDecimal pendiente, LocalDate dia) {
        CollectionRoundItemDto dto = new CollectionRoundItemDto();
        dto.setId(item.getId());
        dto.setPosition(item.getPosition());
        dto.setClientId(item.getClientId());
        dto.setClientName(item.getClientName());
        dto.setClientPhone(item.getClientPhone());
        dto.setClientAddress(item.getClientAddress());
        dto.setCurrency(item.getCurrency());
        dto.setAmount(item.getAmount());
        dto.setPendingAmount(pendiente);
        dto.setDaysOverdue(item.getDaysOverdue());
        dto.setPromiseId(item.getPromiseId());
        dto.setPromiseDate(item.getPromiseDate());
        dto.setPromiseAmount(item.getPromiseAmount());
        dto.setPromiseOverdue(item.getPromiseDate() != null && item.getPromiseDate().isBefore(dia));
        dto.setNote(item.getNote());
        dto.setEstado(estadoDe(item, pendiente).name());
        return dto;
    }

    private RoundItemStatus estadoDe(CollectionRoundItem item, BigDecimal pendiente) {
        if (pendiente == null) {
            return RoundItemStatus.COBRADO;
        }
        if (Boolean.TRUE.equals(item.getNoEstaba())) {
            return RoundItemStatus.NO_ESTABA;
        }
        return RoundItemStatus.PENDIENTE;
    }

    private List<Grupo> agrupar(List<CuotaListItemDto> cuotas) {
        Map<String, List<CuotaListItemDto>> porGrupo = new LinkedHashMap<>();
        for (CuotaListItemDto cuota : cuotas) {
            if (cuota.getClientId() == null) {
                continue;
            }
            porGrupo.computeIfAbsent(clave(cuota.getClientId(), cuota.getCurrency()), k -> new ArrayList<>())
                    .add(cuota);
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<String, List<CuotaListItemDto>> entrada : porGrupo.entrySet()) {
            List<CuotaListItemDto> delGrupo = entrada.getValue();
            CuotaListItemDto primero = delGrupo.get(0);
            long atraso = delGrupo.stream().map(CuotaListItemDto::getDiasAtraso).filter(Objects::nonNull)
                    .max(Long::compare).orElse(0L);
            grupos.add(new Grupo(primero.getClientId(), primero.getClientName(), primero.getClientPhone(),
                    primero.getClientAddress(), primero.getCurrency(), montoCobrable(delGrupo), atraso));
        }
        return grupos;
    }

    /**
     * Suma lo cobrable del grupo con el mismo tope que la agenda: por operacion
     * nunca se promete mas que la deuda de la venta.
     */
    private BigDecimal montoCobrable(List<CuotaListItemDto> cuotas) {
        Map<Long, BigDecimal> sumaPorVenta = new LinkedHashMap<>();
        Map<Long, BigDecimal> deudaPorVenta = new LinkedHashMap<>();
        for (CuotaListItemDto cuota : cuotas) {
            if (Boolean.TRUE.equals(cuota.getPagada())) {
                continue;
            }
            Long saleId = cuota.getSaleId();
            sumaPorVenta.merge(saleId, montoDe(cuota), BigDecimal::add);
            deudaPorVenta.putIfAbsent(saleId, cuota.getDeudaVenta());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entrada : sumaPorVenta.entrySet()) {
            BigDecimal suma = entrada.getValue();
            BigDecimal deuda = deudaPorVenta.get(entrada.getKey());
            total = total.add(deuda == null ? suma : suma.min(deuda.max(BigDecimal.ZERO)));
        }
        return total;
    }

    private BigDecimal montoDe(CuotaListItemDto cuota) {
        return cuota.getMonto() == null ? BigDecimal.ZERO : cuota.getMonto();
    }

    private Map<String, PaymentPromise> promesasPendientes(List<Grupo> grupos) {
        List<Long> clientIds = grupos.stream().map(grupo -> grupo.clientId).filter(Objects::nonNull).distinct()
                .toList();
        if (clientIds.isEmpty()) {
            return Map.of();
        }
        List<PaymentPromise> promesas = promiseRepository
                .findByClientIdInAndStatusOrderByPromisedDateAsc(clientIds, PromiseStatus.PENDING);
        Map<String, PaymentPromise> porGrupo = new HashMap<>();
        for (PaymentPromise promesa : promesas) {
            porGrupo.putIfAbsent(clave(promesa.getClient().getId(), promesa.getCurrency()), promesa);
        }
        return porGrupo;
    }

    private List<Grupo> ordenar(List<Grupo> grupos, Map<String, PaymentPromise> promesas, LocalDate dia) {
        return grupos.stream().sorted((a, b) -> {
            boolean vencidaA = promesaVencida(promesas.get(a.clave()), dia);
            boolean vencidaB = promesaVencida(promesas.get(b.clave()), dia);
            if (vencidaA != vencidaB) {
                return vencidaA ? -1 : 1;
            }
            int porAtraso = Long.compare(b.diasAtraso, a.diasAtraso);
            if (porAtraso != 0) {
                return porAtraso;
            }
            return compararNombres(a.clientName, b.clientName);
        }).toList();
    }

    private boolean promesaVencida(PaymentPromise promesa, LocalDate dia) {
        return promesa != null && promesa.getPromisedDate() != null && promesa.getPromisedDate().isBefore(dia);
    }

    private int compararNombres(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareToIgnoreCase(b);
    }

    private String normalizarNota(String note) {
        if (note == null) {
            return null;
        }
        String limpia = note.trim();
        return limpia.isEmpty() ? null : limpia;
    }

    private static String clave(Long clientId, String currency) {
        return clientId + ":" + (currency == null ? "" : currency);
    }

    private static final class Grupo {
        private final Long clientId;
        private final String clientName;
        private final String clientPhone;
        private final String clientAddress;
        private final String currency;
        private final BigDecimal montoCobrable;
        private final long diasAtraso;

        private Grupo(Long clientId, String clientName, String clientPhone, String clientAddress, String currency,
                BigDecimal montoCobrable, long diasAtraso) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.clientPhone = clientPhone;
            this.clientAddress = clientAddress;
            this.currency = currency;
            this.montoCobrable = montoCobrable;
            this.diasAtraso = diasAtraso;
        }

        private String clave() {
            return RoundServices.clave(clientId, currency);
        }
    }
}
