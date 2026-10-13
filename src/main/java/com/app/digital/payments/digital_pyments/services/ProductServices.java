package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Product;
import com.app.digital.payments.digital_pyments.models.ProductType;
import com.app.digital.payments.digital_pyments.models.dtos.ProductDto;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductRepository;
import com.app.digital.payments.digital_pyments.repositories.IProductTypeRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;
import com.app.digital.payments.digital_pyments.utils.TenantGuard;

@Service
public class ProductServices {

    private final IProductRepository productRepository;
    private final IProductTypeRepository productTypeRepository;
    private final ISaleRepository saleRepository;
    private final IBusinessRepository businessRepository;

    public ProductServices(IProductRepository productRepository, IProductTypeRepository productTypeRepository,
            ISaleRepository saleRepository, IBusinessRepository businessRepository) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.saleRepository = saleRepository;
        this.businessRepository = businessRepository;
    }

    // crear un nuevo Product (del catálogo) para el negocio autenticado
    @Transactional
    public boolean createProduct(String name, BigDecimal price, Integer stock, Long productTypeId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        // Se conserva el formato original (trim) para que la descripcion autocompletada
        // de una venta se muestre natural; la unicidad es exacta dentro del negocio.
        String normalized = name.trim();
        Long negocioId = TenantContext.requireBusinessId();
        if (productRepository.existsByNameAndBusinessId(normalized, negocioId)) {
            return false; // Ya existe un producto con este nombre en el negocio
        }

        // El precio (si se indica) debe ser mayor a 0
        if (price != null && price.signum() <= 0) {
            return false;
        }

        // El stock (si se indica) no puede ser negativo; null = sin control de inventario
        if (stock != null && stock < 0) {
            return false;
        }

        // La categoría es obligatoria: un producto sin categoría no puede venderse
        if (productTypeId == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(productTypeId).orElse(null);
        if (productType == null) {
            return false; // La categoría indicada no existe
        }
        // La categoria tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        var product = new Product();
        product.setName(normalized);
        product.setPrice(price);
        product.setStock(stock);
        product.setProductType(productType);
        product.setBusiness(businessRepository.getReferenceById(negocioId));
        productRepository.save(product);
        return true;
    }

    // eliminar un Product (del catálogo) por su ID
    @Transactional
    public boolean deleteProduct(Long id) {
        if (id == null) {
            return false;
        }
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return false; // No existe un producto con este ID
        }
        // El producto tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);

        // Protección: no se puede borrar un producto en uso por transacciones no canceladas
        boolean enUso = saleRepository.findByProductIdAndStatusNot(id, SaleStatus.CANCELED, PageRequest.of(0, 1))
                .hasContent();
        if (enUso) {
            return false;
        }
        productRepository.delete(product);
        return true;
    }

    // modificar un Product (del catálogo) por su ID: nombre, precio, stock y categoría
    @Transactional
    public boolean updateProduct(Long id, String name, BigDecimal price, Integer stock, Long productTypeId) {
        if (id == null) {
            return false;
        }
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return false; // No existe un producto con este ID
        }
        // El producto tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(product.getBusiness() != null ? product.getBusiness().getId() : null);

        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim();
        Long negocioId = TenantContext.requireBusinessId();
        if (productRepository.existsByNameAndBusinessIdAndIdNot(normalized, negocioId, id)) {
            return false; // Otro producto ya usa este nombre en el negocio
        }

        // El precio (si se indica) debe ser mayor a 0
        if (price != null && price.signum() <= 0) {
            return false;
        }

        // El stock (si se indica) no puede ser negativo; null = sin control de inventario
        if (stock != null && stock < 0) {
            return false;
        }

        // La categoría es obligatoria: un producto sin categoría no puede venderse
        if (productTypeId == null) {
            return false;
        }
        ProductType productType = productTypeRepository.findById(productTypeId).orElse(null);
        if (productType == null) {
            return false; // La categoría indicada no existe
        }
        // La categoria tiene que pertenecer al negocio autenticado
        TenantGuard.verificar(productType.getBusiness() != null ? productType.getBusiness().getId() : null);

        product.setName(normalized);
        product.setPrice(price);
        product.setStock(stock);
        product.setProductType(productType);
        productRepository.save(product);
        return true;
    }

    // obtener todos los Products del negocio autenticado (filtro de tenancy)
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> new ProductDto(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStock(),
                        product.getProductType() != null ? product.getProductType().getId() : null,
                        product.getProductType() != null ? product.getProductType().getName() : null))
                .toList();
    }
}