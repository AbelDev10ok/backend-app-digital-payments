package com.app.digital.payments.digital_pyments.services.iservices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;

public interface IFeeServices {
     Fee getFeeById(Long feeId);        
     FeeDto registerPayment(Long feeId, BigDecimal amount, LocalDate date, String paymentMethod);
     FeeDto postponeFee(Long feeId, LocalDate newDate,LocalDate newDatePayment, BigDecimal amount);
    //  List<FeeDto> getPriorityFees();
     List<FeeDto> getFeeBySaleId(Long feeId) ;
    //  List<FeeDto> getFeesByStatus(FeeStatus status, LocalDate date);
    //  List<FeeDto> getOutstandingFeeByClientId(Long clientId);
    void deleteFee(Long feeId);

}
