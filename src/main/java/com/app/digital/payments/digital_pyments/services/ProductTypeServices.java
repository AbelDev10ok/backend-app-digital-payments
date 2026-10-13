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
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

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

    // crear un nuevo ProductType (categoría de venta) para el negocio autenticado
    @Transactional
    public boolean createProductType(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toUpperCase();
        Long negocioId = TenantContext.requireBusinessId();
        if (productTypeRepository.existsByNameAndBusinessId(normalized, negocioId)) {
            return false; // Ya existe una categoría con este nombre en el negocio
        }
        var productType = new ProductType();
        productType.setName(normalized);
        productType.setBusiness(businessRepository.getReferenceById(negocioId));
        productTypeRepository.save(productType);
        return true;
    }

    // eliminar un ProductType (categoría) por su ID
    @Transactional
    public boolean deleteProductType(Long id) {
        if (id == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(id).orElse(null);
        if (productType == null) {
            return false; // No existe una categoría con este ID
        }
        // La categoria tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        // Protección: no se puede borrar una categoría que tenga productos asociados
        if (productRepository.existsByProductTypeId(id)) {
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

    // modificar un ProductType (categoría) por su ID: renombrar
    @Transactional
    public boolean updateProductType(Long id, String name) {
        if (id == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(id).orElse(null);
        if (productType == null) {
            return false; // No existe una categoría con este ID
        }
        // La categoria tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toUpperCase();
        Long negocioId = TenantContext.requireBusinessId();
        if (productTypeRepository.existsByNameAndBusinessIdAndIdNot(normalized, negocioId, id)) {
            return false; // Otra categoría ya usa este nombre en el negocio
        }

        productType.setName(normalized);
        productTypeRepository.save(productType);
        return true;
    }

    // obtener todos los ProductTypes del negocio autenticado (filtro de tenancy)
    @Transactional(readOnly = true)
    public List<ProductTypeDto> getAllProductTypes() {
        return productTypeRepository.findAll().stream()
                .map(pt -> new ProductTypeDto(pt.getId(), pt.getName(), pt.getIcon()))
                .toList();
    }
}
