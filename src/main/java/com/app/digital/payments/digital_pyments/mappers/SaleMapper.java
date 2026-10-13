package com.app.digital.payments.digital_pyments.mappers;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.ClientDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductTypeDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;

import static com.app.digital.payments.digital_pyments.mappers.FeeMapper.convertToFeeDto;


public class SaleMapper {


    public static SaleResponseDto convertirAVentaResponseDTO(Sale sale) {
        ClientDto clienteDTO = new ClientDto(
            sale.getClient().getId(),
            sale.getClient().getName(),
            sale.getClient().getTelefono(),
            sale.getClient().getEmail(),
            sale.getClient().getDireccion(),
            sale.getClient().isSeller(),
            sale.getClient().getSeller() != null ? sale.getClient().getSeller().getName() : null,
            sale.getClient().getDni()        );

        SaleResponseDto dto = new SaleResponseDto();
        dto.setId(sale.getId());
        dto.setClient(clienteDTO);
        dto.setDescriptionProduct(sale.getDescriptionProduct());
        dto.setPriceTotal(sale.getPriceTotal());
        dto.setDateSale(sale.getDateCreation());
        dto.setFinalPaymentDate(sale.getFinalPaymentDate());
        dto.setTypePayments(sale.getTypePayments());
        dto.setQuantityFees(sale.getQuiantityFees());
        dto.setAmountFee(sale.getAmountFee());
        // dto.setCompleted(sale.isCompleted());
        dto.setCost(sale.getCost()); // Agregar costo del producto/servicio
        dto.setInterestRate(sale.getInterestRate());
        dto.setQuantityFees(sale.getQuiantityFees());
        // dto.setAdditionalFees(sale.getAdditionalFees());
        dto.setTotalFees(sale.getFees().size());
        dto.setRemainingAmount(sale.getRemainingAmount());
        dto.setStatus(sale.getStatus().name());
        dto.setRealFinalPayment(sale.getRealFinalDate());
        dto.setKind(sale.getKind());
        dto.setQuantity(sale.getQuantity());


                // Aquí está el cambio principal
        if (sale.getProductType() != null) {
            ProductTypeDto productTypeDto = new ProductTypeDto(
                sale.getProductType().getId(),
                sale.getProductType().getName()
                // sale.getProductType().getDescription()
            );
            dto.setProductType(productTypeDto);
        }

        if (sale.getProduct() != null) {
            ProductDto productDto = new ProductDto(
                sale.getProduct().getId(),
                sale.getProduct().getName(),
                sale.getProduct().getPrice(),
                sale.getProduct().getStock(),
                sale.getProduct().getProductType() != null ? sale.getProduct().getProductType().getId() : null,
                sale.getProduct().getProductType() != null ? sale.getProduct().getProductType().getName() : null
            );
            dto.setProduct(productDto);
        }


        dto.setFees(sale.getFees().stream()
            .sorted(Comparator.comparing(Fee::getExpirationDate))
            .map(fee -> convertToFeeDto(fee))
            .collect(Collectors.toList()));

        // Calcular cuotas pagadas
        long paidFeesCount = sale.getFees().stream()
            .filter(Fee::getPaid)
            .count();
        dto.setPaidFeesCount((int) paidFeesCount);

        // Monto ya cobrado antes de la anulación (suma de cuotas pagadas)
        BigDecimal collectedAmount = sale.getFees().stream()
            .filter(Fee::getPaid)
            .map(Fee::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setCollectedAmount(collectedAmount);

        // Lo devuelto al cliente al anular (0 si el monto cobrado se retiene)
        dto.setRefundAmount(sale.getRefundAmount() != null ? sale.getRefundAmount() : BigDecimal.ZERO);

        return dto;
    }
}