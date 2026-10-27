package com.app.digital.payments.digital_pyments.services;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Client;
import com.app.digital.payments.digital_pyments.models.dtos.SellerMetricDto;
import com.app.digital.payments.digital_pyments.models.dtos.SellerMetricsDto;
import com.app.digital.payments.digital_pyments.repositories.IClientRepository;
import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;
import com.app.digital.payments.digital_pyments.services.iservices.ISellerMetricServices;
import com.app.digital.payments.digital_pyments.utils.CurrencyUtils;
import com.app.digital.payments.digital_pyments.utils.SaleKind;
import com.app.digital.payments.digital_pyments.utils.SaleStatus;
import com.app.digital.payments.digital_pyments.utils.TenantContext;

/**
 * Métricas por vendedor del período: quién movió qué y qué cartera tiene hoy.
 *
 * Reglas acordadas con el negocio:
 * - "Vendido" y "cobrado" siguen el sello de cada operación (no se mueven si el
 *   cliente se reasigna); "deuda" es la cartera actual del vendedor.
 * - Fechas como el tablero: vendido por fecha de venta (incluye las luego
 *   anuladas, informadas aparte) y cobrado por fecha de cobro.
 * - Montos por moneda y separados VENTA/PRESTAMO; la fila "Sin vendedor"
 *   completa la reconciliación con los totales generales.
 */
@Service
public class SellerMetricServices implements ISellerMetricServices {

    private final ISaleRepository saleRepository;
    private final IClientRepository clientRepository;

    SellerMetricServices(ISaleRepository saleRepository, IClientRepository clientRepository) {
        this.saleRepository = saleRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SellerMetricsDto getSellerMetrics(int year, int month) {
        // Fila por vendedor; la clave nula queda reservada para "Sin vendedor".
        Map<Long, SellerMetricDto> filas = new LinkedHashMap<>();
        for (Client vendedor : clientRepository.findAllSellersActivos(TenantContext.requireBusinessId())) {
            filas.put(vendedor.getId(), nuevaFila(vendedor.getId(), vendedor.getName(), true));
        }
        filas.put(null, nuevaFila(null, "Sin vendedor", false));

        // Cartera actual: clientes asignados a cada fila
        for (Object[] row : clientRepository.countCarteraBySeller()) {
            filaDe(filas, aLong(row[0])).setClientesCartera(((Number) row[1]).longValue());
        }

        // Actividad del período sellada en la operación (ventas y préstamos aparte)
        for (Object[] row : saleRepository.findSellerMetricsByPeriod(month, year, SaleStatus.CANCELED)) {
            SellerMetricDto fila = filaDe(filas, aLong(row[0]));
            SaleKind kind = (SaleKind) row[1];
            fila.getOperaciones().merge(kind, ((Number) row[3]).longValue(), Long::sum);
            fila.getAnuladas().merge(kind, ((Number) row[4]).longValue(), Long::sum);
            CurrencyUtils.acumular(fila.getVendido().get(kind), (String) row[2], (BigDecimal) row[5]);
        }

        for (Object[] row : saleRepository.findSellerCollectedByPeriod(month, year)) {
            SellerMetricDto fila = filaDe(filas, aLong(row[0]));
            CurrencyUtils.acumular(fila.getCobrado().get((SaleKind) row[1]), (String) row[2], (BigDecimal) row[3]);
        }

        // Deuda de la cartera actual (no la de las operaciones del vendedor)
        for (Object[] row : saleRepository.findSellerPortfolioDebtByKindAndCurrency(SaleStatus.ACTIVE)) {
            SellerMetricDto fila = filaDe(filas, aLong(row[0]));
            CurrencyUtils.acumular(fila.getDeuda().get((SaleKind) row[1]), (String) row[2], (BigDecimal) row[3]);
        }

        SellerMetricsDto respuesta = new SellerMetricsDto();
        respuesta.setYear(year);
        respuesta.setMonth(month);
        respuesta.setSinVendedor(filas.remove(null));
        respuesta.setVendedores(List.copyOf(filas.values()));
        respuesta.setMonedasDisponibles(monedas(respuesta.getVendedores(), respuesta.getSinVendedor()));
        return respuesta;
    }

    private static SellerMetricDto nuevaFila(Long sellerId, String nombre, boolean activo) {
        SellerMetricDto fila = new SellerMetricDto();
        fila.setSellerId(sellerId);
        fila.setSellerName(nombre);
        fila.setActivo(activo);
        return fila;
    }

    /**
     * Fila del vendedor en los agregados. Si aparece alguien que ya no está
     * habilitado, se conserva su nombre para el historial (marcado inactivo).
     */
    private SellerMetricDto filaDe(Map<Long, SellerMetricDto> filas, Long sellerId) {
        SellerMetricDto fila = filas.get(sellerId);
        if (fila != null) {
            return fila;
        }
        String nombre = clientRepository.findById(sellerId)
                .map(Client::getName)
                .orElse("Vendedor no disponible");
        fila = nuevaFila(sellerId, nombre, false);
        filas.put(sellerId, fila);
        return fila;
    }

    private static Long aLong(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : null;
    }

    private static List<String> monedas(List<SellerMetricDto> vendedores, SellerMetricDto sinVendedor) {
        Set<String> monedas = new TreeSet<>();
        for (SellerMetricDto fila : vendedores) {
            recolectarMonedas(fila, monedas);
        }
        recolectarMonedas(sinVendedor, monedas);
        return List.copyOf(monedas);
    }

    private static void recolectarMonedas(SellerMetricDto fila, Set<String> destino) {
        fila.getVendido().values().forEach(montos -> destino.addAll(montos.keySet()));
        fila.getCobrado().values().forEach(montos -> destino.addAll(montos.keySet()));
        fila.getDeuda().values().forEach(montos -> destino.addAll(montos.keySet()));
    }
}
