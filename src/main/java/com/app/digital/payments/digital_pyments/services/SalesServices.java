package com.app.digital.payments.digital_pyments.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.PaidFeeDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleOldRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.Payments;

import static com.app.digital.payments.digital_pyments.mappers.SaleMapper.convertirAVentaResponseDTO;;

@Service
public class SalesServices implements ISalesServices {

    @Autowired
    private ISaleRepository salesRepository;
    @Autowired
    private IClientRepository clientRepository;
    @Autowired
    private IFeeRepository feeRepository;

    // venta antigua, que no olvidamos de crear, puede tener cuotas pagadas o no
    // si tiene cuotas pagadas, se calcula el monto restante
    @Override
    @Transactional
    public SaleResponseDto createSaleOld(SaleOldRequestDto saleDto, LocalDate date) throws Exception { 
        // Validaciones de negocio
        if (saleDto.getQuantityFees() <= 0) {
            throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a 0");
        }
        if (saleDto.getPriceTotal() <= 0) {
            throw new IllegalArgumentException("El monto total debe ser positivo");
        }

        Client client = clientRepository.findById(saleDto.getClientId())
                .orElseThrow(() -> new Exception("Cliente no encontrado"));

        Sale newSale = new Sale();
        newSale.setClient(client);

        newSale.setDescriptiononProduct(saleDto.getDescriptionProduct());  
        newSale.setPriceTotal(saleDto.getPriceTotal());
        newSale.setDateSale(date);

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
            newSale.setRemainingAmount(saleDto.getPriceTotal() - totalPaid);
        } else {
            newSale.setRemainingAmount(saleDto.getPriceTotal()); // Inicializa con el total
        }

        // Configuración de la venta
        Payments paymentType = Payments.valueOf(saleDto.getPayments().toUpperCase());
        newSale.setTypePayments(paymentType);
        newSale.setQuiantityFees(saleDto.getQuantityFees());
        newSale.setAmountFe(saleDto.getPriceTotal() / saleDto.getQuantityFees());
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
    public SaleResponseDto createSale(SaleRequestDto saleDto) throws Exception {
        
        Client client = clientRepository.findById(saleDto.getClienteId())
                .orElseThrow(() -> new Exception("Cliente no encontrado"));

        Sale newSale = new Sale();
        newSale.setClient(client);
        newSale.setDescriptiononProduct(saleDto.getDescripcionProducto());
        newSale.setPriceTotal(saleDto.getPrecioTotal());

        // newSale.setDateSale(date != null ? date : LocalDate.now());
        newSale.setDateSale(LocalDate.now());


        // si no tiene fecha de primera cuota, se usa la fecha actual
        // si tiene fecha de primera cuota, se usa esa
        LocalDate firstFeeDate = saleDto.getFirstFeeDate()!= null ? saleDto.getFirstFeeDate() : LocalDate.now();

        // Validación de negocio: cantidad de cuotas debe ser mayor a 0
        if (saleDto.getCantidadCuotas() <= 0) {
            throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a 0");
        }
        // Validación de negocio: monto total debe ser positivo
        if (saleDto.getPrecioTotal() <= 0) {
            throw new IllegalArgumentException("El monto total debe ser positivo");
        }
        // Configuración de la venta
        Payments paymentType = Payments.valueOf(saleDto.getPayments().toUpperCase());
        newSale.setTypePayments(paymentType);
        newSale.setQuiantityFees(saleDto.getCantidadCuotas());
        newSale.setAmountFe(saleDto.getPrecioTotal() / saleDto.getCantidadCuotas());

        newSale.setRemainingAmount(saleDto.getPrecioTotal()); // Inicializa con el total

        
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
    if (saleDto.getPayFirstFee()) {
        primeraCuota.setPaid(true);
        primeraCuota.setDatePayment(LocalDate.now());
        Double montoPagado = saleDto.getFirstFeeAmount() != null
            ? saleDto.getFirstFeeAmount()
            : saleDto.getPrecioTotal() / saleDto.getCantidadCuotas();
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


    // actualiza los días de atraso de una venta
    // si la venta no está completada, calcula los días de atraso y actualiza la venta
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

    // Marca una venta como completada, actualizando la fecha final y los días de atraso
    // Si la venta ya está completada, no hace nada
    @Transactional
    @Override
    public void markSaleAsCompleted(Long saleId) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        
        if (sale.getRemainingAmount() > 0) {
            throw new IllegalStateException("No se puede completar, hay saldo pendiente");
        }   

        if (!sale.isCompleted()) {
            // 1. Calcular atraso final antes de marcar como completada
            int finalDelay = calculateGlobalDelay(saleId);
            
            // 2. Actualizar campos
            sale.setCompleted(true);
            sale.setRealFinalDate(LocalDate.now());
            sale.setDaysLate(finalDelay); // Conservamos el atraso histórico
            sale.setFinalPaymentDate(LocalDate.now()); // Actualizar fecha final real
            
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

    @Transactional(readOnly = true)
    @Override
    public SaleResponseDto getSaleById(Long id) throws Exception {
        return salesRepository.findById(id)
            .map(sale -> convertirAVentaResponseDTO(sale))
            .orElseThrow(() -> new Exception("Sale not found"));
    }


    // Modificamos una venta existente
    // Validaciones: venta existente, cliente válido, monto total positivo, cuotas válidas
    @Transactional
    @Override
    public void updateSale(SaleRequestDto sale, Long id) throws Exception {
        Sale existingSale = salesRepository.findById(id)
            .orElseThrow(() -> new Exception("Sale not found"));

        Client client = clientRepository.findById(sale.getClienteId())
            .orElseThrow(() -> new Exception("Client not found"));

        existingSale.setClient(client);
        existingSale.setDescriptiononProduct(sale.getDescripcionProducto());
        existingSale.setPriceTotal(sale.getPrecioTotal());
        existingSale.setDateSale(sale.getDateSale());
        Payments paymentType = Payments.valueOf(sale.getPayments().toUpperCase());
        existingSale.setTypePayments(paymentType);
        existingSale.setQuiantityFees(sale.getCantidadCuotas());

        double montoCuota = sale.getPrecioTotal() / sale.getCantidadCuotas();

        existingSale.setAmountFe(montoCuota);

        // reacaulcuarl deuda restante
        // calcular deuda restante con las cuotas pagadas


    
        existingSale.getFees().clear();
        salesRepository.save(existingSale);
    }

    @Transactional
    @Override
    public void deleteSale(Long id) throws Exception {
        Sale existingSale = salesRepository.findById(id)
            .orElseThrow(() -> new Exception("Sale not found"));

        for (Fee fee : existingSale.getFees()) {
            feeRepository.delete(fee);
        }

        salesRepository.delete(existingSale);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SaleResponseDto> getAllSales() throws Exception {
        return salesRepository.findAll().stream()
            .map(sale -> convertirAVentaResponseDTO(sale))
            .collect(Collectors.toList());
    }

    // Agrega una cuota adicional a una venta existente
    // Validaciones: venta no completada, monto válido, cuota adicional no duplicada
    @Transactional
    public SaleResponseDto addAdditionalFee(Long saleId, Double amount) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        
        // Validaciones
        if (sale.isCompleted()) {
            throw new IllegalStateException("Venta ya completada");
        }
        if (amount <= 0 || amount > sale.getRemainingAmount()) {
            throw new IllegalArgumentException("Monto inválido");
        }

        // Crear cuota adicional
        Fee additionalFee = new Fee();
        additionalFee.setSale(sale);
        additionalFee.setNumberFee(sale.getQuiantityFees() + sale.getAdditionalFees() + 1);
        additionalFee.setAmount(amount);
        additionalFee.setExpirationDate(calculateNextAdditionalFeeDate(sale));
        additionalFee.setPaid(false);
        additionalFee.setAdditional(true); // Nuevo campo para identificar extras
        // additionalFee.setExpirationOrigialDate(additionalFee.getExpirationDate());

        // Actualizar venta
        sale.deductFromRemainingAmount(amount);
        sale.setAdditionalFees(sale.getAdditionalFees() + 1);

        // Guardar
        feeRepository.save(additionalFee);
        Sale updatedSale = salesRepository.save(sale);
        
        return convertirAVentaResponseDTO(updatedSale);
    }


    // Calcula la fecha de la próxima cuota adicional
    // Basado en la última cuota existente (original o adicional)
    private LocalDate calculateNextAdditionalFeeDate(Sale sale) {
    // 1. Obtener la última cuota existente (original o adicional)
        Optional<Fee> lastFee = sale.getFees().stream()
            .max(Comparator.comparing(Fee::getNumberFee));
        
        // 2. Si no hay cuotas previas (caso inusual), usar fecha actual + periodicidad
        if (lastFee.isEmpty()) {
            return calculateDateByFrequency(LocalDate.now(), sale.getTypePayments());
        }
        
        // 3. Calcular basado en la última cuota existente
        Fee last = lastFee.get();
        return calculateDateByFrequency(last.getExpirationDate(), sale.getTypePayments());
    }

    // Calcula la fecha según la frecuencia de pago
    // Utiliza un switch para determinar la periodicidad
    private LocalDate calculateDateByFrequency(LocalDate baseDate, Payments frequency) {
        return switch (frequency) {
            case SEMANAL -> baseDate.plusWeeks(1);
            case QUINCENAL -> baseDate.plusWeeks(2);
            case MENSUAL -> baseDate.plusMonths(1);
            default -> baseDate.plusWeeks(1); // Default semanal
        };
    }
        

  

}
