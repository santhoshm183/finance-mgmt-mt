package com.example.financemgmt.dto;

import com.example.financemgmt.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(String id, Long customerId, String customerName, LoanType loanType,
                           BigDecimal principalAmount, BigDecimal interestRate,
                           RepaymentFrequency repaymentFrequency, String weeklyDay,
                           LocalDate startDate, BigDecimal installmentAmount, int installmentCount,
                           LocalDate nextInstallmentDate) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getCustomer().getId(), loan.getCustomer().getName(),
                loan.getLoanType(), loan.getPrincipalAmount(), loan.getInterestRate(),
                loan.getRepaymentFrequency(), loan.getWeeklyDay(), loan.getStartDate(),
                loan.getInstallmentAmount(), loan.getInstallmentCount(), loan.getNextInstallmentDate());
    }
}
