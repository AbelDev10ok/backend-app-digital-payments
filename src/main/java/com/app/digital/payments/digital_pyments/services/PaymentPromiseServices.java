package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.mappers.PaymentPromiseMapper;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.PaymentPromise;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentPromiseRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IPaymentPromiseRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IPaymentPromiseServices;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
import com.app.digital.payments.digital_pyments.utils.PromiseStatus;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

import jakarta.persistence.EntityNotFoundException;

/**
 * Promesas de pago: la gestión comercial de "te pago tal día". No mueve plata
 * ni toca cuotas; sólo deja registrado el compromiso para que la agenda sepa
 * distinguir una deuda reclamada de una deuda con fecha prometida.
 */
@Service
public class PaymentPromiseServices implements IPaymentPromiseServices {

    private final IPaymentPromiseRepository promiseRepository;
    private final IClientRepository clientRepository;
    private final ISaleRepository saleRepository;
    private final IBusinessRepository businessRepository;

    PaymentPromiseServices(IPaymentPromiseRepository promiseRepository, IClientRepository clientRepository,
            ISaleRepository saleRepository, IBusinessRepository businessRepository) {
        this.promiseRepository = promiseRepository;
        this.clientRepository = clientRepository;
        this.saleRepository = saleRepository;
        this.businessRepository = businessRepository;
    }

    @Override
    @Transactional
    public PaymentPromiseDto crear(Long clientId, PaymentPromiseRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("La promesa de pago es obligatoria");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("La moneda es obligatoria");
        }
        String moneda = request.getCurrency().trim().toUpperCase();
        if (!CurrencyUtils.esSoportada(moneda)) {
            throw new IllegalArgumentException("La moneda no está soportada: " + moneda);
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("El monto prometido debe ser mayor a cero");
        }
        if (request.getPromisedDate() == null) {
            throw new IllegalArgumentException("La fecha prometida es obligatoria");
        }
        LocalDate hoy = LocalDate.now();
        if (request.getPromisedDate().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha prometida no puede ser anterior a hoy");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + clientId));
        TenantGuard.verificar(client.getBusiness() != null ? client.getBusiness().getId() : null);

        // Una promesa sólo tiene sentido si el cliente debe plata en esa moneda
        BigDecimal deuda = deudaDelCliente(clientId, moneda);
        if (deuda.signum() <= 0) {
            throw new IllegalArgumentException("El cliente no tiene deuda pendiente en " + moneda);
        }
        if (request.getAmount().compareTo(deuda) > 0) {
            throw new IllegalArgumentException("El monto prometido no puede superar la deuda del cliente en " + moneda);
        }

        Long negocioId = TenantContext.requireBusinessId();
        Business business = businessRepository.getReferenceById(negocioId);

        PaymentPromise promise = new PaymentPromise(business, client, moneda, request.getAmount(),
                request.getPromisedDate(), normalizarNota(request.getNote()), LocalDateTime.now());
        promiseRepository.save(promise);
        return PaymentPromiseMapper.toDto(promise, hoy);
    }

    @Override
    @Transactional
    public PaymentPromiseDto cancelar(Long promiseId) {
        PaymentPromise promise = buscar(promiseId);
        if (promise.getStatus() != PromiseStatus.PENDING) {
            throw new IllegalStateException("Sólo se puede cancelar una promesa pendiente");
        }
        promise.setStatus(PromiseStatus.CANCELLED);
        promise.setResolvedAt(LocalDateTime.now());
        return PaymentPromiseMapper.toDto(promise, LocalDate.now());
    }

    @Override
    @Transactional
    public PaymentPromiseDto cumplir(Long promiseId) {
        PaymentPromise promise = buscar(promiseId);
        if (promise.getStatus() != PromiseStatus.PENDING) {
            throw new IllegalStateException("Sólo se puede cumplir una promesa pendiente");
        }
        promise.setStatus(PromiseStatus.FULFILLED);
        promise.setResolvedAt(LocalDateTime.now());
        return PaymentPromiseMapper.toDto(promise, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPromiseDto> listarPorCliente(Long clientId, String currency) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + clientId));
        TenantGuard.verificar(client.getBusiness() != null ? client.getBusiness().getId() : null);

        List<PaymentPromise> promesas = (currency == null || currency.isBlank())
                ? promiseRepository.findByClientIdOrderByPromisedDateDesc(clientId)
                : promiseRepository.findByClientIdAndCurrencyOrderByPromisedDateDesc(clientId,
                        currency.trim().toUpperCase());

        LocalDate hoy = LocalDate.now();
        return promesas.stream().map(promesa -> PaymentPromiseMapper.toDto(promesa, hoy)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPromiseDto> listarVigentesDeClientes(Collection<Long> clientIds) {
        // Sin clientes no hay promesas: se evita la consulta con colección vacía
        if (clientIds == null || clientIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = clientIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        return promiseRepository.findByClientIdInAndStatusOrderByPromisedDateAsc(ids, PromiseStatus.PENDING)
                .stream()
                .map(promesa -> PaymentPromiseMapper.toDto(promesa, hoy))
                .toList();
    }

    private PaymentPromise buscar(Long promiseId) {
        PaymentPromise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new EntityNotFoundException("Promesa de pago no encontrada con ID: " + promiseId));
        TenantGuard.verificar(promise.getBusiness() != null ? promise.getBusiness().getId() : null);
        return promise;
    }

    /**
     * Deuda del cliente en una moneda: saldo restante de sus operaciones no
     * anuladas. El saldo se recalcula antes de sumar para no depender de un
     * caché que pueda estar viejo.
     */
    private BigDecimal deudaDelCliente(Long clientId, String moneda) {
        return saleRepository.findByClientIdAndStatusNot(clientId, SaleStatus.CANCELED).stream()
                .filter(sale -> sale.getStatus() != SaleStatus.CANCELED)
                .filter(sale -> moneda.equalsIgnoreCase(sale.getCurrency()))
                .map(Sale::recalcularSaldo)
                .filter(saldo -> saldo != null && saldo.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizarNota(String note) {
        if (note == null) {
            return null;
        }
        String limpia = note.trim();
        return limpia.isEmpty() ? null : limpia;
    }
}
