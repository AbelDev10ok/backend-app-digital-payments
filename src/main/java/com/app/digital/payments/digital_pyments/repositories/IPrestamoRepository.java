package com.app.digital.payments.digital_pyments.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Sale;

@Repository
public interface IPrestamoRepository extends JpaRepository<Sale, Long>{
    
    // PRESTAMOS

    // OBTENGO TODOS LOS PRESTAMOS
    List<Sale> findByDescriptionProduct(String descriptiononProduct);

    //OBTENGO TODOS LOS PRESTAMOS POR CLIENTE 
    List<Sale> findByClientIdAndDescriptionProductOrderByDateCreationDesc(Long clientId, String descriptiononProduct);

    //OBTENGO TODOS LOS PRESTAMOS POR CLIENTE NO PAGADOS
    List<Sale> findByClientIdAndDescriptionProductAndCompletedFalseOrderByDateCreationDesc(Long clientId, String descriptiononProduct);
    
    //OBTENGO TODOS LOS PRESTAMOS POR CLIENTE PAGADOS
    List<Sale> findByClientIdAndDescriptionProductAndCompletedTrueOrderByDateCreationDesc(Long clientId, String descriptiononProduct); 

    //OBTENGO LA CANTIDAD DE PRESTAMOS COMPLETADOS POR CLIENTE
    Long countByClientIdAndCompletedTrue(Long clientId);
    
    //OBTENER LA CANTIDAD DE PRESTAMOS SIN COMPLETAR POR CLIENTE     
    Long countByClientIdAndCompletedFalse(Long clientId);

    //PRESTAMOS QUE HAN SUPERADO SU FECHA FINAL DE PAGO      
    List<Sale> findByFinalPaymentDateBeforeAndDescriptionProduct(LocalDate date, String descriptiononProduct);
        
    //PRESTAMOS QUE NO ESTAN COMPLETADOS 
    List<Sale> findByCompletedFalseAndDescriptionProduct(String descriptiononProduct);

    //PRESTAMOS QUE TIENEN MAS DE X DIAS DE ATRASO     
    List<Sale> findByDaysLateGreaterThanAndDescriptionProduct(int daysLate, String descriptiononProduct);

    //PRESTAMOS CON CUOTAS A COBRAR HOY (O UNA FECHA ESPECIFICA)     
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate = :date AND f.paid = false AND s.descriptionProduct = :productDescription")
    List<Sale> findSalesWithFeesToChargeTodayAndProductDescription(@Param("date") LocalDate date, @Param("productDescription") String productDescription);

    //PRESTAMOS CON CUOTAS ATRASADAS
    @Query("SELECT DISTINCT s FROM Sale s JOIN s.fees f " +
            "WHERE f.expirationDate < :date AND f.paid = false AND s.descriptionProduct = :productDescription")
    List<Sale> findSalesWithDelayedFeesAndProductDescription(@Param("date") LocalDate date, @Param("productDescription") String productDescription);
   
    // ELIMINAR TODOS LOS PRESTAMOS PAGADOS DE UN AÑO ESPECIFICO
    @Query("DELETE FROM Sale s WHERE s.completed = true AND s.descriptionProduct = 'PRESTAMO' AND YEAR(s.dateCreation) = :year")
    void deletePaidLoansByYear(@Param("year") int year);

    // ELIMINAR TODOS LOS PRESTAMOS PAGADOS DE UN MES ESPECIFICO
    @Query("DELETE FROM Sale s WHERE s.completed = true AND s.descriptionProduct = 'PRESTAMO' AND  MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    void deletePaidLoansByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // ELIMINAR TODOS LOS PRESTAMOS PAGADOS DE UN CLIENTE
    @Query("DELETE FROM Sale s WHERE s.completed = true AND s.descriptionProduct = 'PRESTAMO' AND  s.client.Id = :clientId")
    void deletePaidLoansByClientId(@Param("clientId") Long clientId);

    // OBTENER MONTO TOTAL DE PRESTAMOS PAGADOS DE UN AÑO ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.completed = true AND  s.descriptionProduct = 'PRESTAMO' AND  YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalPaidLoansAmountByYear(@Param("year") int year);

    // OBTENER EL TOTAL DEL VALOR DE TODOS LOS PRESTAMOS HECHOS DE UN AÑO ESPECIFICO     
    @Query("SELECT SUM(s.priceTotal) FROM Sale s " +
           "WHERE s.descriptionProduct = 'PRESTAMO' " +
           "AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalPriceOfLoansCreatedByYear(
        @Param("year") int year
    );

   //  OBTENER MONTO TOTAL QUE VAMOS COBRANDO DEL AÑO DE LOS PRESTAMOS    
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
           "WHERE s.descriptionProduct = 'PRESTAMO' " +
           "AND f.paid = TRUE " +
           "AND YEAR(f.datePayment) = :year")
    Optional<Double> findTotalCollectedLoansAmountByYear(
        @Param("year") int year
    );


    // OBTENER MONTO TOTAL DE PRESTAMOS PAGADOS DE MES ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.completed = true AND s.descriptionProduct = 'PRESTAMO' AND  MONTH(s.dateCreation) = :month AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalPaidLoansAmountByMonthAndYear(@Param("month") int month, @Param("year") int year);


   // OBTENER EL TOTAL DEL VALOR DE TODOS LOS PRESTAMOS HECHOS DE UN MES ESPECIFICO     
    @Query("SELECT SUM(s.priceTotal) FROM Sale s " +
           "WHERE s.descriptionProduct = 'PRESTAMO' " +
           "AND MONTH(s.dateCreation) = :month " +
           "AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalPriceOfLoansCreatedByMonthAndYear(
        @Param("month") int month,
        @Param("year") int year
    );


   //  OBTENER MONTO TOTAL QUE VAMOS COBRANDO DEL MES DE LOS PRESTAMOS    
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f " +
           "WHERE s.descriptionProduct = 'PRESTAMO' " +
           "AND f.paid = TRUE " +
           "AND MONTH(f.datePayment) = :month " +
           "AND YEAR(f.datePayment) = :year")
    Optional<Double> findTotalCollectedLoansAmountByMonthAndYear(
        @Param("month") int month,
        @Param("year") int year
    );

    // OBTENER MONTO TOTAL DE PRESTAMOS PAGADOS UN DIA ESPECIFICO
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.completed = true AND s.descriptionProduct = 'PRESTAMO' AND  DATE(s.dateCreation) = :date")
    Optional<Double> findTotalPaidLoansAmountByDate(@Param("date") LocalDate date);

    // OBTENER GANANCIAS TOTALES DE UN MES ESPECIFICO
    @Query("SELECT SUM(s.priceTotal - s.cost) FROM Sale s " +
        "WHERE s.descriptionProduct = 'PRESTAMO' " +
        "AND MONTH(s.dateCreation) = :month " +
        "AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalProfitOfLoansByMonthAndYear(
        @Param("month") int month,
        @Param("year") int year
    );

    // OBTENER GANANCIAS TOTALES DE UN AÑO ESPECIFICO
    @Query("SELECT SUM(s.priceTotal - s.cost) FROM Sale s " +
        "WHERE s.descriptionProduct = 'PRESTAMO' " +
        "AND YEAR(s.dateCreation) = :year")
    Optional<Double> findTotalProfitOfLoansByYear(
        @Param("year") int year
    );
}
