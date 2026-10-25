package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessMeDto;
import com.app.digital.payments.digital_pyments.models.dtos.BusinessUpdateRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
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
        // Si el campo viene informado (aunque sea vacío), se actualiza: así el
        // usuario puede borrar el CUIT, la dirección o el teléfono.
        if (request.getCuit() != null) {
            business.setCuit(limpiarOpcional(request.getCuit()));
        }
        if (request.getAddress() != null) {
            business.setAddress(limpiarOpcional(request.getAddress()));
        }
        if (request.getPhone() != null) {
            business.setPhone(limpiarOpcional(request.getPhone()));
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            String nueva = request.getCurrency().trim().toUpperCase();
            // Solo se admiten las monedas soportadas, salvo que sea la que el
            // negocio ya tiene (dato legado): así puede guardar el resto de los
            // datos sin quedar trabado por una moneda que ya no se ofrece.
            if (!CurrencyUtils.esSoportada(nueva)
                    && !nueva.equals(CurrencyUtils.orDefault(business.getCurrency(), null))) {
                throw new IllegalArgumentException("La moneda debe ser ARS o USD");
            }
            business.setCurrency(nueva);
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
        dto.setCuit(business.getCuit());
        dto.setAddress(business.getAddress());
        dto.setPhone(business.getPhone());
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

    private String limpiarOpcional(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
