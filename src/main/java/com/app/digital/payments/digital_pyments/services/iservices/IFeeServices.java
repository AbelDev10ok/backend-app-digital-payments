package com.app.digital.payments.digital_pyments.services.iservices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.PaymentDto;

public interface IFeeServices {
     Fee getFeeById(Long feeId);        
     FeeDto registerPayment(Long feeId, BigDecimal amount, LocalDate date, String paymentMethod);
     FeeDto postponeFee(Long feeId, LocalDate newDate,LocalDate newDatePayment, BigDecimal amount);
     // Reprogramación en bloque: corre la cuota nombrada y todas las impagas que le
     // siguen, en períodos de la frecuencia (periods) o días naturales (days).
     // Exactamente uno de los dos, siempre mayor a 0; devuelve las cuotas movidas.
     List<FeeDto> reprogramFee(Long feeId, Integer periods, Integer days);
    //  List<FeeDto> getPriorityFees();
     List<FeeDto> getFeeBySaleId(Long feeId) ;
     // Historial de cobros de una cuota, con fecha y hora
     List<PaymentDto> getPaymentsByFeeId(Long feeId);
    //  List<FeeDto> getFeesByStatus(FeeStatus status, LocalDate date);
    //  List<FeeDto> getOutstandingFeeByClientId(Long clientId);

}
