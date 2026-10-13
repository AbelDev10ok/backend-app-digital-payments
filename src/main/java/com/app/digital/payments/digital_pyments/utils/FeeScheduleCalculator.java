package com.app.digital.payments.digital_pyments.utils;

import java.time.LocalDate;

public final class FeeScheduleCalculator {

    private FeeScheduleCalculator() {
    }

    public static LocalDate nextExpirationDate(LocalDate date, Payments frequency) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return switch (frequency) {
            case SEMANAL -> date.plusWeeks(1);
            case QUINCENAL -> date.plusDays(15);
            case MENSUAL -> date.plusMonths(1);
            case CONTADO -> date.plusDays(1);
        };
    }

    public static LocalDate finalPaymentDate(LocalDate startDate, Payments frequency, int installments) {
        if (startDate == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser nula");
        }
        return switch (frequency) {
            case SEMANAL -> startDate.plusWeeks(installments - 1L).minusDays(1);
            case QUINCENAL -> startDate.plusDays((installments - 1L) * 15L).minusDays(1);
            case MENSUAL -> startDate.plusMonths(installments - 1L).minusDays(1);
            case CONTADO -> startDate;
        };
    }
}
