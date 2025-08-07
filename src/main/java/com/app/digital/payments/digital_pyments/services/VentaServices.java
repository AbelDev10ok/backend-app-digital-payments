package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.SaleOldRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;

import static com.app.digital.payments.digital_pyments.mappers.SaleMapper.convertirAVentaResponseDTO;;

@Service
public class VentaServices implements IVentaServices{

    @Autowired
    private ISaleRepository salesRepository;
    @Autowired
    private IClientRepository clientRepository;
    @Autowired
    private IFeeRepository feeRepository;
    // @Autowired
    // private IVentaServices salesServices;

    // prestamo antiguo, que no olvidamos de crear, puede tener cuotas pagadas o no
    // si tiene cuotas pagadas, se calcula el monto restante
    @Transactional
    @Override
    public SaleResponseDto createPrestamoOld(SaleOldRequestDto saleDto, LocalDate date) throws Exception { 
        // Validaciones de negocio
        if (saleDto.getQuantityFees() <= 0) {
            throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a 0");
        }
        // if (saleDto.getPriceTotal() <= 0) {
        //     throw new IllegalArgumentException("El monto total debe ser positivo");
        // }

        Client client = clientRepository.findById(saleDto.getClientId())
                .orElseThrow(() -> new Exception("Cliente no encontrado"));

        Sale newSale = new Sale();
        newSale.setClient(client);

        newSale.setDescriptionProduct("PRESTAMO");  
        // mejor calculemos el precio total basado en la cantidad de cuotas y el monto de la primera cuota
        Double totalPrice = saleDto.getQuantityFees() * saleDto.getAmountFee();
        // newSale.setPriceTotal( saleDto.getQuantityFees() * saleDto.getAmount());
        newSale.setDateCreation(date);

        // si ya tenia cuotas pagadas en caso de que sea una venta existente
        if (saleDto.getPaidFees() != null && !saleDto.getPaidFees().isEmpty()) {
            List<Fee> paidFees = saleDto.getPaidFees().stream()
                .map(paidFee -> {
                    Fee fee = new Fee();
                    fee.setAmount(paidFee.getAmount());
                    fee.setSale(newSale);
                    // como le asigno un numero que no tenga conflictos con las cuotas existentes?
                    fee.setNumberFee(newSale.getFees().size() + 1);
                    //-------- 
                    fee.setDatePayment(paidFee.getDatePayment());
                    fee.setExpirationDate(paidFee.getDatePayment()); // Asignar la misma fecha de pago como fecha de vencimiento
                    fee.setPaid(true);
                    return fee;
                }).collect(Collectors.toList());
            newSale.setFees(paidFees);
            // calcular el monto restante
            double totalPaid = paidFees.stream()
                .mapToDouble(Fee::getAmount)
                .sum();
            newSale.setRemainingAmount(totalPrice - totalPaid);
        } else {
            newSale.setRemainingAmount(totalPrice); // Inicializa con el total
        }

        // Configuración de la venta
        Payments paymentType = Payments.valueOf(saleDto.getPayments().toUpperCase());
        newSale.setTypePayments(paymentType);
        newSale.setQuiantityFees(saleDto.getQuantityFees());
        newSale.setAmountFee(saleDto.getAmountFee());
        newSale.setFinalPaymentDate(calculateFinalPaymentDate(
            saleDto.getPaidFees().get(0).getDatePayment(),
            newSale.getTypePayments(),
            newSale.getQuiantityFees()
        ));
        newSale.setDaysLate(0);
        newSale.setCompleted(false);

        //utilizo la primer cuota pagada
        LocalDate firstFeeDate = saleDto.getPaidFees().get(saleDto.getPaidFees().size()-1).getDatePayment();

        List <Fee> fees = new ArrayList<>();
        // Si quedan cuotas, crea la siguiente
        if (newSale.getRemainingAmount() > 0) {
            Fee siguienteCuota = new Fee();
            siguienteCuota.setSale(newSale);
            siguienteCuota.setNumberFee(2);
            siguienteCuota.setExpirationDate(calculateNextDate(firstFeeDate, newSale.getTypePayments()));
            siguienteCuota.setPaid(false);
            fees.add(siguienteCuota);
        }
    
        // Asignar cuotas a la venta
        newSale.getFees().addAll(fees);
        Sale savedSale = salesRepository.save(newSale);
        return convertirAVentaResponseDTO(savedSale);

    }

    @Transactional
    @Override
    public SaleResponseDto createPrestamo(SaleRequestDto saleDto, String productType) throws Exception {
        
        Client client = clientRepository.findById(saleDto.getClienteId())
                .orElseThrow(() -> new Exception("Cliente no encontrado"));

        Sale newSale = new Sale();
        newSale.setClient(client);
        newSale.setDescriptionProduct(productType.equals("PRESTAMO") ? "PRESTAMO" : saleDto.getDescriptionProduct());
        Double totalPrice = saleDto.getCantidadCuotas() * saleDto.getAmountFee();
        newSale.setPriceTotal(saleDto.getAmountFee() * saleDto.getQuantityFees());
        newSale.setCost(saleDto.getCost()); // Costo del producto/servicio
        newSale.setProductType(productType);

        newSale.setDateCreation(LocalDate.now());

        // validar que la primer cuota no sea anterior a la fecha de creacion del prestamos
        if (saleDto.getFirstFeeDate() != null && saleDto.getFirstFeeDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la primera cuota no puede ser anterior a la fecha del prestamo");
        }

        // si no tiene fecha de primera cuota, se usa la fecha actual
        // si tiene fecha de primera cuota, se usa esa
        LocalDate firstFeeDate = saleDto.getFirstFeeDate()!= null ? saleDto.getFirstFeeDate() : LocalDate.now();


        // Validación de negocio: cantidad de cuotas debe ser mayor a 0
        if (saleDto.getCantidadCuotas() <= 0) {
            throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a 0");
        }

        // Configuración de la venta
        Payments paymentType = Payments.valueOf(saleDto.getPayments().toUpperCase());
        newSale.setTypePayments(paymentType);
        newSale.setQuiantityFees(saleDto.getCantidadCuotas());
        newSale.setAmountFee(saleDto.getAmountFee());

        newSale.setRemainingAmount(totalPrice); // Inicializa con el total

        
        // Configuración de fechas y atrasos
        newSale.setFinalPaymentDate(calculateFinalPaymentDate(
            firstFeeDate,
            newSale.getTypePayments(),
            newSale.getQuiantityFees()
        ));
        newSale.setDaysLate(0);
        newSale.setCompleted(false);

    // Solo crear la primera cuota
    List<Fee> fees = new ArrayList<>();
    Fee primeraCuota = new Fee();
    primeraCuota.setSale(newSale);
    primeraCuota.setNumberFee(1);
    primeraCuota.setExpirationDate(firstFeeDate);
    primeraCuota.setPaid(false);

    // Si se paga la primera cuota al crear la venta
    if (saleDto.getPayFirstFee()!= null) {
        primeraCuota.setPaid(true);
        primeraCuota.setDatePayment(LocalDate.now());
        Double montoPagado = saleDto.getFirstFeeAmount() != null
            ? saleDto.getFirstFeeAmount()
            : totalPrice / saleDto.getCantidadCuotas();
        primeraCuota.setAmount(montoPagado);
        newSale.deductFromRemainingAmount(montoPagado);

        // Si quedan cuotas, crea la siguiente
        if (newSale.getRemainingAmount() > 0) {
            Fee siguienteCuota = new Fee();
            siguienteCuota.setSale(newSale);
            siguienteCuota.setNumberFee(2);
            siguienteCuota.setExpirationDate(calculateNextDate(firstFeeDate, newSale.getTypePayments()));
            siguienteCuota.setPaid(false);
            fees.add(siguienteCuota);
        }
    }
        fees.add(primeraCuota);
        // Asignar cuotas a la venta

        newSale.setFees(fees);
        
        Sale savedSale = salesRepository.save(newSale);
        return convertirAVentaResponseDTO(savedSale);
    }

    // Calcula la fecha final de pago según la frecuencia y cantidad de cuotas
    // Utiliza un switch para determinar la periodicidad
    private LocalDate calculateFinalPaymentDate(LocalDate startDate, 
                                                Payments frequency, 
                                                int installments) {
            switch (frequency) {
                case SEMANAL: return startDate.plusWeeks(installments);
                case QUINCENAL: return startDate.plusWeeks(installments * 2);
                case MENSUAL: return startDate.plusMonths(installments);
                default: return startDate;
            }
    }

    public int calculateGlobalDelay(Long saleId) {
        Sale sale = salesRepository.findById(saleId)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        

        // si ya pasó la fecha final de pago, calculo los días de atraso
        if(LocalDate.now().isAfter(sale.getFinalPaymentDate())) {
                return (int) ChronoUnit.DAYS.between(
                    sale.getFinalPaymentDate(), 
                    LocalDate.now()
            );
        }
        return 0;
    }

    // Marca una venta o prestamo como completada, actualizando la fecha final y los días de atraso
    // Si la venta ya está completada, no hace nada
    @Transactional
    @Override
    public void markPrestamoAsCompleted(Long saleId, Double amountPaid) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (sale.isCompleted()) {
            throw new IllegalStateException("La venta ya está completada");
        }


        // Solo marcar como completada si ya no queda saldo pendiente
        if (sale.getRemainingAmount() >= 0 && !sale.isCompleted()) {

            
            if (amountPaid != null && amountPaid > 0) {
                sale.deductFromRemainingAmount(amountPaid);
            }

            Fee lastFee = sale.getFees().stream()
                .filter(fee -> fee.getPaid() == null || !fee.getPaid())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay cuotas pendientes para completar la venta"));
            
            Fee updatedFee = feeRepository.findById(lastFee.getId()).orElseThrow();
            System.out.println("Paid: " + updatedFee.getPaid() + ", Amount: " + updatedFee.getAmount());

            feeRepository.findById(lastFee.getId())
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));

            lastFee.setPaid(true);
            lastFee.setDatePayment(LocalDate.now());
            lastFee.setAmount(sale.getAmountFee());
            feeRepository.save(lastFee);

            sale.setRealFinalDate(LocalDate.now());
            int finalDelay = calculateGlobalDelay(saleId);
            sale.setCompleted(true);
            sale.setRealFinalDate(LocalDate.now());
            sale.setDaysLate(finalDelay);
            salesRepository.save(sale);
        } else {
            // Si aún queda saldo, solo guarda el descuento
            salesRepository.save(sale);
        }
    // Si ya está completada, no hacemos nada
    }

    // Calcula la siguiente fecha de pago según la frecuencia
    // Utiliza un switch para determinar la periodicidad
    private LocalDate calculateNextDate(LocalDate date, Payments frequency) {
        switch (frequency) {
            case SEMANAL: return date.plusWeeks(1);
            case QUINCENAL: return date.plusWeeks(2);
            case MENSUAL: return date.plusMonths(1);
            default: return date;
        }
    }

    // Modificamos una venta existente
    // Validaciones: venta existente, cliente válido, monto total positivo, cuotas válidas
    @Transactional
    @Override
    public void updatePrestamo(SaleUpdateRequestDto sale, Long id, LocalDate date) throws Exception {
    // 1. Buscar la venta existente
    Sale existingSale = salesRepository.findById(id)
        .orElseThrow(() -> new Exception("Sale not found"));

    // 2. Buscar el cliente
    Client client = clientRepository.findById(sale.getClientId())
        .orElseThrow(() -> new Exception("Client not found"));

    // 3. Actualizar datos básicos
    existingSale.setClient(client);

    // 4. Calcular el total pagado hasta ahora
    Double totalPagado = calculateTotalPaidFeesForSaleUsingStream(existingSale);

    // 5. Calcular el nuevo precio total
    Double nuevoPriceTotal = sale.getQuantityFees() * sale.getAmountFee();

    // 6. Validar que el nuevo precio total no sea menor al total pagado
    if (nuevoPriceTotal < totalPagado) {
        throw new IllegalArgumentException("El nuevo monto total no puede ser menor al total ya pagado (" + totalPagado + ")");
    }

    // 7. Actualizar el precio total
    existingSale.setPriceTotal(nuevoPriceTotal);

    // 8. Actualizar la fecha de creación si corresponde
    existingSale.setDateCreation(date != null ? date : existingSale.getDateCreation());

    // 9. Actualizar tipo de pago y cantidad de cuotas
    Payments paymentType = Payments.valueOf(sale.getPayments().toUpperCase());
    existingSale.setTypePayments(paymentType);
    existingSale.setQuiantityFees(sale.getQuantityFees());

    // 10. Calcular y actualizar el monto de la cuota usando BigDecimal
    BigDecimal montoCuota = BigDecimal.valueOf(nuevoPriceTotal)
        .divide(BigDecimal.valueOf(sale.getQuantityFees()), 2, BigDecimal.ROUND_HALF_UP);
    existingSale.setAmountFee(montoCuota.doubleValue());

    // 11. Actualizar la fecha final de pago
    existingSale.setFinalPaymentDate(
        calculateFinalPaymentDate(existingSale.getDateCreation(), paymentType, sale.getQuantityFees())
    );

    // 12. Recalcular la deuda restante
    Double newRemainingAmount = nuevoPriceTotal - totalPagado;
    if (newRemainingAmount < 0) {
        existingSale.setRemainingAmount(0.0);
    } else {
        existingSale.setRemainingAmount(newRemainingAmount);
    }

    // 13. Actualizar estado de completado
    existingSale.setCompleted(existingSale.getRemainingAmount() <= 0);

    // 14. Recalcular días de atraso
    updateSaleGlobalDelay(existingSale.getId());

    // 15. Guardar los cambios principales
    salesRepository.save(existingSale);

    // 16. Si hay saldo pendiente, genera una nueva cuota SOLO si no existe ya una pendiente
    if (existingSale.getRemainingAmount() > 0) {
        boolean existePendiente = existingSale.getFees().stream()
            .anyMatch(fee -> fee.getPaid() == null || !fee.getPaid());
        if (!existePendiente) {
            // Buscar la última cuota generada (por número de cuota)
            Fee ultimaCuota = existingSale.getFees().stream()
                .max((f1, f2) -> Integer.compare(f1.getNumberFee(), f2.getNumberFee()))
                .orElse(null);

            LocalDate fechaBase = ultimaCuota != null ? ultimaCuota.getExpirationDate() : existingSale.getDateCreation();
            int nextNumberFee = ultimaCuota != null ? ultimaCuota.getNumberFee() + 1 : 1;

            Fee siguienteCuota = new Fee();
            siguienteCuota.setSale(existingSale);
            siguienteCuota.setNumberFee(nextNumberFee);
            siguienteCuota.setExpirationDate(calculateNextDate(fechaBase, existingSale.getTypePayments()));
            // siguienteCuota.setAmount(existingSale.getAmountFee());
            siguienteCuota.setPaid(false);

            existingSale.getFees().add(siguienteCuota);
            feeRepository.save(siguienteCuota);
            salesRepository.save(existingSale);
        }
    }

}

    @Transactional
    @Override
    public void updateSaleGlobalDelay(Long saleId) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        
        if (!sale.isCompleted()) {
            int newDelay = calculateGlobalDelay(saleId);
            if (newDelay != sale.getDaysLate()) {
                sale.setDaysLate(newDelay);
                salesRepository.save(sale);
            }
        }
    }


    // --- Tu método para sumar cuotas pagadas (corregido para usar getAmount()) ---
    public Double calculateTotalPaidFeesForSaleUsingStream(Sale sale) {
        if (sale == null || sale.getFees() == null) {
            return 0.0; // Devuelve 0.0 si la venta o sus cuotas son nulas
        }

        // Aquí usamos .mapToDouble().sum() para mayor simplicidad y para obtener un double primitivo directamente.
        // También verifica que fee.getAmount() no sea null antes de sumarlo.
        return sale.getFees().stream()
            .filter(fee -> fee.getPaid() != null && fee.getPaid()) // Solo cuotas pagadas
            .mapToDouble(fee -> fee.getAmount() != null ? fee.getAmount() : 0.0) // Mapea a Double, maneja nulls de amount
            .sum();
    }

    @Transactional
    @Override
    public void deletePrestamo(Long id) throws Exception {
        Sale existingSale = salesRepository.findById(id)
            .orElseThrow(() -> new Exception("Sale not found"));

        for (Fee fee : existingSale.getFees()) {
            feeRepository.delete(fee);
        }

        salesRepository.delete(existingSale);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SaleResponseDto> getAll() throws Exception {
        // findAll
        return salesRepository.findByProductTypeNot("VENTA").stream()
            .map(sale -> convertirAVentaResponseDTO(sale))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getSaleById(Long id) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSaleById'");
    }

    @Override
    public void deleteSale(Long id) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteSale'");
    }

    @Override
    public List<SaleResponseDto> getAllSales() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllSales'");
    }

  
}
