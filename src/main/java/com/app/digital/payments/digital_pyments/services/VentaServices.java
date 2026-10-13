package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;


import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SalesCountsDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IVentaServices;
import com.app.digital.payments.digital_pyments.utils.FeeScheduleCalculator;
import com.app.digital.payments.digital_pyments.utils.Payments;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

import static com.app.digital.payments.digital_pyments.mappers.SaleMapper.convertirAVentaResponseDTO;;

@Service
public class VentaServices implements IVentaServices {

    private final ISaleRepository salesRepository;
    private final IClientRepository clientRepository;
    private final IFeeRepository feeRepository;
    private final IProductTypeRepository productTypeRepository;
    private final IProductRepository productRepository;
    private final IBusinessRepository businessRepository;

    private static final Logger log = LoggerFactory.getLogger(VentaServices.class);

    VentaServices(ISaleRepository salesRepository, IClientRepository clientRepository, IFeeRepository feeRepository,
            IProductTypeRepository productTypeRepository, IProductRepository productRepository,
            IBusinessRepository businessRepository) {
        this.salesRepository = salesRepository;
        this.clientRepository = clientRepository;
        this.feeRepository = feeRepository;
        this.productTypeRepository = productTypeRepository;
        this.productRepository = productRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    @Override
    public SaleResponseDto createVenta(SaleRequestDto saleDto) throws Exception {

        SaleKind kind = SaleKind.fromString(saleDto.getKind());
        Long negocioId = TenantContext.requireBusinessId();

        Client client = clientRepository.findById(saleDto.getClientId())
                .orElseThrow(() -> new Exception("Cliente no encontrado"));
        // el cliente tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(client.getBusiness() != null ? client.getBusiness().getId() : null);

        // Categoría de producto: para VENTA se deriva SIEMPRE del producto del
        // catálogo (cada producto está ligado a su categoría); para PRESTAMO es opcional.
        ProductType productType = null;
        if (kind == SaleKind.PRESTAMO && saleDto.getProductType() != null) {
            productType = productTypeRepository.findById(saleDto.getProductType())
                    .orElseThrow(() -> new Exception("Categoría de producto no encontrada"));
            // la categoria tiene que ser del mismo negocio
            TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);
        }

        // Producto del catálogo: obligatorio para VENTA, tiene que pertenecer al negocio autenticado
        Product product = null;
        if (kind == SaleKind.VENTA && saleDto.getProduct() == null) {
            throw new IllegalArgumentException("Debe seleccionar un producto para la venta");
        }
        if (saleDto.getProduct() != null) {
            product = productRepository.findById(saleDto.getProduct())
                    .orElseThrow(() -> new Exception("Producto no encontrado"));
            TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);
        }

        // En VENTA la categoría es la del producto seleccionado, no la del request
        if (kind == SaleKind.VENTA) {
            productType = product.getProductType();
            if (productType == null) {
                throw new IllegalArgumentException("El producto no tiene una categoría asignada");
            }
        }

        // Cantidad de unidades vendidas (V9): solo aplica a VENTA, por defecto 1.
        Integer quantity;
        if (kind == SaleKind.VENTA) {
            quantity = saleDto.getQuantity() != null ? saleDto.getQuantity() : 1;
            if (quantity <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }
        } else {
            quantity = 1;
        }

        BigDecimal amountFee = saleDto.getAmountFee();
        if (amountFee != null && amountFee.signum() == 0) {
            amountFee = saleDto.getFirstFeeAmount();
        }
        if (amountFee == null || amountFee.signum() <= 0) {
            throw new IllegalArgumentException("El monto de la cuota debe ser mayor a 0");
        }

        if (saleDto.getQuantityFees() == null || saleDto.getQuantityFees() <= 0) {
            throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a 0");
        }

        Payments paymentType = Payments.fromString(saleDto.getPayments());

        if (kind == SaleKind.PRESTAMO && paymentType == Payments.CONTADO) {
            throw new IllegalArgumentException("Un préstamo no puede ser a contado; debe pagarse en cuotas");
        }

        Integer quantityFees = saleDto.getQuantityFees();
        if (paymentType == Payments.CONTADO) {
            quantityFees = 1;
        }

        // Costo: para VENTA se toma fijo del precio del producto (no editable);
        // para PRESTAMO es el monto entregado que envía el cliente.
        final BigDecimal cost;
        if (kind == SaleKind.VENTA) {
            if (product.getPrice() == null || product.getPrice().signum() <= 0) {
                throw new IllegalArgumentException("El producto no tiene un precio válido");
            }
            // Costo del negocio = precio unitario x cantidad vendida (V9)
            cost = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        } else {
            if (saleDto.getCost() == null || saleDto.getCost().signum() <= 0) {
                throw new IllegalArgumentException("El monto entregado debe ser mayor a 0");
            }
            cost = saleDto.getCost();
        }

        BigDecimal totalPrice;
        BigDecimal rate = saleDto.getInterestRate();

        if (kind == SaleKind.PRESTAMO && rate != null) {
            // Con tasa de interés: cost es el capital entregado y
            // el total a devolver incluye el interés calculado.
            if (rate.signum() < 0) {
                throw new IllegalArgumentException("La tasa de interés no puede ser negativa");
            }
            BigDecimal factor = BigDecimal.ONE.add(
                    rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal total = cost.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            amountFee = total.divide(BigDecimal.valueOf(quantityFees), 2, RoundingMode.HALF_UP);
            totalPrice = BigDecimal.valueOf(quantityFees).multiply(amountFee);
        } else {
            totalPrice = BigDecimal.valueOf(quantityFees).multiply(amountFee);
        }

        if (kind == SaleKind.VENTA && cost.compareTo(totalPrice) >= 0) {
            throw new IllegalArgumentException("El costo debe ser menor al monto total de la venta");
        }
        if (kind == SaleKind.PRESTAMO && rate == null
                && cost.compareTo(totalPrice) != 0) {
            throw new IllegalArgumentException("El monto entregado del préstamo debe coincidir con el total a devolver");
        }

        String description = saleDto.getDescriptionProduct();
        if (kind == SaleKind.VENTA) {
            // Con más de una unidad la descripción refleja la cantidad (V9)
            description = quantity > 1
                    ? quantity + " x " + product.getName()
                    : product.getName();
        } else if (description == null || description.isBlank()) {
            description = "Préstamo personal";
        }

        Sale newSale = new Sale();
        newSale.setClient(client);
        newSale.setKind(kind);
        newSale.setDescriptionProduct(description);
        newSale.setProductType(productType);
        newSale.setProduct(product);
        newSale.setBusiness(businessRepository.getReferenceById(negocioId));

        newSale.setPriceTotal(totalPrice);
        newSale.setCost(cost);
        newSale.setInterestRate(kind == SaleKind.PRESTAMO ? rate : null);
        newSale.setQuantity(quantity);

        newSale.setDateCreation(saleDto.getDateSale() != null ? saleDto.getDateSale() : LocalDate.now());

        if (saleDto.getFirstFeeDate() != null && saleDto.getFirstFeeDate().isBefore(newSale.getDateCreation())) {
            throw new IllegalArgumentException("La fecha de la primera cuota no puede ser anterior a la fecha de la transacción");
        }

        LocalDate firstFeeDate = saleDto.getFirstFeeDate() != null ? saleDto.getFirstFeeDate() : LocalDate.now();

        newSale.setTypePayments(paymentType);
        newSale.setQuiantityFees(quantityFees);
        newSale.setAmountFee(amountFee);
        newSale.setRemainingAmount(totalPrice);
        newSale.setFinalPaymentDate(FeeScheduleCalculator.finalPaymentDate(
                firstFeeDate, paymentType, quantityFees));
        newSale.setStatus(SaleStatus.ACTIVE);

        List<Fee> fees = new ArrayList<>();
        Fee primeraCuota = new Fee();
        primeraCuota.setSale(newSale);
        primeraCuota.setNumberFee(1);
        primeraCuota.setExpirationDate(firstFeeDate);
        primeraCuota.setPaid(false);
        primeraCuota.setAmount(amountFee);
        primeraCuota.setBusiness(newSale.getBusiness());

        if (Boolean.TRUE.equals(saleDto.getPayFirstFee())) {
            primeraCuota.setPaid(true);
            primeraCuota.setDatePayment(LocalDate.now());
            BigDecimal montoPagado = saleDto.getFirstFeeAmount() != null
                    ? saleDto.getFirstFeeAmount()
                    : amountFee;
            if (montoPagado.signum() <= 0) {
                throw new IllegalArgumentException("El monto de la primera cuota debe ser mayor a 0");
            }
            if (montoPagado.compareTo(newSale.getRemainingAmount()) > 0) {
                throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
            }
            primeraCuota.setAmount(montoPagado);
            newSale.deductFromRemainingAmount(montoPagado);

            if (newSale.getRemainingAmount().signum() > 0) {
                Fee siguienteCuota = new Fee();
                siguienteCuota.setSale(newSale);
                siguienteCuota.setNumberFee(2);
                siguienteCuota.setExpirationDate(FeeScheduleCalculator.nextExpirationDate(firstFeeDate, paymentType));
                siguienteCuota.setPaid(false);
                siguienteCuota.setAmount(amountFee);
                siguienteCuota.setBusiness(newSale.getBusiness());
                fees.add(siguienteCuota);
            }
        }
        fees.add(primeraCuota);
        newSale.setFees(fees);

        // Descontar stock (V9): solo si hay suficiente inventario.
        if (kind == SaleKind.VENTA && product.getStock() != null && quantity <= product.getStock()) {
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
        }

        Sale savedSale = salesRepository.save(newSale);

        return convertirAVentaResponseDTO(savedSale);
    }

    // Marca una venta o prestamo como completada, actualizando la fecha final real.
    // Solo se marca como completada cuando el saldo restante llega a cero.
    @Transactional
    @Override
    public void markPrestamoAsCompleted(Long saleId, BigDecimal amountPaid) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);

        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new IllegalStateException("La venta ya está completada");
        }

        Fee nextFee = sale.getFees().stream()
                .filter(fee -> fee.getPaid() == null || !fee.getPaid())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay cuotas pendientes para completar la venta"));

        BigDecimal paid = amountPaid != null ? amountPaid : nextFee.getAmount();
        if (paid == null || paid.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        if (paid.compareTo(sale.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        }

        sale.deductFromRemainingAmount(paid);
        nextFee.setPaid(true);
        nextFee.setDatePayment(LocalDate.now());
        nextFee.setAmount(paid);
        feeRepository.save(nextFee);

        if (sale.getRemainingAmount().signum() <= 0) {
            sale.setStatus(SaleStatus.COMPLETED);
            sale.setRealFinalDate(LocalDate.now());
        } else {
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setRealFinalDate(null);
        }
        salesRepository.save(sale);
    }

    // Modificamos una venta o préstamo existente (tipo, categoría y descripción)
    @Transactional
    @Override
    public void updateSale(SaleUpdateRequestDto sale, Long id) throws Exception {
        Sale existingSale = salesRepository.findById(id)
                .orElseThrow(() -> new Exception("Venta no encontrada con ID: " + id));
        TenantGuard.verificar(existingSale.getBusiness() != null ? existingSale.getBusiness().getId() : null);

        SaleKind kind = SaleKind.fromString(sale.getKind());

        ProductType productType = null;
        if (sale.getProductType() != null) {
            productType = productTypeRepository.findById(sale.getProductType())
                    .orElseThrow(() -> new Exception("Categoría de producto no encontrada"));
            // la categoria tiene que ser del mismo negocio
            TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);
        }

        Product product = null;
        if (sale.getProduct() != null) {
            product = productRepository.findById(sale.getProduct())
                    .orElseThrow(() -> new Exception("Producto no encontrado"));
            // el producto tiene que ser del mismo negocio
            TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);
        }

        if (kind == SaleKind.VENTA && product != null) {
            // La categoría de la venta es siempre la del producto (no la del request)
            productType = product.getProductType();
            if (productType == null) {
                throw new IllegalArgumentException("El producto no tiene una categoría asignada");
            }
        }
        if (kind == SaleKind.VENTA && productType == null) {
            throw new IllegalArgumentException("La categoría de producto es obligatoria para las ventas");
        }
        if (kind == SaleKind.VENTA
                && (sale.getDescriptionProduct() == null || sale.getDescriptionProduct().isBlank())) {
            throw new IllegalArgumentException("La descripción del producto es obligatoria para las ventas");
        }

        String description = sale.getDescriptionProduct();
        if (description == null || description.isBlank()) {
            description = "Préstamo personal";
        }

        existingSale.setKind(kind);
        existingSale.setProductType(productType);
        existingSale.setProduct(product);
        existingSale.setDescriptionProduct(description);

        salesRepository.save(existingSale);
    }

    public int calculateGlobalDelay(Long saleId) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);

        if (sale.getFinalPaymentDate() != null && LocalDate.now().isAfter(sale.getFinalPaymentDate())) {
            return (int) ChronoUnit.DAYS.between(sale.getFinalPaymentDate(), LocalDate.now());
        }
        return 0;
    }

    // método para sumar cuotas pagadas
    public BigDecimal calculateTotalPaidFeesForSaleUsingStream(Sale sale) {
        if (sale == null || sale.getFees() == null) {
            return BigDecimal.ZERO;
        }
        return sale.getFees().stream()
                .filter(fee -> fee.getPaid() != null && fee.getPaid())
                .map(Fee::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SaleResponseDto> getAllVentasPaginacion(
        Pageable pageable,
        Integer year,
        Integer month,
        Integer day,
        SaleStatus status,
        Boolean aCobrar,
        String clientName,
        String descriptionProduct,
        Long productType,
        SaleKind kind,
        Payments typePayments,
        BigDecimal minAmount,
        BigDecimal maxAmount
        ) {
        Specification<Sale> spec = buildSalesSpecification(
            year, month, day, status, aCobrar, clientName, descriptionProduct, productType, kind,
            typePayments, minAmount, maxAmount
        );
        return salesRepository.findAll(spec, pageable).map(sale -> convertirAVentaResponseDTO(sale));
    }

    @Override
    @Transactional(readOnly = true)
    public SalesCountsDto getSalesCounts(SaleKind kind) {
        SalesCountsDto counts = new SalesCountsDto();
        counts.setTotal(countSales(kind, null));
        counts.setActive(countSales(kind, SaleStatus.ACTIVE));
        counts.setCompleted(countSales(kind, SaleStatus.COMPLETED));
        counts.setCanceled(countSales(kind, SaleStatus.CANCELED));
        counts.setACobrar(countACobrar(kind));
        return counts;
    }

    private long countSales(SaleKind kind, SaleStatus status) {
        Specification<Sale> spec = buildSalesSpecification(
            null, null, null, status, null, null, null, null, kind,
            null, null, null
        );
        return salesRepository.count(spec);
    }

    private long countACobrar(SaleKind kind) {
        Specification<Sale> spec = buildSalesSpecification(
            null, null, null, null, true, null, null, null, kind,
            null, null, null
        );
        return salesRepository.count(spec);
    }

    private Specification<Sale> buildSalesSpecification(
        Integer year,
        Integer month,
        Integer day,
        SaleStatus status,
        Boolean aCobrar,
        String clientName,
        String descriptionProduct,
        Long productType,
        SaleKind kind,
        Payments typePayments,
        BigDecimal minAmount,
        BigDecimal maxAmount
        ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro especial para cuotas vencidas sin pagar (A_COBRAR)
            if (Boolean.TRUE.equals(aCobrar)) {
                Join<Sale, Fee> feeJoin = root.join("fees");
                query.distinct(true);
                predicates.add(criteriaBuilder.isFalse(feeJoin.get("paid")));
                predicates.add(criteriaBuilder.lessThanOrEqualTo(feeJoin.get("expirationDate"), LocalDate.now()));
            } else if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (kind != null) {
                predicates.add(criteriaBuilder.equal(root.get("kind"), kind));
            }

            if (typePayments != null) {
                predicates.add(criteriaBuilder.equal(root.get("typePayments"), typePayments));
            }

            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("priceTotal"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("priceTotal"), maxAmount));
            }

            if (year != null) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.function("date_part", Integer.class, criteriaBuilder.literal("year"), root.get("dateCreation")),
                    year
                ));
            }

            if (month != null) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.function("date_part", Integer.class, criteriaBuilder.literal("month"), root.get("dateCreation")),
                    month
                ));
            }

            if (day != null) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.function("date_part", Integer.class, criteriaBuilder.literal("day"), root.get("dateCreation")),
                    day
                ));
            }

            if (clientName != null && !clientName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("client").get("name")),
                    "%" + clientName.trim().toLowerCase() + "%"
                ));
            }

            if (descriptionProduct != null && !descriptionProduct.isBlank()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("descriptionProduct")),
                    "%" + descriptionProduct.trim().toLowerCase() + "%"
                ));
            }

            if (productType != null) {
                predicates.add(criteriaBuilder.equal(root.get("productType").get("id"), productType));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getSaleById(Long id) throws Exception {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new Exception("Sale not found"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);
        return convertirAVentaResponseDTO(sale);
    }

    @Transactional
    @Override
    public void deleteVenta(Long id) throws Exception {
        Sale existingSale = salesRepository.findById(id)
                .orElseThrow(() -> new Exception("Sale not found"));
        TenantGuard.verificar(existingSale.getBusiness() != null ? existingSale.getBusiness().getId() : null);

        // Restricción (anular + eliminar restringido): una venta con pagos registrados
        // no se puede borrar físicamente; debe anularse para conservar el historial.
        boolean tienePagos = existingSale.getFees().stream()
                .anyMatch(fee -> Boolean.TRUE.equals(fee.getPaid()));
        if (tienePagos) {
            throw new IllegalArgumentException("La venta tiene pagos registrados. Anúlala en lugar de eliminarla.");
        }

        for (Fee fee : existingSale.getFees()) {
            feeRepository.delete(fee);
        }

        // Reponer stock (V9): si la venta tenía un producto con control de inventario,
        // las unidades vendidas vuelven al catálogo al eliminar la venta.
        Product productoVendido = existingSale.getProduct();
        Integer cantidadVendida = existingSale.getQuantity() != null ? existingSale.getQuantity() : 1;
        if (productoVendido != null && productoVendido.getStock() != null && cantidadVendida > 0) {
            productoVendido.setStock(productoVendido.getStock() + cantidadVendida);
            productRepository.save(productoVendido);
        }

        salesRepository.delete(existingSale);
    }

    // Anula una venta o préstamo: conserva el registro con estado CANCELADO (queda
    // excluida de deudas y estadísticas), registra el monto ya cobrado como reembolso
    // (si refund=true se devuelve al cliente; si false queda retenido) y repone el
    // stock de los productos con control de inventario (V9).
    @Transactional
    @Override
    public SaleResponseDto cancelVenta(Long id, boolean refund) throws Exception {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new Exception("Sale not found"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);

        if (sale.getStatus() == SaleStatus.CANCELED) {
            return convertirAVentaResponseDTO(sale);
        }

        // Lo ya cobrado (cuotas pagadas). Las cuotas se conservan como historial del
        // registro anulado. Si refund=true se registra como devolución al cliente;
        // si refund=false el monto queda retenido por el negocio.
        BigDecimal collected = sale.getFees().stream()
                .filter(fee -> Boolean.TRUE.equals(fee.getPaid()))
                .map(Fee::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sale.setRefundAmount(refund ? collected : BigDecimal.ZERO);
        sale.setRemainingAmount(BigDecimal.ZERO);
        sale.setRealFinalDate(LocalDate.now());
        sale.setStatus(SaleStatus.CANCELED);

        // Reponer stock: mismas condiciones que el borrado físico.
        Product productoVendido = sale.getProduct();
        Integer cantidadVendida = sale.getQuantity() != null ? sale.getQuantity() : 1;
        if (productoVendido != null && productoVendido.getStock() != null && cantidadVendida > 0) {
            productoVendido.setStock(productoVendido.getStock() + cantidadVendida);
            productRepository.save(productoVendido);
        }

        return convertirAVentaResponseDTO(salesRepository.save(sale));
    }

}
