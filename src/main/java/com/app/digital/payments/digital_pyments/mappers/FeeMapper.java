package com.app.digital.payments.digital_pyments.mappers;

// import java.time.LocalDate;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;

public class FeeMapper {

    public static FeeDto convertToFeeDto(Fee fee) {
        FeeDto dto = new FeeDto();
        dto.setId(fee.getId());
        dto.setSaleId(fee.getSale().getId());
        dto.setNumberFee(fee.getNumberFee());
        dto.setAmount(fee.getAmount());
        dto.setExpirationDate(fee.getExpirationDate());
        // dto.setOriginalExpirationDate(fee.getExpirationOrigialDate());
        dto.setPaid(fee.getPaid());
        dto.setPaymentDate(fee.getDatePayment());
        // dto.setPostponed(fee.getPostponed());

        dto.setStatus(fee.getPaid() ? "PAID" :  
                      "PENDING");
        
        // Información contextual
        dto.setProductDescription(fee.getSale().getDescriptionProduct());
        
        ClientDto clientDto = new ClientDto();
        clientDto.setId(fee.getSale().getClient().getId());
        clientDto.setName(fee.getSale().getClient().getName());
        // dto.setClient(clientDto);
        
        // Calcular estado
        dto.calculateStatus();
        
        return dto;
    }
}
