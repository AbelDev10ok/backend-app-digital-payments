package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessMeDto;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessUpdateRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Lectura y actualizacion del negocio propio (tenant autenticado).
 * El id del negocio SIEMPRE sale del contexto de seguridad; jamas del request.
 */
@Service
public class BusinessServices {

    private final IBusinessRepository businessRepository;

    public BusinessServices(IBusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public BusinessMeDto getMyBusiness() {
        Business business = findOwnBusiness();
        return toDto(business);
    }

    @Transactional
    public BusinessMeDto updateMyBusiness(BusinessUpdateRequestDto request) {
        Business business = findOwnBusiness();

        if (request.getName() != null && !request.getName().isBlank()) {
            business.setName(request.getName().trim());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            business.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        // La tasa solo se actualiza si viene informada (null = no tocar)
        if (request.getDefaultInterestRate() != null) {
            business.setDefaultInterestRate(request.getDefaultInterestRate());
        }
        if (request.getDefaultPaymentFrequency() != null && !request.getDefaultPaymentFrequency().isBlank()) {
            business.setDefaultPaymentFrequency(
                    Payments.fromString(request.getDefaultPaymentFrequency()).name());
        }

        Business saved = businessRepository.save(business);
        return toDto(saved);
    }

    private Business findOwnBusiness() {
        Long businessId = TenantContext.requireBusinessId();
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalStateException("El negocio autenticado no existe"));
    }

    private BusinessMeDto toDto(Business business) {
        BusinessMeDto dto = new BusinessMeDto();
        dto.setId(business.getId());
        dto.setName(business.getName());
        dto.setCurrency(business.getCurrency());
        BigDecimal rate = business.getDefaultInterestRate();
        dto.setDefaultInterestRate(rate);
        dto.setDefaultPaymentFrequency(business.getDefaultPaymentFrequency());
        dto.setPlanStatus(business.getPlanStatus());
        dto.setTrialEndsAt(business.getTrialEndsAt());
        dto.setSubscriptionUntil(business.getSubscriptionUntil());
        dto.setCreatedAt(business.getCreatedAt() == null ? null : business.getCreatedAt().toLocalDate());
        return dto;
    }
}
