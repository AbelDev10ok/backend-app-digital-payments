package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;


import com.app.digital.payments.digital_pyments.mappers.ProductMapper;
import com.app.digital.payments.digital_pyments.mappers.SaleDocumentMapper;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.Fee;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.SaleDocument;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.SaleRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleResponseDto;
import com.app.digital.payments.digital_pyments.models.dtos.SaleUpdateRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.SalesCountsDto;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.IFeeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleDocumentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.IVentaServices;
import com.app.digital.payments.digital_pyments.utils.DocumentType;
import com.app.digital.payments.digital_pyments.utils.FeePaymentAllocator;
import com.app.digital.payments.digital_pyments.utils.FeeScheduleCalculator;
import com.app.digital.payments.digital_pyments.utils.PaymentMethod;
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
    private final IProductVariantRepository variantRepository;
    private final IBusinessRepository businessRepository;
    private final ISaleDocumentRepository saleDocumentRepository;

    private static final Logger log = LoggerFactory.getLogger(VentaServices.class);

    VentaServices(ISaleRepository salesRepository, IClientRepository clientRepository, IFeeRepository feeRepository,
            IProductTypeRepository productTypeRepository, IProductVariantRepository variantRepository,
            IBusinessRepository businessRepository, ISaleDocumentRepository saleDocumentRepository) {
        this.salesRepository = salesRepository;
        this.clientRepository = clientRepository;
        this.feeRepository = feeRepository;
        this.productTypeRepository = productTypeRepository;
        this.variantRepository = variantRepository;
        this.businessRepository = businessRepository;
        this.saleDocumentRepository = saleDocumentRepository;
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

        // Vendedor de la operación: se sella al crearla y no lo mueve una
        // reasignación posterior del cliente. Null = venta directa.
        Client seller = resolverVendedorDeOperacion(saleDto.getSellerId(), client);

        // Categoría de producto: para VENTA se deriva SIEMPRE del producto del
        // catálogo (cada producto está ligado a su categoría); para PRESTAMO es opcional.
        ProductType productType = null;
        if (kind == SaleKind.PRESTAMO && saleDto.getProductType() != null) {
            productType = productTypeRepository.findById(saleDto.getProductType())
                    .orElseThrow(() -> new Exception("Categoría de producto no encontrada"));
            // la categoria tiene que ser del mismo negocio
            TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);
        }

        // Variante/SKU vendido: obligatorio para VENTA, del negocio y disponible.
        ProductVariant variant = null;
        Product product = null;
        if (kind == SaleKind.VENTA) {
            if (saleDto.getVariant() == null) {
                throw new IllegalArgumentException("Debe seleccionar un producto para la venta");
            }
            variant = variantRepository.findById(saleDto.getVariant())
                    .orElseThrow(() -> new Exception("Producto no encontrado"));
            TenantGuard.verificar(variant.getBusiness() != null ? variant.getBusiness().getId() : null);
            product = variant.getProduct();
            if (product == null) {
                throw new IllegalArgumentException("La variante no tiene un producto asociado");
            }
            TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);
            if (!Boolean.TRUE.equals(variant.getActive()) || !Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException("El producto está desactivado y no se puede vender");
            }
            if (variant.getPrice() == null || variant.getPrice().signum() <= 0) {
                throw new IllegalArgumentException("El producto no tiene un precio de venta válido");
            }
            // En VENTA la categoría es la del producto seleccionado, no la del request
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
            // Stock obligatorio: no se puede vender lo que no hay.
            int disponible = variant.getStock() != null ? variant.getStock() : 0;
            if (disponible < quantity) {
                throw new IllegalArgumentException(disponible == 0
                        ? "No hay stock disponible de " + variant.getSku()
                        : "Stock insuficiente de " + variant.getSku() + ": quedan " + disponible + " unidades");
            }
        } else {
            quantity = 1;
        }

        Payments paymentType = Payments.fromString(saleDto.getPayments());

        if (kind == SaleKind.PRESTAMO && paymentType == Payments.CONTADO) {
            throw new IllegalArgumentException("Un préstamo no puede ser a contado; debe pagarse en cuotas");
        }

        BigDecimal amountFee = saleDto.getAmountFee();
        if (amountFee != null && amountFee.signum() == 0) {
            amountFee = saleDto.getFirstFeeAmount();
        }

        Integer quantityFees = saleDto.getQuantityFees();

        if (kind == SaleKind.VENTA && paymentType == Payments.CONTADO) {
            // Al contado manda el precio de venta del catálogo:
            // total = precio_venta_unitario × cantidad.
            if (variant.getPrice() == null || variant.getPrice().signum() <= 0) {
                throw new IllegalArgumentException("El producto no tiene un precio de venta válido");
            }
            amountFee = variant.getPrice();
            quantityFees = 1;
        } else {
            if (amountFee == null || amountFee.signum() <= 0) {
                throw new IllegalArgumentException("El monto de la cuota debe ser mayor a 0");
            }
            if (quantityFees == null || quantityFees <= 0) {
                throw new IllegalArgumentException("La cantidad de cuotas debe ser mayor a 0");
            }
            // Una sola cuota es una operación al contado: en cuotas el piso son 2.
            if (quantityFees < 2) {
                throw new IllegalArgumentException("La cantidad de cuotas debe ser al menos 2");
            }
        }

        // Costo: para VENTA se toma fijo del costo de la variante (no editable);
        // para PRESTAMO es el monto entregado que envía el cliente.
        final BigDecimal unitPrice;
        final BigDecimal cost;
        if (kind == SaleKind.VENTA) {
            if (variant.getCost() == null || variant.getCost().signum() < 0) {
                throw new IllegalArgumentException("El producto no tiene un costo válido");
            }
            unitPrice = variant.getPrice();
            // Costo del negocio = costo unitario x cantidad vendida (V9)
            cost = variant.getCost().multiply(BigDecimal.valueOf(quantity));
        } else {
            unitPrice = null;
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
        } else if (kind == SaleKind.VENTA && paymentType == Payments.CONTADO) {
            // Contado: total = precio de venta unitario × cantidad.
            totalPrice = amountFee.multiply(BigDecimal.valueOf(quantity));
        } else {
            // Cuotas: total = monto de cuota × cantidad de cuotas.
            totalPrice = BigDecimal.valueOf(quantityFees).multiply(amountFee);
        }

        // Piso de la venta: el total a cobrar nunca puede quedar por debajo del
        // precio de venta del producto (unitario × cantidad). Al contado el total
        // ya es esa suma; en cuotas se exige sobre cuota × cantidad de cuotas.
        if (kind == SaleKind.VENTA) {
            BigDecimal precioDeLista = unitPrice.multiply(BigDecimal.valueOf(quantity));
            if (totalPrice.compareTo(precioDeLista) < 0) {
                throw new IllegalArgumentException(
                        "El total a cobrar no puede ser menor al precio de venta del producto ("
                                + precioDeLista.stripTrailingZeros().toPlainString() + ")");
            }
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
            // Con más de una unidad la descripción refleja la cantidad (V9).
            // La variante agrega su etiqueta de atributos ("Rojo / M") para que
            // el detalle se entienda sin abrir el catálogo.
            String etiqueta = ProductMapper.etiquetaDe(variant);
            String base = etiqueta.isBlank() ? product.getName() : product.getName() + " (" + etiqueta + ")";
            description = quantity > 1 ? quantity + " x " + base : base;
        } else if (description == null || description.isBlank()) {
            description = "Préstamo personal";
        }

        Sale newSale = new Sale();
        newSale.setClient(client);
        newSale.setSeller(seller);
        newSale.setKind(kind);
        newSale.setDescriptionProduct(description);
        newSale.setProductType(productType);
        newSale.setVariant(variant);
        Business business = businessRepository.getReferenceById(negocioId);
        newSale.setBusiness(business);
        // Moneda de la operación (ISO 4217): se congela al crear y no se edita
        // después. La VENTA la hereda de su producto (el precio del catálogo está
        // expresado ahí); el PRESTAMO, al no tener producto, la elige el usuario.
        // La moneda del negocio sólo aporta como respaldo de un registro legado
        // sin moneda propia. Nunca se reinterpreta un importe con la config actual.
        newSale.setCurrency(resolverMoneda(kind, product, saleDto.getCurrency(), business));

        newSale.setPriceTotal(totalPrice);
        newSale.setCost(cost);
        newSale.setUnitPrice(unitPrice);
        newSale.setInterestRate(kind == SaleKind.PRESTAMO ? rate : null);
        newSale.setQuantity(quantity);

        newSale.setDateCreation(saleDto.getDateSale() != null ? saleDto.getDateSale() : LocalDate.now());

        if (saleDto.getFirstFeeDate() != null && saleDto.getFirstFeeDate().isBefore(newSale.getDateCreation())) {
            throw new IllegalArgumentException("La fecha de la primera cuota no puede ser anterior a la fecha de la transacción");
        }

        // La primera cuota vence con la operación: si no viene informada, se agenda
        // en la fecha de la venta. Antes caía en "hoy", así que una venta cargada
        // con fecha pasada corría el plan entero y mostraba otra "fecha final".
        LocalDate firstFeeDate = saleDto.getFirstFeeDate() != null ? saleDto.getFirstFeeDate() : newSale.getDateCreation();

        newSale.setTypePayments(paymentType);
        newSale.setQuiantityFees(quantityFees);
        newSale.setAmountFee(amountFee);
        newSale.setRemainingAmount(totalPrice);
        newSale.setFinalPaymentDate(FeeScheduleCalculator.finalPaymentDate(
                firstFeeDate, paymentType, quantityFees));
        newSale.setStatus(SaleStatus.ACTIVE);

        // El plan completo se materializa al crear la operación: cantidad de cuotas
        // pactada, vencimientos encadenados desde la primera fecha y monto de cuota.
        // Al cobrar no se crea ni se reprograma ninguna cuota.
        List<Fee> fees = new ArrayList<>();
        LocalDate vencimiento = firstFeeDate;
        for (int numero = 1; numero <= quantityFees; numero++) {
            Fee cuota = new Fee();
            cuota.setSale(newSale);
            cuota.setNumberFee(numero);
            cuota.setExpirationDate(vencimiento);
            cuota.setPaid(false);
            // Al contado la única cuota cubre el total (precio unitario × cantidad);
            // en cuotas, todas valen el monto de cuota pactado.
            cuota.setAmount(kind == SaleKind.VENTA && paymentType == Payments.CONTADO
                    ? totalPrice
                    : amountFee);
            cuota.setBusiness(newSale.getBusiness());
            fees.add(cuota);
            vencimiento = FeeScheduleCalculator.nextExpirationDate(vencimiento, paymentType);
        }
        newSale.setFees(fees);

        if (Boolean.TRUE.equals(saleDto.getPayFirstFee())) {
            BigDecimal montoPagado = saleDto.getFirstFeeAmount() != null
                    ? saleDto.getFirstFeeAmount()
                    : amountFee;
            if (montoPagado.signum() <= 0) {
                throw new IllegalArgumentException("El monto de la primera cuota debe ser mayor a 0");
            }
            if (montoPagado.compareTo(newSale.getRemainingAmount()) > 0) {
                throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
            }
            // El pago inicial entra por la misma cascada FIFO que cualquier cobro
            FeePaymentAllocator.aplicar(newSale, montoPagado, LocalDateTime.now(), PaymentMethod.EFECTIVO);
        }

        // El saldo sale de los cobros registrados; con el pago inicial cubriendo
        // todo el plan, la operación nace completada y con fecha final real.
        newSale.recalcularSaldo();
        if (newSale.getRemainingAmount().signum() <= 0) {
            newSale.setStatus(SaleStatus.COMPLETED);
            newSale.setRealFinalDate(LocalDate.now());
        } else {
            newSale.setStatus(SaleStatus.ACTIVE);
        }

        // Descontar stock (V9): el stock es obligatorio y ya se validó que alcanza.
        if (kind == SaleKind.VENTA && variant.getStock() != null) {
            variant.setStock(variant.getStock() - quantity);
            variantRepository.save(variant);
        }

        Sale savedSale = salesRepository.save(newSale);

        return convertirAVentaResponseDTO(savedSale);
    }

    /**
     * Vendedor de una operación (venta o préstamo).
     *
     * La autoría comercial se sella en la operación al crearla: si el pedido no
     * trae vendedor queda sin atribuir (venta directa), y una operación nunca se
     * atribuye a la misma persona que la compra (sus compras personales no
     * cuentan como ventas propias).
     */
    private Client resolverVendedorDeOperacion(Long sellerId, Client cliente) {
        if (sellerId == null) {
            return null;
        }
        Client vendedor = clientRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado con ID: " + sellerId));
        TenantGuard.verificar(vendedor.getBusiness() != null ? vendedor.getBusiness().getId() : null);
        if (!vendedor.isSeller()) {
            throw new IllegalArgumentException("El cliente con ID: " + sellerId + " no es un vendedor válido.");
        }
        if (cliente != null && vendedor.getId() != null && vendedor.getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Una operación no puede atribuirse a la misma persona que la compra.");
        }
        return vendedor;
    }

    /**
     * ¿El registro trae una moneda propia? Los creados antes de la columna
     * {@code currency} no la tienen y no se les inventa una.
     */
    private static boolean tieneMoneda(String currency) {
        return currency != null && !currency.isBlank();
    }

    /**
     * Moneda de la operación al crearla.
     *
     * Regla de dominio: un importe es un número con su moneda, y cambiar la
     * config del negocio no convierte dinero. Por eso la venta hereda la moneda
     * de su producto y un pedido que la contradiga se rechaza en vez de
     * reetiquetar el importe. Un préstamo, sin producto, usa la moneda elegida en
     * el alta; la del negocio queda como respaldo de los registros legados.
     */
    private static String resolverMoneda(SaleKind kind, Product product, String monedaPedida, Business business) {
        String monedaNegocio = CurrencyUtils.snapshot(business);

        if (kind == SaleKind.VENTA) {
            String monedaProducto = CurrencyUtils.orDefault(product.getCurrency(), monedaNegocio);
            if (monedaPedida != null && !monedaPedida.isBlank()
                    && !monedaProducto.equals(CurrencyUtils.orDefault(monedaPedida, null))) {
                throw new IllegalArgumentException(
                        "La moneda de la venta debe ser la del producto (" + monedaProducto + ")");
            }
            return monedaProducto;
        }

        if (monedaPedida != null && !monedaPedida.isBlank() && !CurrencyUtils.esSoportada(monedaPedida)) {
            throw new IllegalArgumentException(
                    "La moneda debe ser " + String.join(" o ", CurrencyUtils.MONEDAS_SOPORTADAS));
        }
        return CurrencyUtils.orDefault(monedaPedida, monedaNegocio);
    }

    // Marca una venta o prestamo como completada, actualizando la fecha final real.
    // Solo se marca como completada cuando el saldo restante llega a cero.
    @Transactional
    @Override
    public void markPrestamoAsCompleted(Long saleId, BigDecimal amountPaid) {
        Sale sale = salesRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);

        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new IllegalStateException("No se puede completar una venta anulada");
        }

        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new IllegalStateException("La venta ya está completada");
        }

        Fee cuotaPendiente = sale.cuotaPendiente();
        if (cuotaPendiente == null) {
            throw new RuntimeException("No hay cuotas pendientes para completar la venta");
        }

        BigDecimal paid = amountPaid != null ? amountPaid : cuotaPendiente.getPendingAmount();
        if (paid == null || paid.signum() <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        // El tope es la deuda real (total menos cobrado), no un numero guardado viejo
        sale.recalcularSaldo();
        if (paid.compareTo(sale.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo restante de la venta");
        }

        // El cobro entra por la cuota impaga más antigua y cascada al resto, sin
        // reescribir el monto programado de ninguna cuota
        List<Fee> cuotasCobradas = FeePaymentAllocator.aplicar(
                sale, paid, LocalDateTime.now(), PaymentMethod.EFECTIVO);
        cuotasCobradas.forEach(feeRepository::save);

        // El saldo sale de los cobros registrados, no de una resta a mano
        sale.recalcularSaldo();

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

        // Una venta anulada no se edita: el estado CANCELADO es terminal.
        if (existingSale.getStatus() == SaleStatus.CANCELED) {
            throw new IllegalStateException("No se puede editar una venta anulada");
        }

        SaleKind kind = SaleKind.fromString(sale.getKind());

        ProductType productType = null;
        if (sale.getProductType() != null) {
            productType = productTypeRepository.findById(sale.getProductType())
                    .orElseThrow(() -> new Exception("Categoría de producto no encontrada"));
            // la categoria tiene que ser del mismo negocio
            TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);
        }

        ProductVariant variant = null;
        Product product = null;
        if (sale.getVariant() != null) {
            variant = variantRepository.findById(sale.getVariant())
                    .orElseThrow(() -> new Exception("Producto no encontrado"));
            // la variante y su producto tienen que ser del mismo negocio
            TenantGuard.verificar(variant.getBusiness() != null ? variant.getBusiness().getId() : null);
            product = variant.getProduct();
            if (product == null) {
                throw new IllegalArgumentException("La variante no tiene un producto asociado");
            }
            TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);
        }

        // La operación conserva la moneda con la que se creó: asociarle un producto
        // de otra moneda dejaría el detalle mostrando importes en la moneda equivocada.
        // Sólo se compara cuando ambos registros tienen moneda (los legados no).
        if (product != null && tieneMoneda(existingSale.getCurrency()) && tieneMoneda(product.getCurrency())
                && !existingSale.getCurrency().trim().equalsIgnoreCase(product.getCurrency().trim())) {
            throw new IllegalArgumentException("El producto está en " + product.getCurrency().trim().toUpperCase()
                    + " y la operación en " + existingSale.getCurrency().trim().toUpperCase());
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

        // La variante puede cambiar en la edición: se devuelve el stock de la
        // anterior y se descuenta el de la nueva. Los importes ya pactados no se
        // tocan (el alta/edición no expone montos); sólo se actualiza el snapshot
        // de precio y costo unitarios para el detalle.
        int cantidad = existingSale.getQuantity() != null ? existingSale.getQuantity() : 1;
        ProductVariant anterior = existingSale.getVariant();
        boolean eraVenta = existingSale.getKind() == SaleKind.VENTA && anterior != null;
        boolean seraVenta = kind == SaleKind.VENTA && variant != null;
        boolean cambiaVariante = !seraVenta || anterior == null
                ? eraVenta
                : anterior.getId() == null || !anterior.getId().equals(variant.getId());

        if (eraVenta && cambiaVariante) {
            anterior.setStock(anterior.getStock() + cantidad);
            variantRepository.save(anterior);
        }
        if (seraVenta && cambiaVariante) {
            int disponible = variant.getStock() != null ? variant.getStock() : 0;
            if (disponible < cantidad) {
                throw new IllegalArgumentException(disponible == 0
                        ? "No hay stock disponible de " + variant.getSku()
                        : "Stock insuficiente de " + variant.getSku() + ": quedan " + disponible + " unidades");
            }
            variant.setStock(disponible - cantidad);
            variantRepository.save(variant);
        }

        existingSale.setKind(kind);
        existingSale.setProductType(productType);
        existingSale.setVariant(variant);
        if (variant != null) {
            existingSale.setUnitPrice(variant.getPrice());
            existingSale.setCost(variant.getCost().multiply(BigDecimal.valueOf(cantidad)));
        }
        existingSale.setDescriptionProduct(description);

        // El vendedor de la operación se corrige sólo desde acá (deliberado):
        // reasignar la cartera del cliente no reescribe la autoría histórica.
        // Null = sin atribuir.
        existingSale.setSeller(resolverVendedorDeOperacion(sale.getSellerId(), existingSale.getClient()));

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

    // método para sumar lo cobrado en las cuotas de una venta
    public BigDecimal calculateTotalPaidFeesForSaleUsingStream(Sale sale) {
        if (sale == null || sale.getFees() == null) {
            return BigDecimal.ZERO;
        }
        return sale.getFees().stream()
                .map(Fee::getPaidAmount)
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
        BigDecimal maxAmount,
        Long sellerId,
        Boolean withoutSeller
        ) {
        Specification<Sale> spec = buildSalesSpecification(
            year, month, day, status, aCobrar, clientName, descriptionProduct, productType, kind,
            typePayments, minAmount, maxAmount, sellerId, withoutSeller
        );
        Page<SaleResponseDto> page = salesRepository.findAll(spec, pageable)
                .map(sale -> convertirAVentaResponseDTO(sale));
        adjuntarRemitos(page.getContent());
        return page;
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
            null, null, null, null, null
        );
        return salesRepository.count(spec);
    }

    private long countACobrar(SaleKind kind) {
        Specification<Sale> spec = buildSalesSpecification(
            null, null, null, null, true, null, null, null, kind,
            null, null, null, null, null
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
        BigDecimal maxAmount,
        Long sellerId,
        Boolean withoutSeller
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

            // Vendedor de la operación sellado al crearla; "sin vendedor" cubre
            // las ventas directas y los registros anteriores a la atribución.
            if (sellerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("seller").get("id"), sellerId));
            }

            if (Boolean.TRUE.equals(withoutSeller)) {
                predicates.add(criteriaBuilder.isNull(root.get("seller")));
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
        SaleResponseDto dto = convertirAVentaResponseDTO(sale);
        adjuntarRemito(dto);
        return dto;
    }

    /**
     * Completa el resumen de remito de una venta. La consulta es una sola y se
     * hace fuera del mapper para no acoplar el mapeo de ventas al repositorio.
     */
    private void adjuntarRemito(SaleResponseDto venta) {
        if (venta == null || venta.getId() == null) {
            return;
        }
        saleDocumentRepository.findBySaleIdAndType(venta.getId(), DocumentType.REMITO)
                .ifPresent(document -> venta.setRemito(SaleDocumentMapper.aResumen(document)));
    }

    /** Versión por lote para el listado paginado (evita N+1). */
    private void adjuntarRemitos(List<SaleResponseDto> ventas) {
        List<Long> ids = new ArrayList<>();
        for (SaleResponseDto venta : ventas) {
            if (venta.getId() != null) {
                ids.add(venta.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, SaleDocument> porVenta = new HashMap<>();
        for (SaleDocument document : saleDocumentRepository.findBySaleIdInAndType(ids, DocumentType.REMITO)) {
            if (document.getSale() != null) {
                porVenta.put(document.getSale().getId(), document);
            }
        }
        for (SaleResponseDto venta : ventas) {
            venta.setRemito(SaleDocumentMapper.aResumen(porVenta.get(venta.getId())));
        }
    }

    @Transactional
    @Override
    public void deleteVenta(Long id) throws Exception {
        Sale existingSale = salesRepository.findById(id)
                .orElseThrow(() -> new Exception("Sale not found"));
        TenantGuard.verificar(existingSale.getBusiness() != null ? existingSale.getBusiness().getId() : null);

        // Un remito emitido puede estar en manos del cliente y su número es
        // correlativo por negocio: la venta se anula en lugar de borrarse para no
        // dejar un comprobante huérfano ni liberar su número.
        boolean tieneRemito = saleDocumentRepository
                .findBySaleIdAndType(id, DocumentType.REMITO).isPresent();
        if (tieneRemito) {
            throw new IllegalArgumentException("La venta tiene un remito emitido. Anúlala en lugar de eliminarla.");
        }

        // Una venta con pagos registrados no se puede borrar físicamente; debe
        // anularse para conservar el historial del dinero que entró.
        boolean tienePagos = existingSale.getFees().stream()
                .anyMatch(fee -> fee.getPaidAmount().signum() > 0);
        if (tienePagos) {
            throw new IllegalArgumentException("La venta tiene pagos registrados. Anúlala en lugar de eliminarla.");
        }

        for (Fee fee : existingSale.getFees()) {
            feeRepository.delete(fee);
        }

        // Reponer stock (V9): las unidades vendidas vuelven al catálogo al eliminar
        // la venta. Si ya estaba anulada, el stock se repuso al anular: no se toca
        // de nuevo (antes se sumaba dos veces).
        if (existingSale.getStatus() != SaleStatus.CANCELED) {
            reponerStock(existingSale);
        }

        // Limpieza defensiva: si algún documento quedó (p. ej. de otro tipo), se
        // borra junto con la venta para no dejar huérfanos.
        saleDocumentRepository.deleteBySaleId(id);

        salesRepository.delete(existingSale);
    }

    /**
     * Devuelve al stock las unidades de la variante vendida. No aplica a
     * préstamos ni a registros sin variante (legados).
     */
    private void reponerStock(Sale sale) {
        if (sale.getKind() != SaleKind.VENTA || sale.getVariant() == null) {
            return;
        }
        ProductVariant variante = sale.getVariant();
        int cantidad = sale.getQuantity() != null ? sale.getQuantity() : 1;
        if (cantidad <= 0) {
            return;
        }
        variante.setStock((variante.getStock() != null ? variante.getStock() : 0) + cantidad);
        variantRepository.save(variante);
    }

    // Anula una venta o préstamo: conserva el registro con estado CANCELADO (queda
    // excluida de deudas, agenda y estadísticas) y deja la deuda en cero. Dos efectos
    // opcionales e independientes:
    //  - refund=true: asienta como devolución el monto ya cobrado (si false, no se
    //    registra ninguna devolución).
    //  - restock=true: devuelve las unidades al stock (sólo venta con producto); si
    //    false el inventario no se toca, y el borrado posterior tampoco lo repone.
    // Queda registrada la auditoría de la anulación: fecha, usuario y motivo.
    @Transactional
    @Override
    public SaleResponseDto cancelVenta(Long id, boolean refund, boolean restock, String reason) throws Exception {
        Sale sale = salesRepository.findById(id)
                .orElseThrow(() -> new Exception("Sale not found"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);

        // Ya anulada: idempotente. No vuelve a tocar stock, devoluciones ni auditoría.
        if (sale.getStatus() == SaleStatus.CANCELED) {
            SaleResponseDto dto = convertirAVentaResponseDTO(sale);
            adjuntarRemito(dto);
            return dto;
        }

        if (restock && (sale.getKind() != SaleKind.VENTA || sale.getVariant() == null)) {
            throw new IllegalArgumentException("Solo se puede reponer el stock de una venta con producto");
        }

        // Lo ya cobrado (cobros registrados en el log). Las cuotas se conservan como
        // historial del registro anulado.
        BigDecimal collected = sale.getFees().stream()
                .map(Fee::getPaidAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sale.setRefundAmount(refund ? collected : BigDecimal.ZERO);
        // La devolución se fecha al anular: los reportes la ubican en el mes en que
        // salió la plata, no en el de la venta.
        sale.setRefundAt(refund && collected.signum() > 0 ? LocalDate.now() : null);
        sale.setRemainingAmount(BigDecimal.ZERO);
        sale.setRealFinalDate(LocalDate.now());
        sale.setStatus(SaleStatus.CANCELED);

        // Auditoría: quién anuló, cuándo y por qué (el motivo es opcional).
        sale.setCanceledAt(LocalDateTime.now());
        sale.setCanceledBy(usuarioActual());
        sale.setCancelReason(limpiarTexto(reason));

        if (restock) {
            reponerStock(sale);
        }

        SaleResponseDto dto = convertirAVentaResponseDTO(salesRepository.save(sale));
        adjuntarRemito(dto);
        return dto;
    }

    /** Email del usuario autenticado, o null si no hay sesión (tests o datos legados). */
    private static String usuarioActual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return null;
    }

    /** Texto sin espacios al borde; null si queda vacío. */
    private static String limpiarTexto(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
