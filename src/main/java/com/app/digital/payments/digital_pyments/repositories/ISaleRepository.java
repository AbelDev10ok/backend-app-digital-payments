package com.app.digital.payments.digital_pyments.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Obtengo todas las transacciones asociadas a una variante/SKU y que NO están canceladas (paginado)
    @EntityGraph(attributePaths = {"fees", "client"})
    Page<Sale> findByVariantIdAndStatusNot(Long variantId, SaleStatus status, Pageable pageable);

    // ¿La variante tiene ventas? Se usa para no borrarla del catálogo
    boolean existsByVariantId(Long variantId);

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

    // Monto total cobrado (cobros registrados) por año: el log de pagos por fecha
    // de cobro (una cuota puede tener varios pagos). El dinero cuenta cuando entra,
    // aunque la venta se anule después; los reembolsos van por su propia fecha.
    @Query("SELECT SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE YEAR(p.paidAt) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByYear(@Param("year") int year);

    @Query("SELECT SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE s.kind = :kind " +
            "AND YEAR(p.paidAt) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // Monto total cobrado (cobros registrados) por mes y año
    @Query("SELECT SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE MONTH(p.paidAt) = :month " +
            "AND YEAR(p.paidAt) = :year")
    Optional<BigDecimal> findTotalCollectedAmountByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE s.kind = :kind " +
            "AND MONTH(p.paidAt) = :month " +
            "AND YEAR(p.paidAt) = :year")
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
    // Los reembolsos se reportan por la fecha en que se anularon (refundAt): la plata
    // salió ese mes, no el de la venta. COALESCE cubre los registros anteriores a la
    // columna, que se siguen reportando por la fecha de creación como antes.
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND s.kind != :kind AND YEAR(COALESCE(s.refundAt, s.dateCreation)) = :year")
    Optional<BigDecimal> findTotalRefundsByYear(@Param("year") int year, @Param("kind") SaleKind kind);

    // Y para el mes:
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND s.kind != :kind AND MONTH(COALESCE(s.refundAt, s.dateCreation)) = :month AND YEAR(COALESCE(s.refundAt, s.dateCreation)) = :year")
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

    // Serie mensual de cobrado (log de pagos) desde una fecha
    @Query("SELECT YEAR(p.paidAt), MONTH(p.paidAt), SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE p.paidAt >= :since " +
            "GROUP BY YEAR(p.paidAt), MONTH(p.paidAt) ORDER BY YEAR(p.paidAt), MONTH(p.paidAt)")
    List<Object[]> findMonthlyCollectedSeries(@Param("since") LocalDateTime since);

    // Top clientes por monto vendido en un año (ordenados de mayor a menor)
    @Query("SELECT s.client.id, s.client.name, COUNT(s), SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year " +
            "GROUP BY s.client.id, s.client.name ORDER BY SUM(s.priceTotal - s.refundAmount) DESC")
    List<Object[]> findTopClientsByYear(@Param("year") int year);

    // Monto cobrado por cliente en un año (log de pagos)
    @Query("SELECT s.client.id, SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE YEAR(p.paidAt) = :year " +
            "GROUP BY s.client.id")
    List<Object[]> findCollectedAmountByClientInYear(@Param("year") int year);

    // Cuotas vencidas no pagadas (cantidad y monto)
    // Si la cuota no tiene monto fijado (0), se muestra el valor acordado por cuota de la venta
    @Query("SELECT COUNT(f), SUM((CASE WHEN f.amount > 0 THEN f.amount ELSE s.amountFee END) - COALESCE(f.paidAmount, 0)) FROM Sale s JOIN s.fees f " +
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
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND YEAR(COALESCE(s.refundAt, s.dateCreation)) = :year AND MONTH(COALESCE(s.refundAt, s.dateCreation)) = :month")
    Optional<BigDecimal> findTotalRefundsByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Reembolsos totales de un año (sin filtro de tipo de transacción)
    @Query("SELECT SUM(s.refundAmount) FROM Sale s WHERE s.status = 'CANCELED' AND YEAR(COALESCE(s.refundAt, s.dateCreation)) = :year")
    Optional<BigDecimal> findTotalRefundsByYear(@Param("year") int year);

    // ----- Métricas por vendedor -----

    // Operaciones creadas en el período agrupadas por vendedor sellado (seller
    // nulo = sin atribuir), tipo y moneda: total, anuladas y monto vendido.
    // El vendido incluye las luego anuladas (igual que el tablero); la columna
    // de anuladas permite leerlas.
    @Query("SELECT s.seller.id, s.kind, s.currency, COUNT(s), " +
            "SUM(CASE WHEN s.status = :canceled THEN 1 ELSE 0 END), SUM(s.priceTotal) " +
            "FROM Sale s WHERE MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year " +
            "GROUP BY s.seller.id, s.kind, s.currency")
    List<Object[]> findSellerMetricsByPeriod(@Param("month") int month, @Param("year") int year,
            @Param("canceled") SaleStatus canceled);

    // Cobrado en el período por vendedor sellado: log de pagos por fecha de
    // cobro (cuenta cuando entró la plata, aunque la operación se anule luego).
    @Query("SELECT s.seller.id, s.kind, s.currency, SUM(p.amount) " +
            "FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE MONTH(p.paidAt) = :month AND YEAR(p.paidAt) = :year " +
            "GROUP BY s.seller.id, s.kind, s.currency")
    List<Object[]> findSellerCollectedByPeriod(@Param("month") int month, @Param("year") int year);

    // Deuda de la cartera actual: saldo de las operaciones activas de los
    // clientes asignados hoy a cada vendedor (seller nulo = sin vendedor).
    @Query("SELECT c.seller.id, s.kind, s.currency, SUM(s.remainingAmount) " +
            "FROM Sale s JOIN s.client c " +
            "WHERE s.status = :status " +
            "GROUP BY c.seller.id, s.kind, s.currency")
    List<Object[]> findSellerPortfolioDebtByKindAndCurrency(@Param("status") SaleStatus status);

    // ----- Consultas agrupadas por moneda -----
    // Regla de dominio: los montos de monedas distintas NUNCA se suman entre sí.
    // Cada consulta devuelve filas (…, moneda, suma) para que el servicio arme
    // los totales por moneda.

    @Query("SELECT s.currency, SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year " +
            "GROUP BY s.currency")
    List<Object[]> findTotalSalesAmountByMonthAndYearGrouped(@Param("month") int month, @Param("year") int year);

    @Query("SELECT s.currency, SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE MONTH(p.paidAt) = :month AND YEAR(p.paidAt) = :year " +
            "GROUP BY s.currency")
    List<Object[]> findTotalCollectedAmountByMonthAndYearGrouped(@Param("month") int month, @Param("year") int year);

    @Query("SELECT s.currency, SUM(s.priceTotal - s.cost - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year " +
            "GROUP BY s.currency")
    List<Object[]> findTotalProfitByMonthAndYearGrouped(@Param("month") int month, @Param("year") int year);

    // Los reembolsos van por la fecha de anulación (refundAt): COALESCE cubre los
    // registros anteriores a esa columna, que siguen reportándose por creación.
    @Query("SELECT s.currency, SUM(s.refundAmount) FROM Sale s " +
            "WHERE s.status = 'CANCELED' " +
            "AND YEAR(COALESCE(s.refundAt, s.dateCreation)) = :year " +
            "AND MONTH(COALESCE(s.refundAt, s.dateCreation)) = :month " +
            "GROUP BY s.currency")
    List<Object[]> findTotalRefundsByMonthAndYearGrouped(@Param("month") int month, @Param("year") int year);

    @Query("SELECT s.currency, SUM(s.remainingAmount) FROM Sale s " +
            "WHERE s.status = :status AND YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month " +
            "GROUP BY s.currency")
    List<Object[]> sumRemainingAmountByStatusAndMonthYearGrouped(@Param("status") SaleStatus status,
            @Param("year") int year, @Param("month") int month);

    // Deuda total de las ventas activas, a hoy: sin filtro de mes, es lo que falta
    // cobrar de todo el negocio y no sólo de lo vendido en el mes consultado
    @Query("SELECT s.currency, SUM(s.remainingAmount) FROM Sale s " +
            "WHERE s.status = :status " +
            "GROUP BY s.currency")
    List<Object[]> sumRemainingAmountByStatusGrouped(@Param("status") SaleStatus status);

    @Query("SELECT s.currency, SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year GROUP BY s.currency")
    List<Object[]> findTotalSalesAmountByYearGrouped(@Param("year") int year);

    @Query("SELECT s.currency, SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE YEAR(p.paidAt) = :year GROUP BY s.currency")
    List<Object[]> findTotalCollectedAmountByYearGrouped(@Param("year") int year);

    @Query("SELECT s.currency, SUM(s.priceTotal - s.cost - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year GROUP BY s.currency")
    List<Object[]> findTotalProfitByYearGrouped(@Param("year") int year);

    @Query("SELECT YEAR(s.dateCreation), MONTH(s.dateCreation), s.currency, " +
            "SUM(s.priceTotal - s.refundAmount), SUM(s.priceTotal - s.cost - s.refundAmount) " +
            "FROM Sale s WHERE s.status != 'CANCELED' AND s.dateCreation >= :since " +
            "GROUP BY YEAR(s.dateCreation), MONTH(s.dateCreation), s.currency " +
            "ORDER BY YEAR(s.dateCreation), MONTH(s.dateCreation)")
    List<Object[]> findMonthlySalesSeriesGrouped(@Param("since") LocalDate since);

    @Query("SELECT YEAR(p.paidAt), MONTH(p.paidAt), s.currency, SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE p.paidAt >= :since " +
            "GROUP BY YEAR(p.paidAt), MONTH(p.paidAt), s.currency " +
            "ORDER BY YEAR(p.paidAt), MONTH(p.paidAt)")
    List<Object[]> findMonthlyCollectedSeriesGrouped(@Param("since") LocalDateTime since);

    // Top clientes: se agrupa por cliente y moneda; el servicio ordena por
    // cantidad de transacciones (un ranking por monto cruzaría monedas).
    @Query("SELECT s.client.id, s.client.name, s.currency, COUNT(s), SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year " +
            "GROUP BY s.client.id, s.client.name, s.currency")
    List<Object[]> findTopClientsByYearGrouped(@Param("year") int year);

    @Query("SELECT s.client.id, s.currency, SUM(p.amount) FROM Payment p JOIN p.fee f JOIN f.sale s " +
            "WHERE YEAR(p.paidAt) = :year GROUP BY s.client.id, s.currency")
    List<Object[]> findCollectedAmountByClientInYearGrouped(@Param("year") int year);

    @Query("SELECT s.currency, COUNT(f), SUM(" + QueryFragments.MONTO_EFECTIVO_CUOTA + ") " +
            "FROM Sale s JOIN s.fees f " +
            "WHERE s.status != 'CANCELED' AND f.paid = FALSE AND f.expirationDate <= :date GROUP BY s.currency")
    List<Object[]> findOverdueFeesGrouped(@Param("date") LocalDate date);

    @Query("SELECT s.kind, s.currency, COUNT(s), SUM(s.priceTotal - s.refundAmount) FROM Sale s " +
            "WHERE s.status != 'CANCELED' AND YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month " +
            "GROUP BY s.kind, s.currency")
    List<Object[]> findSalesByKindInMonthGrouped(@Param("year") int year, @Param("month") int month);

    // Conteos agrupados por moneda: el filtro del dashboard debe acotar también
    // las cantidades, no solo los montos
    @Query("SELECT s.currency, s.status, COUNT(s) FROM Sale s " +
            "WHERE YEAR(s.dateCreation) = :year AND MONTH(s.dateCreation) = :month " +
            "GROUP BY s.currency, s.status")
    List<Object[]> countSalesByStatusInMonthGrouped(@Param("year") int year, @Param("month") int month);

    @Query("SELECT s.currency, s.status, COUNT(s) FROM Sale s " +
            "WHERE YEAR(s.dateCreation) = :year GROUP BY s.currency, s.status")
    List<Object[]> countSalesByStatusInYearGrouped(@Param("year") int year);

    // Monedas (ISO 4217) presentes en los datos: alimentan el selector del dashboard
    @Query("SELECT DISTINCT s.currency FROM Sale s WHERE s.currency IS NOT NULL ORDER BY s.currency")
    List<String> findDistinctCurrencies();

}
