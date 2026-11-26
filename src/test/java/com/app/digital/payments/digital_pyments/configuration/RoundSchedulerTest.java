package com.app.digital.payments.digital_pyments.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IRoundServices;

/**
 * Job diario de la ronda: arma la ronda de cada negocio y un negocio que falla
 * no corta al resto.
 */
class RoundSchedulerTest {

    @Mock
    private IRoundServices roundServices;
    @Mock
    private IBusinessRepository businessRepository;

    private RoundScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new RoundScheduler(roundServices, businessRepository);
    }

    @Test
    void armaLaRondaDeCadaNegocio() {
        when(businessRepository.findAll()).thenReturn(List.of(negocio(1L), negocio(2L)));

        scheduler.armarRondasDelDia();

        verify(roundServices).construirRonda(eq(1L), any(LocalDate.class));
        verify(roundServices).construirRonda(eq(2L), any(LocalDate.class));
    }

    @Test
    void unNegocioQueFallaNoCortaAlResto() {
        when(businessRepository.findAll()).thenReturn(List.of(negocio(1L), negocio(2L), negocio(3L)));
        when(roundServices.construirRonda(eq(2L), any(LocalDate.class)))
                .thenThrow(new RuntimeException("base caida"));

        scheduler.armarRondasDelDia();

        verify(roundServices, times(1)).construirRonda(eq(1L), any(LocalDate.class));
        verify(roundServices, times(1)).construirRonda(eq(2L), any(LocalDate.class));
        verify(roundServices, times(1)).construirRonda(eq(3L), any(LocalDate.class));
    }

    @Test
    void sinNegociosNoLlamaAlServicio() {
        when(businessRepository.findAll()).thenReturn(List.of());

        scheduler.armarRondasDelDia();

        verify(roundServices, never()).construirRonda(any(), any());
    }

    private Business negocio(Long id) {
        Business business = new Business();
        business.setId(id);
        return business;
    }
}
