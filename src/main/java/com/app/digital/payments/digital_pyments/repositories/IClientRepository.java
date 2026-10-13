package com.app.digital.payments.digital_pyments.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;


@Repository
public interface IClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    
    // Contar clientes activos (que tienen al menos una venta)
    @Query("SELECT COUNT(DISTINCT c) FROM Client c JOIN c.sales s")
    long countActiveClients();
    
// --------------

    // obtener clientes sin vendedor
    @Query("SELECT c FROM Client c WHERE c.seller IS NULL")
    List<Client> findAllClientsWithoutSeller();

   // --- Consultas para VENTAS (kind = VENTA) ---

    /**
     * Calcula la deuda total (suma de remainingAmount) para las ventas de un cliente que no están completadas.
     * Una venta es considerada "no completada" si completed es false y kind es VENTA.
     * @param clientId El ID del cliente.
     * @return La suma del remainingAmount de las ventas no completadas.
     */
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.client.Id = :clientId AND s.status <> :status AND s.kind <> :kind")
    BigDecimal sumRemainingAmountByClientIdAndSalesNotCompleted(@Param("clientId") Long clientId,
            @Param("status") SaleStatus status, @Param("kind") SaleKind kind);

    /**
     * Calcula el total de ventas pagadas (suma de priceTotal) para un cliente.
     * Una venta es considerada "pagada" si completed es true y kind es VENTA.
     * @param clientId El ID del cliente.
     * @return La suma del priceTotal de las ventas completadas.
     */
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.client.Id = :clientId AND s.status = :status AND s.kind <> :kind")
    BigDecimal sumPriceTotalByClientIdAndSalesCompleted(@Param("clientId") Long clientId,
            @Param("status") SaleStatus status, @Param("kind") SaleKind kind);

    // --- Consultas para PRESTAMOS (kind = PRESTAMO) ---

    /**
     * Calcula la deuda total (suma de remainingAmount) para los préstamos de un cliente que no están completados.
     * Un préstamo es considerado "no completado" si completed es false y kind es PRESTAMO.
     * @param clientId El ID del cliente.
     * @return La suma del remainingAmount de los préstamos no completados.
     */
    @Query("SELECT SUM(s.remainingAmount) FROM Sale s WHERE s.client.Id = :clientId AND s.status <> :status AND s.kind = :kind")
    BigDecimal sumRemainingAmountByClientIdAndLoansNotCompleted(@Param("clientId") Long clientId,
            @Param("status") SaleStatus status, @Param("kind") SaleKind kind);

    /**
     * Calcula el total de préstamos pagados (suma de priceTotal) para un cliente.
     * Un préstamo es considerado "pagado" si completed es true y kind es PRESTAMO.
     * @param clientId El ID del cliente.
     * @return La suma del priceTotal de los préstamos completados.
     */
    @Query("SELECT SUM(s.priceTotal) FROM Sale s WHERE s.client.Id = :clientId AND s.status = :status AND s.kind = :kind")
    BigDecimal sumPriceTotalByClientIdAndLoansCompleted(@Param("clientId") Long clientId,
            @Param("status") SaleStatus status, @Param("kind") SaleKind kind);

    // cuenta cuantos clientes estan asignado a este vendedor
    @Query("SELECT COUNT(c) FROM Client c WHERE c.seller.id = :sellerId")
    long countBySellerId(Long sellerId);

    // obtener vendedores activos del negocio
    @Query("SELECT c FROM Client c WHERE c.isSeller = TRUE AND c.business.id = :businessId")
    List<Client> findAllSellersActivos(@Param("businessId") Long businessId);

    // obtener vendedores que sigan asignados a clientes del negocio
    // c.seller es el vendedor asignado al cliente, no el cliente que es vendedor
    @Query("SELECT DISTINCT c.seller FROM Client c WHERE c.seller IS NOT NULL AND c.seller.business.id = :businessId")
    List<Client> findSellersWithAssignedClients(@Param("businessId") Long businessId);


    // obtener clientes por vendedor
    List<Client> findBySellerId(Long sellerId);

    Optional<Client> findByDni(String dni);

    Optional<Client> findByName(String name);

    Optional<Client> findByEmail(String email);

    // Variantes con scope de negocio (dni/email son únicos dentro del tenant)
    Optional<Client> findByBusinessIdAndDni(Long businessId, String dni);

    Optional<Client> findByBusinessIdAndEmail(Long businessId, String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Client c JOIN c.sales s WHERE c.id = :clientId")
    boolean existenClientesConTransaccionesAsociadas(Long clientId);

    // Deuda total (remainingAmount) agrupada por cliente, para una lista de IDs
    @Query("SELECT s.client.id, SUM(s.remainingAmount) FROM Sale s WHERE s.client.id IN :clientIds AND s.status = :status GROUP BY s.client.id")
    List<Object[]> sumRemainingAmountByClientIds(@Param("clientIds") List<Long> clientIds, @Param("status") SaleStatus status);

    // Total cobrado de cuotas pagadas (VENTA, excluyendo PRESTAMO)
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f WHERE s.client.id = :clientId AND f.paid = TRUE AND s.kind <> :kind")
    BigDecimal sumCollectedAmountByClientIdExcludingKind(@Param("clientId") Long clientId, @Param("kind") SaleKind kind);

    // Total cobrado de cuotas pagadas (PRESTAMO)
    @Query("SELECT SUM(f.amount) FROM Sale s JOIN s.fees f WHERE s.client.id = :clientId AND f.paid = TRUE AND s.kind = :kind")
    BigDecimal sumCollectedAmountByClientIdByKind(@Param("clientId") Long clientId, @Param("kind") SaleKind kind);
}
