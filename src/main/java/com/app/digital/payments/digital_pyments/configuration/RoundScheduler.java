package com.app.digital.payments.digital_pyments.configuration;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Component
public class RoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundScheduler.class);

    private final IRoundServices roundServices;
    private final IBusinessRepository businessRepository;

    public RoundScheduler(IRoundServices roundServices, IBusinessRepository businessRepository) {
        this.roundServices = roundServices;
        this.businessRepository = businessRepository;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Argentina/Buenos_Aires")
    public void armarRondasDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Business> negocios = businessRepository.findAll();
        for (Business negocio : negocios) {
            try {
                roundServices.construirRonda(negocio.getId(), hoy);
            } catch (RuntimeException e) {
                log.warn("No se pudo armar la ronda del negocio {}: {}", negocio.getId(), e.getMessage());
            }
        }
    }
}
