package com.app.digital.payments.digital_pyments.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.dtos.ProductTypeDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.SlugUtils;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

/**
 * Categorías del catálogo: jerárquicas por negocio, con slug estable.
 * Una categoría con subcategorías o con productos/ventas asociadas no se borra.
 */
@Service
public class ProductTypeServices {

    private final IProductTypeRepository productTypeRepository;
    private final IProductRepository productRepository;
    private final ISaleRepository saleRepository;
    private final IBusinessRepository businessRepository;

    public ProductTypeServices(IProductTypeRepository productTypeRepository, IProductRepository productRepository,
            ISaleRepository saleRepository, IBusinessRepository businessRepository) {
        this.productTypeRepository = productTypeRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.businessRepository = businessRepository;
    }

    // Compatibilidad con llamadas viejas (sin padre/slug)
    @Transactional
    public boolean createProductType(String name) {
        return createProductType(name, null, null);
    }

    @Transactional
    public boolean createProductType(String name, Long parentId, String slug) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toUpperCase();
        Long negocioId = TenantContext.requireBusinessId();
        if (productTypeRepository.existsByNameAndBusinessId(normalized, negocioId)) {
            return false; // Ya existe una categoría con este nombre en el negocio
        }

        ProductType parent = null;
        if (parentId != null) {
            parent = productTypeRepository.findById(parentId).orElse(null);
            if (parent == null) {
                return false;
            }
            TenantGuard.verificar(parent.getBusiness() != null ? parent.getBusiness().getId() : null);
        }

        String slugFinal = resolverSlug(slug, normalized, negocioId, null);

        var productType = new ProductType();
        productType.setName(normalized);
        productType.setParent(parent);
        productType.setSlug(slugFinal);
        productType.setBusiness(businessRepository.getReferenceById(negocioId));
        productTypeRepository.save(productType);
        return true;
    }

    @Transactional
    public boolean deleteProductType(Long id) {
        if (id == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(id).orElse(null);
        if (productType == null) {
            return false; // No existe una categoría con este ID
        }
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        // Protección: no se puede borrar una categoría que tenga productos asociados
        if (productRepository.existsByProductTypeId(id)) {
            return false;
        }

        // Protección: tampoco si tiene subcategorías colgando
        if (productTypeRepository.existsByParentId(id)) {
            return false;
        }

        // Protección: no se puede borrar una categoría en uso por transacciones no canceladas
        boolean enUso = saleRepository.findByProductTypeIdAndStatusNot(id, SaleStatus.CANCELED, PageRequest.of(0, 1))
                .hasContent();
        if (enUso) {
            return false;
        }
        productTypeRepository.delete(productType);
        return true;
    }

    // Compatibilidad: renombra sin tocar padre ni slug
    @Transactional
    public boolean updateProductType(Long id, String name) {
        if (id == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(id).orElse(null);
        if (productType == null) {
            return false;
        }
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toUpperCase();
        Long negocioId = TenantContext.requireBusinessId();
        if (productTypeRepository.existsByNameAndBusinessIdAndIdNot(normalized, negocioId, id)) {
            return false;
        }
        productType.setName(normalized);
        productTypeRepository.save(productType);
        return true;
    }

    @Transactional
    public boolean updateProductType(Long id, String name, Long parentId, String slug) {
        if (id == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(id).orElse(null);
        if (productType == null) {
            return false;
        }
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toUpperCase();
        Long negocioId = TenantContext.requireBusinessId();
        if (productTypeRepository.existsByNameAndBusinessIdAndIdNot(normalized, negocioId, id)) {
            return false;
        }

        ProductType parent = null;
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new IllegalArgumentException("Una categoría no puede ser su propia categoría padre");
            }
            parent = productTypeRepository.findById(parentId).orElse(null);
            if (parent == null) {
                return false;
            }
            TenantGuard.verificar(parent.getBusiness() != null ? parent.getBusiness().getId() : null);
            // Sin ciclos: el nuevo padre no puede colgar (directa o indirectamente) de esta categoría
            ProductType cursor = parent;
            while (cursor != null) {
                if (id.equals(cursor.getId())) {
                    throw new IllegalArgumentException("La jerarquía de categorías no puede tener ciclos");
                }
                cursor = cursor.getParent();
            }
        }

        String slugFinal = resolverSlug(slug, normalized, negocioId, id);

        productType.setName(normalized);
        productType.setParent(parent);
        productType.setSlug(slugFinal);
        productTypeRepository.save(productType);
        return true;
    }

    /**
     * Slug explícito: debe ser único o se rechaza. Slug automático: si el nombre
     * genera uno repetido, se le agrega un sufijo numérico.
     */
    private String resolverSlug(String slug, String nombre, Long negocioId, Long id) {
        boolean explicito = slug != null && !slug.isBlank();
        String base = SlugUtils.slugify(explicito ? slug : nombre);
        if (base.isBlank()) {
            base = "categoria";
        }

        if (explicito) {
            boolean tomado = id == null
                    ? productTypeRepository.existsBySlugAndBusinessId(base, negocioId)
                    : productTypeRepository.existsBySlugAndBusinessIdAndIdNot(base, negocioId, id);
            if (tomado) {
                throw new IllegalArgumentException("Ya existe una categoría con el slug " + base);
            }
            return base;
        }

        String candidato = base;
        int sufijo = 2;
        while (id == null
                ? productTypeRepository.existsBySlugAndBusinessId(candidato, negocioId)
                : productTypeRepository.existsBySlugAndBusinessIdAndIdNot(candidato, negocioId, id)) {
            candidato = base + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }

    @Transactional(readOnly = true)
    public List<ProductTypeDto> getAllProductTypes() {
        return productTypeRepository.findAll().stream()
                .map(pt -> new ProductTypeDto(
                        pt.getId(),
                        pt.getName(),
                        pt.getIcon(),
                        pt.getParent() != null ? pt.getParent().getId() : null,
                        pt.getParent() != null ? pt.getParent().getName() : null,
                        pt.getSlug()))
                .toList();
    }
}
