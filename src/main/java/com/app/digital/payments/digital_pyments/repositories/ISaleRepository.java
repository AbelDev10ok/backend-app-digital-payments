package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Sale;

    // Optional<Sale> findByClientId(Long clientId);
    
    // // Ventas que han superado su fecha final de pago
    // List<Sale> findByFinalPaymentDateBefore(LocalDate date);

    // List<Sale> findByCompletedFalse();

    // List<Sale> findByDaysLateGreaterThan(int daysLate);

    // // puede usarse tanto para hoy como para una fecha específica
    // @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
    //    "WHERE f.expirationDate = :date AND f.paid = false")
    // List<Sale> findSalesWithFeesToChargeToday(@Param("date") LocalDate date);

    // // Ventas con cuotas específicas atrasadas
    // @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
    //        "WHERE f.expirationDate < :date AND f.paid = false")
    // List<Sale> findSalesWithDelayedFees(@Param("date") LocalDate date);


@Repository
public interface ISaleRepository extends JpaRepository<Sale, Long>{

    // OBTENGO TODAS LAS VENTAS (que NO son del tipo indicado)
    List<Sale> findByProductTypeNot(String productType);

    // OBTENGO TODAS LAS VENTAS POR CLIENTE
    List<Sale> findByClientIdAndProductTypeNotOrderByDateCreationDesc(Long clientId, String productType);

    // OBTENGO TODAS LAS VENTAS NO PAGADAS POR CLIENTE
    List<Sale> findByClientIdAndProductTypeNotAndCompletedFalseOrderByDateCreationDesc(Long clientId, String productType);

    // OBTENGO TODAS LAS VENTAS PAGADAS POR CLIENTE
    List<Sale> findByClientIdAndProductTypeNotAndCompletedTrueOrderByDateCreationDesc(Long clientId, String productType);

    // OBTENGO LA CANTIDAD DE VENTAS COMPLETADAS POR CLIENTE
    Long countByClientIdAndCompletedTrue(Long clientId);

    // VENTAS QUE HAN SUPERADO SU FECHA FINAL DE PAGO
    List<Sale> findByFinalPaymentDateBeforeAndProductTypeNot(LocalDate date, String productType);

    // VENTAS QUE NO ESTAN COMPLETADAS
    List<Sale> findByCompletedFalseAndProductTypeNot(String productType);

    // VENTAS QUE TIENEN MAS DE X DIAS DE ATRASO
    List<Sale> findByDaysLateGreaterThanAndProductTypeNot(int daysLate, String productType);

    // VENTAS CON CUOTAS A COBRAR HOY, EXCLUYENDO cierto tipo
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate = :date AND f.paid = false AND s.productType != :productType")
    List<Sale> findSalesWithFeesToChargeTodayAndProductTypeNot(@Param("date") LocalDate date, @Param("productType") String productType);

    // VENTAS CON CUOTAS ATRASADAS, EXCLUYENDO cierto tipo
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate < :date AND f.paid = false AND s.productType != :productType")
    List<Sale> findSalesWithDelayedFeesAndProductTypeNot(@Param("date") LocalDate date, @Param("productType") String productType);

    // ELIMINAR TODAS LAS VENTAS COMPLETADAS DE UN AÑO ESPECIFICO
    @Query("DELETE FROM Sale s WHERE s.completed = true AND s.productType != :productType AND YEAR(s.dateCreation) = :year")
    void deleteCompletedSalesByYear(@Param("year") int year, @Param("productType") String productType);

    // ELIMINAR TODAS LAS VENTAS COMPLETADAS DE UN MES ESPECIFICO
    @Query("DELETE FROM Sale s WHERE s.completed = true AND s.productType != :productType AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    void deleteCompletedSalesByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("productType") String productType);

    // ELIMINAR TODAS LAS VENTAS COMPLETADAS DE UN CLIENTE
    @Query("DELETE FROM Sale s WHERE s.completed = true AND s.productType != :productType AND s.client.Id = :clientId")
    void deleteCompletedSalesByClientId(@Param("clientId") Long clientId, @Param("productType") String productType);

    // OBTENER EL MONTO TOTAL DE VENTAS DE UN AÑO ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.productType != :productType AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalSalesAmountByYear(@Param("year") int year, @Param("productType") String productType);

    // OBTENER EL MONTO TOTAL DE VENTAS PAGADAS DE UN AÑO ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.completed = true AND s.productType != :productType AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalPaidSalesAmountByYear(@Param("year") int year, @Param("productType") String productType);

    // OBTENER EL MONTO TOTAL DE VENTAS DE UN MES ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.productType != :productType AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalSalesAmountByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("productType") String productType);

    // OBTENER EL MONTO TOTAL DE VENTAS PAGADAS DE UN MES ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.completed = true AND s.productType != :productType AND MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalPaidSalesAmountByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("productType") String productType);

    // OBTENER EL MONTO TOTAL DE VENTAS DE UN DIA ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.productType != :productType AND DATE(s.dateCreation) = :date")
    Optional<Double> findTotalSalesAmountByDate(@Param("date") LocalDate date, @Param("productType") String productType);


    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
       "WHERE s.productType != :productType " +
       "AND f.paid = TRUE " +
       "AND YEAR(f.datePayment) = :year")
    Optional<Double> findTotalCollectedAmountByYear(@Param("year") int year, @Param("productType") String productType);

    // Monto total cobrado de productos por mes y año (excluyendo tipo)
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
        "WHERE s.productType != :productType " +
        "AND f.paid = TRUE " +
        "AND MONTH(f.datePayment) = :month " +
        "AND YEAR(f.datePayment) = :year")
    Optional<Double> findTotalCollectedAmountByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("productType") String productType);

    // Ganancia total de productos por mes y año (excluyendo tipo)
    // Ajusta el campo 'profit' según tu modelo, por ejemplo: s.profit o s.priceTotal - s.cost
    @Query("SELECT SUM(s.priceTotal - s.cost) FROM Sale s " +
        "WHERE s.productType != :productType " +
        "AND MONTH(s.dateCreation) = :month " +
        "AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalProfitByMonthAndYear(@Param("month") int month, @Param("year") int year, @Param("productType") String productType);


    // Ganancia total de productos por año (excluyendo tipo)
    // Ajusta 's.profit' por el campo o fórmula de ganancia que uses
    @Query("SELECT SUM(s.priceTotal - s.cost) FROM Sale s " +
        "WHERE s.productType != :productType " +
        "AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalProfitByYear(@Param("year") int year, @Param("productType") String productType);

} 

