package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.app.digital.payments.digital_pyments.mappers.ProductMapper;
import com.app.digital.payments.digital_pyments.mappers.SaleDocumentMapper;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.models.SaleDocument;
import com.app.digital.payments.digital_pyments.models.dtos.SaleDocumentDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleDocumentRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
import com.app.digital.payments.digital_pyments.utils.DocumentType;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

/**
 * Emisión y consulta de documentos comerciales de una venta. Hoy solo se
 * emiten remitos: la infraestructura queda preparada para facturas.
 */
@Service
public class SaleDocumentServices {

    private final ISaleRepository saleRepository;
    private final ISaleDocumentRepository documentRepository;
    private final IBusinessRepository businessRepository;

    public SaleDocumentServices(ISaleRepository saleRepository,
            ISaleDocumentRepository documentRepository,
            IBusinessRepository businessRepository) {
        this.saleRepository = saleRepository;
        this.documentRepository = documentRepository;
        this.businessRepository = businessRepository;
    }

    /**
     * Devuelve el remito ya emitido de la venta, si existe.
     */
    @Transactional(readOnly = true)
    public Optional<SaleDocumentDto> obtenerRemito(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);
        return documentRepository.findBySaleIdAndType(saleId, DocumentType.REMITO)
                .map(SaleDocumentMapper::aDto);
    }

    /**
     * Emite el remito de una venta. Es idempotente: si ya existe, devuelve el
     * snapshot original sin asignar un número nuevo ni modificar nada.
     *
     * La numeración es correlativa por negocio y tipo. Se bloquea la fila del
     * negocio para que dos emisiones simultáneas no repitan número.
     */
    @Transactional
    public SaleDocumentDto emitirRemito(Long saleId, String notes) {
        Long businessId = TenantContext.requireBusinessId();
        Business business = businessRepository.findByIdForUpdate(businessId)
                .orElseThrow(() -> new IllegalStateException("El negocio autenticado no existe"));

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        TenantGuard.verificar(sale.getBusiness() != null ? sale.getBusiness().getId() : null);

        Optional<SaleDocument> existente = documentRepository
                .findBySaleIdAndType(saleId, DocumentType.REMITO);
        if (existente.isPresent()) {
            return SaleDocumentMapper.aDto(existente.get());
        }

        if (sale.getKind() != SaleKind.VENTA) {
            throw new IllegalArgumentException("Solo se puede emitir un remito de una venta");
        }
        if (sale.getStatus() == SaleStatus.CANCELED) {
            throw new IllegalArgumentException("No se puede emitir un remito de una venta anulada");
        }

        Long ultimoNumero = documentRepository.findMaxNumber(businessId, DocumentType.REMITO);
        long numero = (ultimoNumero == null ? 0L : ultimoNumero) + 1L;

        Client client = sale.getClient();
        ProductVariant variant = sale.getVariant();
        int cantidad = sale.getQuantity() != null && sale.getQuantity() > 0 ? sale.getQuantity() : 1;

        SaleDocument document = new SaleDocument();
        document.setBusiness(business);
        document.setSale(sale);
        document.setType(DocumentType.REMITO);
        document.setNumber(numero);
        document.setIssuedAt(LocalDateTime.now());
        document.setSaleDate(sale.getDateCreation());

        document.setBusinessName(business.getName());
        document.setBusinessAddress(limpiar(business.getAddress()));
        document.setBusinessTaxId(limpiar(business.getCuit()));
        document.setBusinessPhone(limpiar(business.getPhone()));

        document.setClientName(client.getName());
        document.setClientDoc(limpiar(client.getDni()));
        document.setClientAddress(limpiar(client.getDireccion()));
        document.setClientPhone(limpiar(client.getTelefono()));

        if (variant != null) {
            document.setProductName(variant.getProduct() != null
                    ? variant.getProduct().getName()
                    : sale.getDescriptionProduct());
            String etiqueta = ProductMapper.etiquetaDe(variant);
            document.setVariantLabel(limpiar(etiqueta));
            document.setSku(limpiar(variant.getSku()));
        } else {
            // Ventas legadas sin variante: se imprime la descripción guardada.
            document.setProductName(sale.getDescriptionProduct());
            document.setVariantLabel(null);
            document.setSku(null);
        }

        document.setQuantity(cantidad);
        document.setUnitPrice(precioUnitario(sale, variant, cantidad));
        document.setTotal(sale.getPriceTotal());
        document.setCurrency(moneda(sale, business));
        document.setNotes(limpiar(notes));

        SaleDocument guardado = documentRepository.save(document);
        return SaleDocumentMapper.aDto(guardado);
    }

    private BigDecimal precioUnitario(Sale sale, ProductVariant variant, int cantidad) {
        if (sale.getUnitPrice() != null) {
            return sale.getUnitPrice();
        }
        if (variant != null && variant.getPrice() != null) {
            return variant.getPrice();
        }
        BigDecimal total = sale.getPriceTotal() != null ? sale.getPriceTotal() : BigDecimal.ZERO;
        return total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private String moneda(Sale sale, Business business) {
        if (sale.getCurrency() != null && !sale.getCurrency().isBlank()) {
            return sale.getCurrency();
        }
        return CurrencyUtils.orDefault(business.getCurrency(), "ARS");
    }

    private String limpiar(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
