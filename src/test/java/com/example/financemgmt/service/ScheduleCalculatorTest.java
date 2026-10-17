package com.example.financemgmt.service;

import com.example.financemgmt.model.RepaymentFrequency;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleCalculatorTest {
    @Test
    void roundsInstallmentsUpAndProfitToCents() {
        assertEquals(4, ScheduleCalculator.installmentCount(new BigDecimal("100"), new BigDecimal("30")));
        assertEquals(new BigDecimal("12.35"),
                ScheduleCalculator.profit(new BigDecimal("123.45"), new BigDecimal("10")));
    }

    @Test
    void initializesAndAdvancesFrequencyDates() {
        LocalDate jan31 = LocalDate.of(2025, 1, 31);
        assertEquals(LocalDate.of(2025, 2, 28),
                ScheduleCalculator.firstDueDate(jan31, RepaymentFrequency.MONTHLY, null));
        assertEquals(LocalDate.of(2025, 3, 31),
                ScheduleCalculator.advanceDueDate(LocalDate.of(2025, 2, 28), jan31, RepaymentFrequency.MONTHLY, null));
        assertEquals(LocalDate.of(2025, 2, 3),
                ScheduleCalculator.firstDueDate(LocalDate.of(2025, 1, 31), RepaymentFrequency.WEEKLY, DayOfWeek.MONDAY));
        assertEquals(LocalDate.of(2025, 2, 10),
                ScheduleCalculator.advanceDueDate(LocalDate.of(2025, 2, 3), jan31, RepaymentFrequency.WEEKLY, DayOfWeek.MONDAY));
        assertEquals(LocalDate.of(2025, 2, 1),
                ScheduleCalculator.firstDueDate(jan31, RepaymentFrequency.DAILY, null));
    }
}
