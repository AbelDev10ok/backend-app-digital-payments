package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.mappers.ProductMapper;
import com.app.digital.payments.digital_pyments.models.Attribute;
import com.app.digital.payments.digital_pyments.models.AttributeValue;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.PriceChange;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductAttribute;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.VariantAttribute;
import com.app.digital.payments.digital_pyments.models.dtos.BulkPriceRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.BulkPriceResultDto;
import com.app.digital.payments.digital_pyments.models.dtos.BulkPriceRowDto;
import com.app.digital.payments.digital_pyments.models.dtos.PriceChangeDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductVariantRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IPriceChangeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductVariantRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
import com.app.digital.payments.digital_pyments.utils.SlugUtils;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

/**
 * Catálogo: producto base + variantes (SKU).
 *
 * Cada variante guarda su costo, su precio de venta y su stock (obligatorio).
 * Un producto sin atributos tiene una única variante; uno con atributos define
 * una variante por combinación (un valor por atributo, sin duplicados).
 */
@Service
public class ProductServices {

    private final IProductRepository productRepository;
    private final IProductVariantRepository variantRepository;
    private final IAttributeRepository attributeRepository;
    private final IProductTypeRepository productTypeRepository;
    private final ISaleRepository saleRepository;
    private final IBusinessRepository businessRepository;
    private final IPriceChangeRepository priceChangeRepository;
    private final StockServices stockServices;

    public ProductServices(IProductRepository productRepository, IProductVariantRepository variantRepository,
            IAttributeRepository attributeRepository, IProductTypeRepository productTypeRepository,
            ISaleRepository saleRepository, IBusinessRepository businessRepository,
            IPriceChangeRepository priceChangeRepository, StockServices stockServices) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.attributeRepository = attributeRepository;
        this.productTypeRepository = productTypeRepository;
        this.saleRepository = saleRepository;
        this.businessRepository = businessRepository;
        this.priceChangeRepository = priceChangeRepository;
        this.stockServices = stockServices;
    }

    @Transactional
    public List<ProductDto> createProduct(ProductRequestDto request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }

        String nombre = request.getName().trim();
        Long negocioId = TenantContext.requireBusinessId();

        if (productRepository.existsByNameAndBusinessId(nombre, negocioId)) {
            throw new IllegalArgumentException("Ya existe un producto con ese nombre en el negocio");
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()
                && !CurrencyUtils.esSoportada(request.getCurrency())) {
            throw new IllegalArgumentException(
                    "La moneda debe ser " + String.join(" o ", CurrencyUtils.MONEDAS_SOPORTADAS));
        }
        if (request.getProductTypeId() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }

        ProductType categoria = productTypeRepository.findById(request.getProductTypeId())
                .orElseThrow(() -> new IllegalArgumentException("La categoría indicada no existe"));
        TenantGuard.verificar(categoria.getBusiness() != null ? categoria.getBusiness().getId() : null);

        // Los productos se asignan a categorías hoja: un padre sólo agrupa.
        if (productTypeRepository.existsByParentId(categoria.getId())) {
            throw new IllegalArgumentException(
                    "La categoría agrupa subcategorías: elegí una subcategoría para el producto");
        }

        Business business = businessRepository.getReferenceById(negocioId);

        Product product = new Product();
        product.setName(nombre);
        product.setDescription(limpiar(request.getDescription()));
        product.setBrand(limpiar(request.getBrand()));
        product.setActive(request.getActive() == null || request.getActive());
        product.setProductType(categoria);
        product.setBusiness(business);
        // La moneda se congela al crear: los importes guardados no se reetiquetan.
        product.setCurrency(CurrencyUtils.orDefault(request.getCurrency(), CurrencyUtils.snapshot(business)));

        List<Attribute> atributos = cargarAtributos(request.getAttributeIds());
        aplicarAtributos(product, atributos);

        List<ProductVariant> variantes = construirVariantes(product, request.getVariants(), atributos, null);
        product.setVariants(variantes);

        productRepository.save(product);
        // El stock inicial queda explicado por el libro: saldo de apertura.
        variantes.forEach(stockServices::registrarCargaInicialSiFalta);
        return variantes.stream().map(v -> ProductMapper.toDto(product, v)).toList();
    }

    @Transactional
    public boolean updateProduct(Long id, ProductRequestDto request) {
        if (id == null || request == null) {
            return false;
        }
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return false;
        }
        TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        String nombre = request.getName().trim();
        Long negocioId = TenantContext.requireBusinessId();
        if (productRepository.existsByNameAndBusinessIdAndIdNot(nombre, negocioId, id)) {
            throw new IllegalArgumentException("Otro producto ya usa ese nombre en el negocio");
        }

        // La moneda del precio queda fija al crear el producto
        if (request.getCurrency() != null && !request.getCurrency().isBlank()
                && product.getCurrency() != null && !product.getCurrency().isBlank()
                && !product.getCurrency().trim().equalsIgnoreCase(request.getCurrency().trim())) {
            throw new IllegalArgumentException(
                    "La moneda del producto (" + product.getCurrency().trim().toUpperCase() + ") no se puede cambiar");
        }

        if (request.getProductTypeId() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        ProductType categoria = productTypeRepository.findById(request.getProductTypeId())
                .orElseThrow(() -> new IllegalArgumentException("La categoría indicada no existe"));
        TenantGuard.verificar(categoria.getBusiness() != null ? categoria.getBusiness().getId() : null);

        // Los productos se asignan a categorías hoja. Editar un producto que ya
        // estaba en un padre (dato viejo) se permite mientras no cambie de categoría.
        ProductType categoriaActual = product.getProductType();
        boolean mismaCategoria = categoriaActual != null && categoria.getId().equals(categoriaActual.getId());
        if (!mismaCategoria && productTypeRepository.existsByParentId(categoria.getId())) {
            throw new IllegalArgumentException(
                    "La categoría agrupa subcategorías: elegí una subcategoría para el producto");
        }

        // El conjunto de atributos define las variantes: cambiarlo después de crear
        // el producto dejaría combinaciones viejas sin sentido. Se rechaza.
        Set<Long> atributosActuales = product.getAttributes().stream()
                .map(pa -> pa.getAttribute().getId())
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> atributosPedidos = idsAtributos(request.getAttributeIds());
        if (!atributosActuales.equals(atributosPedidos)) {
            throw new IllegalArgumentException(
                    "Los atributos que generan variantes no se pueden cambiar después de crear el producto");
        }
        List<Attribute> atributos = cargarAtributos(request.getAttributeIds());

        product.setName(nombre);
        product.setDescription(limpiar(request.getDescription()));
        product.setBrand(limpiar(request.getBrand()));
        product.setActive(request.getActive() == null || request.getActive());
        product.setProductType(categoria);

        actualizarVariantes(product, request.getVariants(), atributos);
        productRepository.save(product);
        // Las variantes nuevas de una edición también necesitan su saldo inicial.
        product.getVariants().forEach(stockServices::registrarCargaInicialSiFalta);
        return true;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .flatMap(product -> product.getVariants().stream()
                        .map(variant -> ProductMapper.toDto(product, variant)))
                .toList();
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        if (id == null) {
            return false;
        }
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return false;
        }
        TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);

        // Un producto con ventas no se borra: se desactiva. Si ya no tiene ventas,
        // se eliminan sus variantes en cascada.
        boolean conVentas = product.getVariants().stream()
                .anyMatch(variant -> saleRepository.existsByVariantId(variant.getId()));
        if (conVentas) {
            throw new IllegalArgumentException(
                    "El producto tiene ventas asociadas; desactivalo en lugar de eliminarlo");
        }
        productRepository.delete(product);
        return true;
    }

    // ------------------------------------------------------------------
    // Variantes y atributos
    // ------------------------------------------------------------------

    private List<Attribute> cargarAtributos(List<Long> ids) {
        Set<Long> unicos = idsAtributos(ids);
        List<Attribute> atributos = new ArrayList<>();
        for (Long atributoId : unicos) {
            Attribute atributo = attributeRepository.findById(atributoId)
                    .orElseThrow(() -> new IllegalArgumentException("El atributo indicado no existe"));
            TenantGuard.verificar(atributo.getBusiness() != null ? atributo.getBusiness().getId() : null);
            atributos.add(atributo);
        }
        return atributos;
    }

    private static Set<Long> idsAtributos(List<Long> ids) {
        if (ids == null) {
            return new HashSet<>();
        }
        return ids.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private void aplicarAtributos(Product product, List<Attribute> atributos) {
        product.getAttributes().clear();
        for (Attribute atributo : atributos) {
            ProductAttribute pa = new ProductAttribute();
            pa.setProduct(product);
            pa.setAttribute(atributo);
            product.getAttributes().add(pa);
        }
    }

    private List<ProductVariant> construirVariantes(Product product, List<ProductVariantRequestDto> pedidas,
            List<Attribute> atributos, Map<Long, ProductVariant> existentes) {
        if (pedidas == null || pedidas.isEmpty()) {
            throw new IllegalArgumentException("El producto debe tener al menos una variante");
        }
        if (atributos.isEmpty() && pedidas.size() > 1) {
            throw new IllegalArgumentException(
                    "Un producto sin atributos sólo puede tener una variante");
        }

        List<ProductVariant> resultado = new ArrayList<>();
        Set<String> combinaciones = new HashSet<>();

        int indice = 0;
        for (ProductVariantRequestDto dto : pedidas) {
            indice++;
            if (dto == null) {
                continue;
            }
            ProductVariant variante = existentes != null && dto.getId() != null
                    ? existentes.get(dto.getId())
                    : new ProductVariant();
            if (variante == null) {
                throw new IllegalArgumentException("La variante indicada no pertenece al producto");
            }

            variante.setProduct(product);
            variante.setBusiness(product.getBusiness());

            aplicarDatosVariante(variante, dto, indice, product.getName());
            aplicarAtributosVariante(variante, dto.getAttributeValueIds(), atributos);

            String combinacion = combinacionDe(variante);
            if (!combinaciones.add(combinacion)) {
                throw new IllegalArgumentException(
                        "Hay dos variantes con la misma combinación de atributos");
            }
            resultado.add(variante);
        }

        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("El producto debe tener al menos una variante");
        }
        return resultado;
    }

    private void actualizarVariantes(Product product, List<ProductVariantRequestDto> pedidas,
            List<Attribute> atributos) {
        if (pedidas == null || pedidas.isEmpty()) {
            throw new IllegalArgumentException("El producto debe tener al menos una variante");
        }
        if (atributos.isEmpty() && pedidas.size() > 1) {
            throw new IllegalArgumentException(
                    "Un producto sin atributos sólo puede tener una variante");
        }

        Map<Long, ProductVariant> existentes = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v, (a, b) -> a, LinkedHashMap::new));

        Set<Long> pedidasIds = pedidas.stream()
                .map(ProductVariantRequestDto::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Variantes quitadas: sólo se pueden borrar si nunca se vendieron.
        List<ProductVariant> aQuitar = new ArrayList<>();
        for (ProductVariant existente : existentes.values()) {
            if (!pedidasIds.contains(existente.getId())) {
                if (saleRepository.existsByVariantId(existente.getId())) {
                    throw new IllegalArgumentException(
                            "No se puede quitar una variante que tiene ventas; desactivala en su lugar");
                }
                aQuitar.add(existente);
            }
        }
        product.getVariants().removeAll(aQuitar);

        List<ProductVariant> resultado = construirVariantes(product, pedidas, atributos, existentes);
        product.getVariants().clear();
        product.getVariants().addAll(resultado);
    }

    private void aplicarDatosVariante(ProductVariant variante, ProductVariantRequestDto dto, int indice,
            String nombreProducto) {
        BigDecimal costo = dto.getCost();
        BigDecimal precio = dto.getPrice();
        if (costo == null || costo.signum() < 0) {
            throw new IllegalArgumentException("El costo de la variante no puede ser negativo");
        }
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor a 0");
        }
        if (precio.compareTo(costo) <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor al costo");
        }
        // El stock no se edita desde el producto: en el alta es el saldo inicial y
        // en la edición sólo se acepta el valor actual. Los cambios van por el
        // libro de movimientos; si no, editar el precio con el formulario abierto
        // pisaría el stock con un valor viejo.
        if (variante.getId() == null) {
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("El stock es obligatorio y no puede ser negativo");
            }
            variante.setStock(dto.getStock());
        } else {
            int actual = variante.getStock() != null ? variante.getStock() : 0;
            if (dto.getStock() != null && !dto.getStock().equals(actual)) {
                throw new IllegalArgumentException(
                        "Para cambiar el stock usá un ajuste de inventario (stock actual: " + actual + ")");
            }
        }

        String sku = limpiar(dto.getSku());
        Long negocioId = TenantContext.requireBusinessId();
        if (sku == null) {
            sku = generarSku(negocioId, nombreProducto, indice);
        }
        sku = sku.toUpperCase(Locale.ROOT);
        boolean skuDuplicado = variante.getId() == null
                ? variantRepository.existsByBusinessIdAndSku(negocioId, sku)
                : variantRepository.existsByBusinessIdAndSkuAndIdNot(negocioId, sku, variante.getId());
        if (skuDuplicado) {
            throw new IllegalArgumentException("Ya existe una variante con el SKU " + sku + " en el negocio");
        }

        String barcode = limpiar(dto.getBarcode());
        if (barcode != null) {
            boolean codigoDuplicado = variante.getId() == null
                    ? variantRepository.existsByBusinessIdAndBarcode(negocioId, barcode)
                    : variantRepository.existsByBusinessIdAndBarcodeAndIdNot(negocioId, barcode, variante.getId());
            if (codigoDuplicado) {
                throw new IllegalArgumentException(
                        "Ya existe una variante con el código de barras " + barcode + " en el negocio");
            }
        }

        variante.setSku(sku);
        variante.setCost(costo);
        variante.setPrice(precio);
        variante.setBarcode(barcode);
        variante.setActive(dto.getActive() == null || dto.getActive());
    }

    private void aplicarAtributosVariante(ProductVariant variante, List<Long> valoresIds,
            List<Attribute> atributos) {
        Set<Long> ids = valoresIds != null ? new HashSet<>(valoresIds) : new HashSet<>();
        if (atributos.isEmpty()) {
            if (!ids.isEmpty()) {
                throw new IllegalArgumentException(
                        "El producto no tiene atributos; la variante no puede tener valores");
            }
            variante.getAttributes().clear();
            return;
        }

        Set<Long> atributosCubiertos = new HashSet<>();
        List<AttributeValue> valores = new ArrayList<>();
        for (Attribute atributo : atributos) {
            AttributeValue valor = atributo.getValues().stream()
                    .filter(v -> ids.contains(v.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Falta el valor del atributo " + atributo.getName()));
            if (!atributosCubiertos.add(atributo.getId())) {
                throw new IllegalArgumentException(
                        "La variante tiene más de un valor para el atributo " + atributo.getName());
            }
            valores.add(valor);
        }

        if (ids.size() != atributosCubiertos.size()) {
            throw new IllegalArgumentException(
                    "La variante tiene valores de atributos que el producto no declara");
        }

        // Los vínculos existentes se reutilizan: variant_attribute es único por
        // (variante, valor) y recrear una fila ya guardada hace que Hibernate
        // inserte antes de borrar el vínculo viejo (rompe el guardado).
        Set<Long> valoresPedidos = valores.stream()
                .map(AttributeValue::getId)
                .collect(Collectors.toSet());
        variante.getAttributes().removeIf(va -> va.getAttributeValue() == null
                || !valoresPedidos.contains(va.getAttributeValue().getId()));

        Set<Long> vinculados = variante.getAttributes().stream()
                .map(va -> va.getAttributeValue().getId())
                .collect(Collectors.toSet());
        for (AttributeValue valor : valores) {
            if (vinculados.contains(valor.getId())) {
                continue;
            }
            VariantAttribute va = new VariantAttribute();
            va.setVariant(variante);
            va.setAttributeValue(valor);
            variante.getAttributes().add(va);
        }
    }

    private static String combinacionDe(ProductVariant variante) {
        return variante.getAttributes().stream()
                .map(va -> va.getAttributeValue().getId())
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }

    private String generarSku(Long negocioId, String nombreProducto, int indice) {
        String base = SlugUtils.slugify(nombreProducto);
        if (base.isBlank()) {
            base = "SKU";
        }
        if (base.length() > 90) {
            base = base.substring(0, 90);
        }
        String candidato = base + "-" + indice;
        int sufijo = 1;
        while (variantRepository.existsByBusinessIdAndSku(negocioId, candidato)) {
            candidato = base + "-" + indice + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }

    // ------------------------------------------------------------------
    // Ajuste masivo de precios con historial
    // ------------------------------------------------------------------

    private static final String ALCANCE_TODOS = "ALL";
    private static final String ALCANCE_CATEGORIA = "CATEGORY";
    private static final String ALCANCE_SKUS = "VARIANTS";
    private static final String MODO_PORCENTAJE = "PERCENT";
    private static final String MODO_MONTO = "AMOUNT";

    /** Un SKU con el precio y costo que le dejaría el ajuste (todavía sin guardar). */
    private record Ajuste(ProductVariant variante, BigDecimal precioNuevo, BigDecimal costoNuevo) {
        boolean cambia() {
            return variante.getPrice().compareTo(precioNuevo) != 0
                    || variante.getCost().compareTo(costoNuevo) != 0;
        }
    }

    /**
     * Calcula, sin tocar nada, qué quedaría en cada SKU del alcance. Es lo que
     * ve el usuario antes de aplicar: mismo criterio que la aplicación real.
     */
    @Transactional(readOnly = true)
    public BulkPriceResultDto previewBulkPrice(BulkPriceRequestDto request) {
        return aResultado(calcularAjustes(request), null);
    }

    /**
     * Aplica el ajuste en una sola transacción y deja el historial. Los SKUs que
     * el ajuste no mueve no se escriben ni generan historial.
     */
    @Transactional
    public BulkPriceResultDto applyBulkPrice(BulkPriceRequestDto request) {
        List<Ajuste> ajustes = calcularAjustes(request);
        List<Ajuste> cambios = ajustes.stream().filter(Ajuste::cambia).toList();
        if (cambios.isEmpty()) {
            throw new IllegalArgumentException("El ajuste no cambia ningún precio: revisá el valor");
        }

        String batchId = UUID.randomUUID().toString();
        String usuario = usuarioActual();
        LocalDateTime ahora = LocalDateTime.now();
        List<PriceChange> historial = new ArrayList<>(cambios.size());
        List<ProductVariant> variantes = new ArrayList<>(cambios.size());
        // El resultado se arma antes de mutar: después, el "antes" y el "después"
        // serían el mismo valor y ningún SKU figuraría como cambiado.
        BulkPriceResultDto resultado = aResultado(ajustes, null);

        for (Ajuste ajuste : cambios) {
            ProductVariant variante = ajuste.variante();
            BigDecimal precioAnterior = variante.getPrice();
            BigDecimal costoAnterior = variante.getCost();
            variante.setPrice(ajuste.precioNuevo());
            variante.setCost(ajuste.costoNuevo());
            variantes.add(variante);
            historial.add(historialDe(variante, precioAnterior, costoAnterior, ajuste.precioNuevo(),
                    ajuste.costoNuevo(), request, batchId, ahora, usuario));
        }

        variantRepository.saveAll(variantes);
        priceChangeRepository.saveAll(historial);
        resultado.setBatchId(batchId);
        return resultado;
    }

    /** Historial de precios de un producto (los 100 cambios más recientes). */
    @Transactional(readOnly = true)
    public List<PriceChangeDto> priceHistory(Long productId) {
        Long negocioId = TenantContext.requireBusinessId();
        Product producto = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("El producto indicado no existe"));
        TenantGuard.verificar(producto.getBusiness() != null ? producto.getBusiness().getId() : null);
        return priceChangeRepository
                .findTop100ByBusinessIdAndProductIdOrderByCreatedAtDescIdDesc(negocioId, productId).stream()
                .map(this::aHistorialDto)
                .toList();
    }

    private List<Ajuste> calcularAjustes(BulkPriceRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Falta el ajuste a aplicar");
        }
        Long negocioId = TenantContext.requireBusinessId();
        validarAjuste(request);
        List<ProductVariant> variantes = variantesDelAlcance(request, negocioId);
        if (variantes.isEmpty()) {
            throw new IllegalArgumentException("No hay SKUs en el alcance elegido");
        }

        List<Ajuste> ajustes = new ArrayList<>(variantes.size());
        for (ProductVariant variante : variantes) {
            BigDecimal precio = ajustar(variante.getPrice(), request);
            BigDecimal costo = Boolean.TRUE.equals(request.getAdjustCost())
                    ? ajustar(variante.getCost(), request)
                    : variante.getCost();
            if (request.getRoundTo() != null) {
                precio = redondear(precio, request.getRoundTo());
            }
            if (precio.signum() <= 0) {
                throw new IllegalArgumentException(
                        "El ajuste deja en cero o negativo el precio del SKU " + variante.getSku());
            }
            if (costo.signum() < 0) {
                throw new IllegalArgumentException(
                        "El ajuste deja en negativo el costo del SKU " + variante.getSku());
            }
            // Misma regla que el alta/edición: el precio de venta supera al costo.
            if (precio.compareTo(costo) <= 0) {
                throw new IllegalArgumentException("El ajuste deja el precio del SKU " + variante.getSku()
                        + " por debajo de su costo. Ajustá también el costo o cambiá el valor.");
            }
            ajustes.add(new Ajuste(variante, precio, costo));
        }

        ajustes.sort(Comparator
                .comparing((Ajuste ajuste) -> nombreDe(ajuste.variante()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ajuste -> ajuste.variante().getSku(), String.CASE_INSENSITIVE_ORDER));
        return ajustes;
    }

    private void validarAjuste(BulkPriceRequestDto request) {
        String modo = modoDe(request);
        if (!MODO_PORCENTAJE.equals(modo) && !MODO_MONTO.equals(modo)) {
            throw new IllegalArgumentException("El ajuste debe ser por porcentaje o por monto fijo");
        }
        if (request.getValue() == null || request.getValue().signum() == 0) {
            throw new IllegalArgumentException("El valor del ajuste no puede ser cero");
        }
        if (MODO_PORCENTAJE.equals(modo) && request.getValue().compareTo(BigDecimal.valueOf(-100)) <= 0) {
            throw new IllegalArgumentException("El porcentaje del ajuste debe ser mayor a -100%");
        }
        if (request.getRoundTo() != null && request.getRoundTo().signum() <= 0) {
            throw new IllegalArgumentException("El redondeo debe ser mayor a cero");
        }
    }

    private List<ProductVariant> variantesDelAlcance(BulkPriceRequestDto request, Long negocioId) {
        String alcance = request.getScope() == null ? "" : request.getScope().trim().toUpperCase(Locale.ROOT);
        return switch (alcance) {
            case ALCANCE_TODOS -> variantRepository.findAllByBusinessId(negocioId);
            case ALCANCE_CATEGORIA -> {
                if (request.getCategoryId() == null) {
                    throw new IllegalArgumentException("Elegí la categoría a ajustar");
                }
                ProductType categoria = productTypeRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new IllegalArgumentException("La categoría indicada no existe"));
                TenantGuard.verificar(categoria.getBusiness() != null ? categoria.getBusiness().getId() : null);
                yield variantRepository.findAllByBusinessIdAndProductProductTypeIdIn(negocioId,
                        ramaDeCategoria(categoria.getId(), negocioId));
            }
            case ALCANCE_SKUS -> {
                List<Long> ids = request.getVariantIds() == null ? List.of()
                        : request.getVariantIds().stream().filter(Objects::nonNull).distinct().toList();
                if (ids.isEmpty()) {
                    throw new IllegalArgumentException("Elegí al menos un SKU para ajustar");
                }
                List<ProductVariant> encontradas = variantRepository.findAllByBusinessIdAndIdIn(negocioId, ids);
                if (encontradas.size() != ids.size()) {
                    throw new IllegalArgumentException("Algunos SKUs no pertenecen al negocio");
                }
                yield encontradas;
            }
            default -> throw new IllegalArgumentException(
                    "El alcance del ajuste debe ser todo el catálogo, una categoría o SKUs elegidos");
        };
    }

    /** La categoría y todas sus subcategorías: ajustar un padre alcanza su rama. */
    private Set<Long> ramaDeCategoria(Long categoriaId, Long negocioId) {
        List<ProductType> todas = productTypeRepository.findAllByBusinessId(negocioId);
        Set<Long> rama = new HashSet<>();
        rama.add(categoriaId);
        boolean agrego = true;
        while (agrego) {
            agrego = false;
            for (ProductType tipo : todas) {
                Long padreId = tipo.getParent() != null ? tipo.getParent().getId() : null;
                if (padreId != null && rama.contains(padreId) && rama.add(tipo.getId())) {
                    agrego = true;
                }
            }
        }
        return rama;
    }

    private static BigDecimal ajustar(BigDecimal valor, BulkPriceRequestDto request) {
        if (MODO_PORCENTAJE.equals(modoDe(request))) {
            BigDecimal factor = BigDecimal.ONE
                    .add(request.getValue().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            return valor.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }
        return valor.add(request.getValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal redondear(BigDecimal valor, BigDecimal paso) {
        return valor.divide(paso, 0, RoundingMode.HALF_UP).multiply(paso).setScale(2, RoundingMode.HALF_UP);
    }

    private static String modoDe(BulkPriceRequestDto request) {
        return request.getMode() == null ? "" : request.getMode().trim().toUpperCase(Locale.ROOT);
    }

    private static String nombreDe(ProductVariant variante) {
        return variante.getProduct() != null && variante.getProduct().getName() != null
                ? variante.getProduct().getName()
                : "";
    }

    private BulkPriceResultDto aResultado(List<Ajuste> ajustes, String batchId) {
        List<BulkPriceRowDto> filas = new ArrayList<>(ajustes.size());
        int cambiados = 0;
        for (Ajuste ajuste : ajustes) {
            ProductVariant variante = ajuste.variante();
            Product producto = variante.getProduct();
            BulkPriceRowDto fila = new BulkPriceRowDto();
            fila.setVariantId(variante.getId());
            fila.setProductId(producto != null ? producto.getId() : null);
            fila.setProductName(producto != null ? producto.getName() : null);
            fila.setSku(variante.getSku());
            fila.setVariantLabel(ProductMapper.etiquetaDe(variante));
            fila.setCurrency(producto != null ? producto.getCurrency() : null);
            fila.setOldPrice(variante.getPrice());
            fila.setNewPrice(ajuste.precioNuevo());
            fila.setOldCost(variante.getCost());
            fila.setNewCost(ajuste.costoNuevo());
            boolean cambia = ajuste.cambia();
            fila.setChanged(cambia);
            if (cambia) {
                cambiados++;
            }
            filas.add(fila);
        }

        BulkPriceResultDto resultado = new BulkPriceResultDto();
        resultado.setRows(filas);
        resultado.setTotalCount(filas.size());
        resultado.setChangedCount(cambiados);
        resultado.setUnchangedCount(filas.size() - cambiados);
        resultado.setBatchId(batchId);
        return resultado;
    }

    private PriceChange historialDe(ProductVariant variante, BigDecimal precioAnterior, BigDecimal costoAnterior,
            BigDecimal precioNuevo, BigDecimal costoNuevo, BulkPriceRequestDto request, String batchId,
            LocalDateTime fecha, String usuario) {
        Product producto = variante.getProduct();
        PriceChange cambio = new PriceChange();
        cambio.setBusiness(variante.getBusiness());
        cambio.setVariantId(variante.getId());
        cambio.setProductId(producto != null ? producto.getId() : null);
        cambio.setProductName(producto != null ? producto.getName() : null);
        cambio.setSku(variante.getSku());
        cambio.setCurrency(producto != null ? producto.getCurrency() : null);
        cambio.setOldPrice(precioAnterior);
        cambio.setNewPrice(precioNuevo);
        cambio.setOldCost(costoAnterior);
        cambio.setNewCost(costoNuevo);
        cambio.setAdjustmentMode(modoDe(request));
        cambio.setAdjustmentValue(request.getValue());
        cambio.setAdjustCost(Boolean.TRUE.equals(request.getAdjustCost()));
        cambio.setRoundTo(request.getRoundTo());
        cambio.setBatchId(batchId);
        cambio.setCreatedAt(fecha);
        cambio.setCreatedBy(usuario);
        return cambio;
    }

    private PriceChangeDto aHistorialDto(PriceChange cambio) {
        PriceChangeDto dto = new PriceChangeDto();
        dto.setId(cambio.getId());
        dto.setVariantId(cambio.getVariantId());
        dto.setProductId(cambio.getProductId());
        dto.setProductName(cambio.getProductName());
        dto.setSku(cambio.getSku());
        dto.setCurrency(cambio.getCurrency());
        dto.setOldPrice(cambio.getOldPrice());
        dto.setNewPrice(cambio.getNewPrice());
        dto.setOldCost(cambio.getOldCost());
        dto.setNewCost(cambio.getNewCost());
        dto.setAdjustmentMode(cambio.getAdjustmentMode());
        dto.setAdjustmentValue(cambio.getAdjustmentValue());
        dto.setAdjustCost(cambio.getAdjustCost());
        dto.setRoundTo(cambio.getRoundTo());
        dto.setBatchId(cambio.getBatchId());
        dto.setCreatedAt(cambio.getCreatedAt());
        dto.setCreatedBy(cambio.getCreatedBy());
        return dto;
    }

    /** Email del usuario autenticado, o null si no hay sesión (tests o jobs). */
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

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
