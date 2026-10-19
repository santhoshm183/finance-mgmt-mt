package com.example.financemgmt.dto;

import com.example.financemgmt.model.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingPaymentResponse(
        String id,
        String loanId,
        Long customerId,
        String customerName,
        LocalDate dueDate,
        LocalDate nextInstallmentDate,
        BigDecimal amount,
        BigDecimal installmentAmount,
        String loanType,
        String repaymentFrequency,
        String weeklyDay,
        String status) {

    public static PendingPaymentResponse from(Loan loan, LocalDate dueDate) {
        return new PendingPaymentResponse(
                loan.getId() + "-" + dueDate,
                loan.getId(),
                loan.getCustomer().getId(),
                loan.getCustomer().getName(),
                dueDate,
                dueDate,
                loan.getInstallmentAmount(),
                loan.getInstallmentAmount(),
                loan.getLoanType().name(),
                loan.getRepaymentFrequency().name(),
                loan.getWeeklyDay(),
                "PENDING");
    }
}
