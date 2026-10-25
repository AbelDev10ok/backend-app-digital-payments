package com.app.digital.payments.digital_pyments.utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.app.digital.payments.digital_pyments.models.Business;

/**
 * Resolución de la moneda para el snapshot por transacción.
 *
 * La moneda (ISO 4217) se congela al crear el registro — venta/préstamo o
 * producto — y nunca se edita después: los montos no se convierten, solo se
 * etiquetan con la moneda vigente del negocio en el momento de crearlos.
 */
public final class CurrencyUtils {

    /** Moneda por defecto cuando el negocio no define una (misma que al registrarse). */
    public static final String MONEDA_DEFAULT = "ARS";

    /**
     * Monedas que la app ofrece explícitamente (ISO 4217). Es la única fuente de
     * verdad del backend y la misma lista que usa el frontend; cualquier otro
     * código queda fuera para que un tipeo no cree una moneda paralela (el
     * snapshot de la moneda es inmutable, así que el error sería permanente).
     */
    public static final List<String> MONEDAS_SOPORTADAS = List.of("ARS", "USD");

    private CurrencyUtils() {
    }

    /** ¿La moneda (normalizada a mayúsculas) es una de las soportadas? */
    public static boolean esSoportada(String currency) {
        return currency != null && MONEDAS_SOPORTADAS.contains(currency.trim().toUpperCase());
    }

    /**
     * Toma la moneda vigente del negocio en formato ISO 4217 (mayúsculas).
     * Null-safe a propósito: los tests unitarios no stubbean el negocio y puede
     * haber registros legados sin moneda.
     */
    public static String snapshot(Business business) {
        if (business == null || business.getCurrency() == null || business.getCurrency().isBlank()) {
            return MONEDA_DEFAULT;
        }
        return business.getCurrency().trim().toUpperCase();
    }

    /**
     * Moneda de un registro ya creado. Si por ser legado viniera nula, se usa la
     * indicada como respaldo (normalmente la vigente del negocio).
     */
    public static String orDefault(String currency, String fallback) {
        if (currency == null || currency.isBlank()) {
            return fallback != null && !fallback.isBlank() ? fallback.trim().toUpperCase() : MONEDA_DEFAULT;
        }
        return currency.trim().toUpperCase();
    }

    /**
     * Acumula un monto en el mapa por moneda. Sumar dentro de una misma moneda
     * está permitido; lo que nunca se hace es sumar monedas distintas.
     */
    public static void acumular(Map<String, BigDecimal> montos, String currency, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        montos.merge(orDefault(currency, null), amount, BigDecimal::add);
    }

    /**
     * Convierte filas (moneda, monto) de una query agrupada por moneda en un
     * mapa ordenado {moneda → monto}.
     */
    public static Map<String, BigDecimal> montosPorMoneda(List<Object[]> rows) {
        Map<String, BigDecimal> montos = new TreeMap<>();
        for (Object[] row : rows) {
            acumular(montos, (String) row[0], row[1] instanceof BigDecimal monto ? monto : null);
        }
        return montos;
    }
}
