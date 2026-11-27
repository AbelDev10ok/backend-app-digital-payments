package com.app.digital.payments.digital_pyments.configuration;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IRoundServices;

/**
 * Job diario que arma la ronda de cobro de cada negocio.
 *
 * <p>
 * Corre a las 6:00 (hora argentina) y es idempotente: si la ronda del dia ya
 * existe no se reconstruye, asi que un reinicio del servidor no pisa el plan
 * que el cobrador ya empezo a trabajar. Un negocio que falla no corta al resto.
 * </p>
 *
 * <p>
 * Cada negocio se reintenta con backoff lineal antes de darse por vencido: la
 * mayoria de los fallos son transitorios (un timeout de base, un deadlock), y
 * un segundo intento los resuelve sin intervencion. El reintento es en el
 * mismo proceso, no en un cron aparte, porque el job corre una sola vez al dia
 * y esperar 24 horas por un fallo de dos segundos no tiene sentido.
 * </p>
 */
@Component
public class RoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundScheduler.class);

    private final IRoundServices roundServices;
    private final IBusinessRepository businessRepository;
    private final int intentos;
    private final long esperaBaseMs;

    public RoundScheduler(IRoundServices roundServices, IBusinessRepository businessRepository,
            @Value("${app.round.retry.attempts:3}") int intentos,
            @Value("${app.round.retry.backoff-ms:2000}") long esperaBaseMs) {
        this.roundServices = roundServices;
        this.businessRepository = businessRepository;
        this.intentos = Math.max(1, intentos);
        this.esperaBaseMs = Math.max(0, esperaBaseMs);
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Argentina/Buenos_Aires")
    public void armarRondasDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Business> negocios = businessRepository.findAll();
        for (Business negocio : negocios) {
            armarConReintentos(negocio.getId(), hoy);
        }
    }

    /**
     * Arma la ronda de un negocio reintentando ante fallos transitorios. El
     * ultimo intento loguea en error; los anteriores en warn con el numero de
     * intento para poder distinguir un fallo resuelto de uno persistente.
     */
    private void armarConReintentos(Long businessId, LocalDate hoy) {
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                roundServices.construirRonda(businessId, hoy);
                return;
            } catch (RuntimeException e) {
                if (intento == intentos) {
                    log.error("No se pudo armar la ronda del negocio {} tras {} intentos: {}", businessId, intentos,
                            e.getMessage());
                    return;
                }
                log.warn("Fallo al armar la ronda del negocio {} (intento {}/{}): {}", businessId, intento, intentos,
                        e.getMessage());
                if (!esperar(intento)) {
                    return;
                }
            }
        }
    }

    /**
     * Espera el backoff lineal. Devuelve false si el hilo fue interrumpido: en
     * ese caso se corta el reintento y se restaura el flag de interrupcion para
     * no tragarse el shutdown del servidor.
     */
    private boolean esperar(int intento) {
        if (esperaBaseMs == 0) {
            return true;
        }
        try {
            Thread.sleep(esperaBaseMs * intento);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
