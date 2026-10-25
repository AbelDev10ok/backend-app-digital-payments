package com.app.digital.payments.digital_pyments.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.SaleDocument;
import com.app.digital.payments.digital_pyments.utils.DocumentType;

@Repository
public interface ISaleDocumentRepository extends JpaRepository<SaleDocument, Long> {

    Optional<SaleDocument> findBySaleIdAndType(Long saleId, DocumentType type);

    List<SaleDocument> findBySaleIdInAndType(List<Long> saleIds, DocumentType type);

    @Query("SELECT COALESCE(MAX(d.number), 0) FROM SaleDocument d "
            + "WHERE d.business.id = :businessId AND d.type = :type")
    Long findMaxNumber(@Param("businessId") Long businessId, @Param("type") DocumentType type);

    @Modifying
    @Query("DELETE FROM SaleDocument d WHERE d.sale.id = :saleId")
    void deleteBySaleId(@Param("saleId") Long saleId);
}
