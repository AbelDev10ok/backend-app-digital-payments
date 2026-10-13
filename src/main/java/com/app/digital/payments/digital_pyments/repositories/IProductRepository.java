package com.app.digital.payments.digital_pyments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.Product;

public interface IProductRepository extends JpaRepository<Product, Long> {

    // Verificar si un Product existe por su nombre dentro del negocio
    boolean existsByNameAndBusinessId(String name, Long businessId);

    // Verificar si existe otro Product con ese nombre dentro del negocio (excluyendo uno dado)
    boolean existsByNameAndBusinessIdAndIdNot(String name, Long businessId, Long id);

    // Verificar si existe algún Producto asociado a la categoría
    boolean existsByProductTypeId(Long productTypeId);

}