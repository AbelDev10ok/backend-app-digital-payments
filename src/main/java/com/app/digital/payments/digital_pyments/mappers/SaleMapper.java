package com.app.digital.payments.digital_pyments.mappers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.FeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;

import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;


public class SaleMapper {


    public static SaleResponseDto convertirAVentaResponseDTO(Sale sale) {
        ClientDto clienteDTO = new ClientDto(
            sale.getClient().getId(),
            sale.getClient().getName(),
            sale.getClient().getTelefono(),
            sale.getClient().getEmail(),
            sale.getClient().getDireccion()
        );

        SaleResponseDto dto = new SaleResponseDto();
        dto.setId(sale.getId());
        dto.setClient(clienteDTO);
        dto.setDescriptionProduct(sale.getDescriptiononProduct());
        dto.setPriceTotal(sale.getPriceTotal());
        dto.setDateSale(sale.getDateSale());
        dto.setFinalPaymentDate(sale.getFinalPaymentDate());
        dto.setTypePayments(sale.getTypePayments());
        dto.setDaysLate(sale.getDaysLate());
        dto.setQuantityFees(sale.getQuiantityFees());
        dto.setAmountFe(sale.getAmountFe());
        dto.setCompleted(sale.isCompleted());

        dto.setQuantityFees(sale.getQuiantityFees());
        dto.setAdditionalFees(sale.getAdditionalFees());
        dto.setTotalFees(sale.getQuiantityFees() + sale.getAdditionalFees());
        dto.setRemainingAmount(sale.getRemainingAmount());


        dto.setFees(sale.getFees().stream()
            .sorted(Comparator.comparing(Fee::getExpirationDate))
            .map(fee -> convertToFeeDto(fee))
            .collect(Collectors.toList()));

        // Separar cuotas normales de adicionales (opcional, puedes ajustar si quieres solo relevantes)
        List<FeeDto> additionalFees = sale.getFees().stream()
            .filter(Fee::isAdditional)
            .map(fee -> convertToFeeDto(fee))
            .collect(Collectors.toList());

        dto.setAdditionalFees(additionalFees.size());
        dto.setAdditionalFeesList(additionalFees);

        // Calcular cuotas pagadas
        long paidFeesCount = sale.getFees().stream()
            .filter(Fee::getPaid)
            .count();
        dto.setPaidFeesCount((int) paidFeesCount);

        return dto;
    }
}