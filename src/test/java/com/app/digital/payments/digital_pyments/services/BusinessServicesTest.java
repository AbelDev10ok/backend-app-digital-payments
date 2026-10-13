package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessMeDto;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessUpdateRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

class BusinessServicesTest {

    private static final Long BUSINESS_ID = 7L;

    @Mock
    private IBusinessRepository businessRepository;

    private BusinessServices businessServices;

    private Business business;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        businessServices = new BusinessServices(businessRepository);
        TenantContext.setBusinessId(BUSINESS_ID);

        business = new Business("Mi Negocio", "ARS");
        business.setId(BUSINESS_ID);
        business.setDefaultInterestRate(null);
        business.setDefaultPaymentFrequency(null);
        business.setPlanStatus("TRIAL");
        business.setTrialEndsAt(LocalDate.now().plusDays(30));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getMyBusinessDevuelveLosDatosYElEstadoDelPlan() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));

        BusinessMeDto dto = businessServices.getMyBusiness();

        assertEquals(BUSINESS_ID, dto.getId());
        assertEquals("Mi Negocio", dto.getName());
        assertEquals("ARS", dto.getCurrency());
        assertEquals("TRIAL", dto.getPlanStatus());
        assertEquals(business.getTrialEndsAt(), dto.getTrialEndsAt());
        assertNull(dto.getDefaultInterestRate());
        assertNull(dto.getDefaultPaymentFrequency());
    }

    @Test
    void getMyBusinessFallaSinNegocioAutenticado() {
        TenantContext.clear();

        assertThrows(IllegalStateException.class, () -> businessServices.getMyBusiness());
    }

    @Test
    void getMyBusinessFallaSiElNegocioNoExiste() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> businessServices.getMyBusiness());
    }

    @Test
    void updateMyBusinessActualizaNombreMonedaTasaYFrecuencia() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(business));
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        BusinessUpdateRequestDto request = new BusinessUpdateRequestDto();
        request.setName("Cobros del Sur");
        request.setCurrency("pyg"); // se normaliza a mayusculas
        request.setDefaultInterestRate(new BigDecimal("12.5"));
        request.setDefaultPaymentFrequency("mensual"); // se normaliza a MENSUAL

        BusinessMeDto dto = businessServices.updateMyBusiness(request);

        assertEquals("Cobros del Sur", business.getName());
        assertEquals("PYG", business.getCurrency());
        assertEquals(new BigDecimal("12.5"), dto.getDefaultInterestRate());
        assertEquals("MENSUAL", dto.getDefaultPaymentFrequency());
        assertEquals("TRIAL", dto.getPlanStatus());
    }
}
