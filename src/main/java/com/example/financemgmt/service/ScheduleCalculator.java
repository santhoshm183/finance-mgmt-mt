package com.example.financemgmt.service;

import com.example.financemgmt.model.RepaymentFrequency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public final class ScheduleCalculator {
    private ScheduleCalculator() {}

    public static int installmentCount(BigDecimal principal, BigDecimal installment) {
        return principal.divide(installment, 0, RoundingMode.CEILING).intValueExact();
    }

    public static BigDecimal profit(BigDecimal principal, BigDecimal rate) {
        return principal.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public static LocalDate firstDueDate(LocalDate startDate, RepaymentFrequency frequency, DayOfWeek weeklyDay) {
        return switch (frequency) {
            case DAILY -> startDate.plusDays(1);
            case WEEKLY -> {
                LocalDate candidate = startDate.with(java.time.temporal.TemporalAdjusters.next(weeklyDay));
                yield candidate;
            }
            case MONTHLY -> monthDay(startDate.plusMonths(1), startDate.getDayOfMonth());
        };
    }

    public static LocalDate advanceDueDate(LocalDate currentDueDate, LocalDate startDate,
                                           RepaymentFrequency frequency, DayOfWeek weeklyDay) {
        return switch (frequency) {
            case DAILY -> currentDueDate.plusDays(1);
            case WEEKLY -> currentDueDate.with(java.time.temporal.TemporalAdjusters.next(weeklyDay));
            case MONTHLY -> monthDay(currentDueDate.plusMonths(1), startDate.getDayOfMonth());
        };
    }

    private static LocalDate monthDay(LocalDate inMonth, int dayOfMonth) {
        YearMonth month = YearMonth.from(inMonth);
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }
}
