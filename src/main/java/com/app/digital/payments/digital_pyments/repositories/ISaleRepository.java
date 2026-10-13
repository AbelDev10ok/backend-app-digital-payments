package com.app.digital.payments.digital_pyments.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Sale;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;

@Repository
public interface ISaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    @Override
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findAll(org.springframework.data.jpa.domain.Specification<Sale> spec, Pageable pageable);

    // Contar ventas por rango de fechas
    long countByDateCreationBetween(LocalDate startDate, LocalDate endDate);

    // Suma de priceTotal para ventas completadas en rango de fechas
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.status = :status AND s.dateCreation BETWEEN :startDate AND :endDate")
    BigDecimal sumPriceTotalByStatusAndDateCreationBetween(@Param("status") SaleStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Suma de remainingAmount para ventas no completadas
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.status != :status AND s.dateCreation BETWEEN :startDate AND :endDate")
    BigDecimal sumRemainingAmountByStatusNotAndDateCreationBetween(@Param("status") SaleStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Suma de ganancia (priceTotal - cost) para ventas completadas
    @Query("SELECT SUM(s.priceTotal - s.cost) FROM Sale s WHERE s.status = :status AND s.dateCreation BETWEEN :startDate AND :endDate")
    BigDecimal sumProfitByStatusAndDateCreationBetween(@Param("status") SaleStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // obtener venta o prestamo por id
    Optional<Sale> findById(Long id);

    // obtener ventas por descripcion de producto (paginado)
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findByDescriptionProductContainingIgnoreCase(String description, Pageable pageable);

    // Obtengo todas las ventas
    List<Sale> findAll();

    // Obtengo todas las transacciones por tipo (kind) y que NO están canceladas (paginado)
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findByKindAndStatusNot(SaleKind kind, SaleStatus status, Pageable pageable);

    // Obtengo todas las transacciones por categoría de producto (productTypeId) y que NO están canceladas (paginado)
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findByProductTypeIdAndStatusNot(Long productTypeId, SaleStatus status, Pageable pageable);

    // Obtengo todas las transacciones asociadas a un producto del catálogo y que NO están canceladas (paginado)
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findByProductIdAndStatusNot(Long productId, SaleStatus status, Pageable pageable);

    // Obtengo todas las ventas por cliente y que NO están canceladas (paginado)
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findByClientIdAndStatusNotOrderByDateCreationDesc(Long clientId, SaleStatus status, Pageable pageable);

    // Obtengo todas las ventas no pagadas por cliente y que NO están canceladas (paginado)
    @Query(value = "SELECT s FROM Sale s WHERE s.client.id = :clientId AND s.status <> :completed AND s.status <> :canceled ORDER BY s.dateCreation DESC",
           countQuery = "SELECT COUNT(s) FROM Sale s WHERE s.client.id = :clientId AND s.status <> :completed AND s.status <> :canceled")
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findNotCompletedByClientIdOrderByDateCreationDesc(@Param("clientId") Long clientId,
            @Param("completed") SaleStatus completed, @Param("canceled") SaleStatus canceled, Pageable pageable);

    // Obtengo todas las ventas pagadas por cliente y que NO están canceladas (paginado)
    @Query(value = "SELECT s FROM Sale s WHERE s.client.id = :clientId AND s.status = :completed ORDER BY s.dateCreation DESC",
           countQuery = "SELECT COUNT(s) FROM Sale s WHERE s.client.id = :clientId AND s.status = :completed")
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findCompletedByClientIdOrderByDateCreationDesc(@Param("clientId") Long clientId,
            @Param("completed") SaleStatus completed, Pageable pageable);

    // OBTENGO LA CANTIDAD DE VENTAS COMPLETADAS POR CLIENTE
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.client.id = :clientId AND s.status = :completed")
    Long countCompletedByClientId(@Param("clientId") Long clientId, @Param("completed") SaleStatus completed);

    // OBTENGO LA CANTIDAD DE VENTAS NO COMPLETADAS POR CLIENTE (y NO canceladas)
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.client.id = :clientId AND s.status <> :completed AND s.status <> :canceled")
    Long countNotCompletedByClientId(@Param("clientId") Long clientId,
            @Param("completed") SaleStatus completed, @Param("canceled") SaleStatus canceled);

    // VENTAS QUE HAN SUPERADO SU FECHA FINAL DE PAGO (y NO están canceladas)
    @EntityGraph(attributePaths = {"fees", "client"})
    List<Sale> findByFinalPaymentDateBeforeAndStatusNot(LocalDate date, SaleStatus status);

    // VENTAS CON CUOTAS A COBRAR HOY y NO CANCELADAS
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate = :date AND f.paid = false AND s.status != 'CANCELED'")
    @EntityGraph(attributePaths = {"fees", "client"})
    List<Sale> findSalesWithFeesToChargeToday(@Param("date") LocalDate date);

    // OBTENER EL MONTO TOTAL DE VENTAS DE UN AÑO ESPECIFICO (y NO canceladas)
    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalSalesAmountByYear(@Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND s.kind = :kind AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalSalesAmountByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // OBTENER EL MONTO TOTAL DE VENTAS PAGADAS DE UN AÑO ESPECIFICO (y NO canceladas)
    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status = 'COMPLETED' AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalPaidSalesAmountByYear(@Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status = 'COMPLETED' AND s.kind = :kind AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalPaidSalesAmountByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // OBTENER EL MONTO TOTAL DE VENTAS DE UN MES ESPECIFICO (y NO canceladas)
    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalSalesAmountByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND s.kind = :kind AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalSalesAmountByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("kind") SaleKind kind);

    // OBTENER EL MONTO TOTAL DE VENTAS PAGADAS DE UN MES ESPECIFICO (y NO canceladas)
    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status = 'COMPLETED' AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalPaidSalesAmountByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status = 'COMPLETED' AND s.kind = :kind AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalPaidSalesAmountByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("kind") SaleKind kind);

    // OBTENER EL MONTO TOTAL DE VENTAS DE UN DIA ESPECIFICO (y NO canceladas)
    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND DATE(s.dateCreation) = :date")
    Optional<BigDecimal> findTotalSalesAmountByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(s.priceTotal - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND s.kind = :kind AND DATE(s.dateCreation) = :date")
    Optional<BigDecimal> findTotalSalesAmountByDate(@Param("date") LocalDate date, @Param("kind") SaleKind kind);

    // Monto total cobrado (de cuotas pagadas) de productos por año, excluyendo tipo y CANCELADAS
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
            "WHERE (s.status != 'CANCELED' OR (s.status = 'CANCELED' AND s.refundAmount = 0)) " +
            "AND f.paid = TRUE " +
            "AND YEAR(f.datePayment) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByYear(@Param("year") int year);

    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
            "WHERE (s.status != 'CANCELED' OR (s.status = 'CANCELED' AND s.refundAmount = 0)) AND s.kind = :kind " +
            "AND f.paid = TRUE " +
            "AND YEAR(f.datePayment) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // Monto total cobrado (de cuotas pagadas) de VENTAS por mes y año, excluyendo tipo y CANCELADAS
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
            "WHERE (s.status != 'CANCELED' OR (s.status = 'CANCELED' AND s.refundAmount = 0)) " +
            "AND f.paid = TRUE " +
            "AND MONTH(f.datePayment) = :month " +
            "AND YEAR(f.datePayment) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
            "WHERE (s.status != 'CANCELED' OR (s.status = 'CANCELED' AND s.refundAmount = 0)) AND s.kind = :kind " +
            "AND f.paid = TRUE " +
            "AND MONTH(f.datePayment) = :month " +
            "AND YEAR(f.datePayment) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("kind") SaleKind kind);

    // Ganancia total de productos por mes y año (excluyendo tipo y CANCELADAS)
    @Query("SELECT SUM(s.priceTotal - s.cost - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalProfitByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.cost - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND s.kind = :kind AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalProfitByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("kind") SaleKind kind);

    // Ganancia total de productos por año (excluyendo tipo y CANCELADAS)
    @Query("SELECT SUM(s.priceTotal - s.cost - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalProfitByYear(@Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.cost - s.refundAmount) FROM Sale s WHERE s.status != 'CANCELED' AND s.kind = :kind AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalProfitByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // OBTENER EL MONTO TOTAL DE REEMBOLSOS DE UN AÑO ESPECÍFICO (de transacciones de otro tipo)
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND s.kind != :kind AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalRefundsByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // Y para el mes:
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND s.kind != :kind AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalRefundsByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("kind") SaleKind kind);

    // VENTAS CON CUOTAS VENCIDAS O POR VENCER HOY
    @Query(value = "SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate <= :date AND f.paid = false AND s.status != 'CANCELED'",
           countQuery = "SELECT COUNT(DISTINCT s) FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate <= :date AND f.paid = false AND s.status != 'CANCELED'")
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findSalesWithPendingFeesDueBeforeOrOn(@Param("date") LocalDate date, Pageable pageable);

    // ESTADÍSTICAS CONSOLIDADAS: total vendido y ganancia esperada en un mes/año
    @Query("SELECT SUM(s.priceTotal - s.refundAmount), SUM(s.priceTotal - s.cost - s.refundAmount) " +
            "FROM Sale s WHERE s.status != 'CANCELED' AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<Object[]> findSalesAndProfitByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(s.priceTotal - s.refundAmount), SUM(s.priceTotal - s.cost - s.refundAmount) " +
            "FROM Sale s WHERE s.status != 'CANCELED' AND s.kind = :kind " +
            "AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<Object[]> findSalesAndProfitByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("kind") SaleKind kind);

    // ----- Consultas para el dashboard -----

    // Contar ventas por estado en un mes/año específico
    @Query("SELECT s.status, COUNT(s) FROM Sale s WHERE YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month GROUP BY s.status")
    List<Object[]> countSalesByStatusInMonth(@Param("year") int year, @Param("month") int month);

    // Contar ventas por estado en un año específico
    @Query("SELECT s.status, COUNT(s) FROM Sale s WHERE YEAR(s.dateCreation) = :year GROUP BY s.status")
    List<Object[]> countSalesByStatusInYear(@Param("year") int year);

    // Serie mensual de ventas (vendido y ganancia) desde una fecha
    @Query("SELECT YEAR(s.dateCreation), MONTH(s.dateCreation), SUM(s.priceTotal - s.refundAmount), SUM(s.priceTotal - s.cost - s.refundAmount) " +
            "FROM Sale s WHERE s.status != 'CANCELED' AND s.dateCreation >= :since " +
            "GROUP BY YEAR(s.dateCreation), MONTH(s.dateCreation) ORDER BY YEAR(s.dateCreation), MONTH(s.dateCreation)")
    List<Object[]> findMonthlySalesSeries(@Param("since") LocalDate since);

    // Serie mensual de cobrado (cuotas pagadas) desde una fecha
    @Query("SELECT YEAR(f.datePayment), MONTH(f.datePayment), SUM(f.amount) FROM Sale s JOIN s.fees f " +
            "WHERE (s.status != 'CANCELED' OR (s.status = 'CANCELED' AND s.refundAmount = 0)) AND f.paid = TRUE AND f.datePayment >= :since " +
            "GROUP BY YEAR(f.datePayment), MONTH(f.datePayment) ORDER BY YEAR(f.datePayment), MONTH(f.datePayment)")
    List<Object[]> findMonthlyCollectedSeries(@Param("since") LocalDate since);

    // Top clientes por monto vendido en un año (ordenados de mayor a menor)
    @Query("SELECT s.client.id, s.client.name, COUNT(s), SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year " +
            "GROUP BY s.client.id, s.client.name ORDER BY SUM(s.priceTotal - s.refundAmount) DESC")
    List<Object[]> findTopClientsByYear(@Param("year") int year);

    // Monto cobrado por cliente en un año (cuotas pagadas)
    @Query("SELECT s.client.id, SUM(f.amount) FROM Sale s JOIN s.fees f " +
            "WHERE (s.status != 'CANCELED' OR (s.status = 'CANCELED' AND s.refundAmount = 0)) AND f.paid = TRUE AND YEAR(f.datePayment) = :year " +
            "GROUP BY s.client.id")
    List<Object[]> findCollectedAmountByClientInYear(@Param("year") int year);

    // Cuotas vencidas no pagadas (cantidad y monto)
    // Si la cuota no tiene monto fijado (0), se muestra el valor acordado por cuota de la venta
    @Query("SELECT COUNT(f), SUM(CASE WHEN f.amount > 0 THEN f.amount ELSE s.amountFee END) FROM Sale s JOIN s.fees f " +
            "WHERE s.status != 'CANCELED' AND f.paid = FALSE AND f.expirationDate <= :date")
    List<Object[]> findOverdueFees(@Param("date") LocalDate date);

    // Transacciones agrupadas por tipo (kind) en un mes/año
    @Query("SELECT s.kind, COUNT(s), SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month " +
            "GROUP BY s.kind")
    List<Object[]> findSalesByKindInMonth(@Param("year") int year, @Param("month") int month);

    // Monto pendiente por cobrar (ventas activas)
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.status = :status")
    Optional<BigDecimal> sumRemainingAmountByStatus(@Param("status") SaleStatus status);

    // Monto pendiente por cobrar (ventas activas) de un mes/año
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.status = :status AND YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month")
    Optional<BigDecimal> sumRemainingAmountByStatusAndMonthYear(@Param("status") SaleStatus status, @Param("year") int year, @Param("month") int month);

    // Reembolsos totales de un mes/año (sin filtro de tipo de transacción)
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month")
    Optional<BigDecimal> findTotalRefundsByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Reembolsos totales de un año (sin filtro de tipo de transacción)
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND YEAR(s.dateCreation) = :year")
    Optional<BigDecimal> findTotalRefundsByYear(@Param("year") int year);

}
