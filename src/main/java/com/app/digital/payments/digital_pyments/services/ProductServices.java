package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.mappers.ProductMapper;
import com.app.digital.payments.digital_pyments.models.Attribute;
import com.app.digital.payments.digital_pyments.models.AttributeValue;
import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductAttribute;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.ProductVariant;
import com.app.digital.payments.digital_pyments.models.VariantAttribute;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductRequestDto;
import com.app.digital.payments.digital_pyments.models.dtos.ProductVariantRequestDto;
import com.app.digital.payments.digital_pyments.repositories.IAttributeRepository;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
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

    public ProductServices(IProductRepository productRepository, IProductVariantRepository variantRepository,
            IAttributeRepository attributeRepository, IProductTypeRepository productTypeRepository,
            ISaleRepository saleRepository, IBusinessRepository businessRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.attributeRepository = attributeRepository;
        this.productTypeRepository = productTypeRepository;
        this.saleRepository = saleRepository;
        this.businessRepository = businessRepository;
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
        if (dto.getStock() == null || dto.getStock() < 0) {
            throw new IllegalArgumentException("El stock es obligatorio y no puede ser negativo");
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
        variante.setStock(dto.getStock());
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

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
