package com.app.digital.payments.digital_pyments.repositories;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Client;


@Repository
public interface IClientRepository extends JpaRepository<Client, Long> {
    
   // --- Consultas para VENTAS (cuando descriptiononProduct es distinto de 'PRESTAMO') ---

    /**
     * Calcula la deuda total (suma de remainingAmount) para las ventas de un cliente que no están completadas.
     * Una venta es considerada "no completada" si completed es false y descriptiononProduct no es 'PRESTAMO'.
     * @param clientId El ID del cliente.
     * @return La suma del remainingAmount de las ventas no completadas.
     */
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.client.Id = :clientId AND s.completed = FALSE AND s.descriptionProduct <> 'PRESTAMO'")
    BigDecimal sumRemainingAmountByClientIdAndSalesNotCompleted(Long clientId);

    /**
     * Calcula el total de ventas pagadas (suma de priceTotal) para un cliente.
     * Una venta es considerada "pagada" si completed es true y descriptiononProduct no es 'PRESTAMO'.
     * @param clientId El ID del cliente.
     * @return La suma del priceTotal de las ventas completadas.
     */
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.client.Id = :clientId AND s.completed = TRUE AND s.descriptionProduct <> 'PRESTAMO'")
    BigDecimal sumPriceTotalByClientIdAndSalesCompleted(Long clientId);

    // --- Consultas para PRESTAMOS (cuando descriptiononProduct es igual a 'PRESTAMO') ---

    /**
     * Calcula la deuda total (suma de remainingAmount) para los préstamos de un cliente que no están completados.
     * Un préstamo es considerado "no completado" si completed es false y descriptiononProduct es 'PRESTAMO'.
     * @param clientId El ID del cliente.
     * @return La suma del remainingAmount de los préstamos no completados.
     */
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.client.Id = :clientId AND s.completed = FALSE AND s.descriptionProduct = 'PRESTAMO'")
    BigDecimal sumRemainingAmountByClientIdAndLoansNotCompleted(Long clientId);

    /**
     * Calcula el total de préstamos pagados (suma de priceTotal) para un cliente.
     * Un préstamo es considerado "pagado" si completed es true y descriptiononProduct es 'PRESTAMO'.
     * @param clientId El ID del cliente.
     * @return La suma del priceTotal de los préstamos completados.
     */
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.client.Id = :clientId AND s.completed = TRUE AND s.descriptionProduct = 'PRESTAMO'")
    BigDecimal sumPriceTotalByClientIdAndLoansCompleted(Long clientId);
}
