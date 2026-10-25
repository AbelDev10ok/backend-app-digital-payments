package com.app.digital.payments.digital_pyments.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.digital.payments.digital_pyments.models.ProductType;

public interface IProductTypeRepository extends JpaRepository<ProductType, Long> {

    // Método para encontrar un ProductType por su nombre
    Optional<ProductType> findByName(String name);

    // Método para verificar si un ProductType existe por su nombre
    boolean existsByName(String name);

    // Variantes con scope de negocio (el nombre es único dentro del tenant)
    Optional<ProductType> findByNameAndBusinessId(String name, Long businessId);

    boolean existsByNameAndBusinessId(String name, Long businessId);

    // Verificar si existe otra categoría con ese nombre dentro del negocio (excluyendo una dada)
    boolean existsByNameAndBusinessIdAndIdNot(String name, Long businessId, Long id);

    // Slug estable dentro del negocio
    boolean existsBySlugAndBusinessId(String slug, Long businessId);

    boolean existsBySlugAndBusinessIdAndIdNot(String slug, Long businessId, Long id);

    // Protección al borrar: una categoría con subcategorías no se elimina
    boolean existsByParentId(Long parentId);

}
