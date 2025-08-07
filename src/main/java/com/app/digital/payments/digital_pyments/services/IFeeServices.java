package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;

public interface IFeeServices {
    Fee getFeeById(Long feeId);        
     FeeDto registerPayment(Long feeId, Double amount);
     FeeDto postponeFee(Long feeId, LocalDate newDate);
    //  List<FeeDto> getPriorityFees();
     List<FeeDto> getFeeBySaleId(Long feeId) ;
    //  List<FeeDto> getFeesByStatus(FeeStatus status, LocalDate date);
    //  List<FeeDto> getOutstandingFeeByClientId(Long clientId);

}
