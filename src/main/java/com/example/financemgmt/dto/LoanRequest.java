package com.example.financemgmt.dto;

import com.example.financemgmt.model.LoanType;
import com.example.financemgmt.model.RepaymentFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanRequest(
        @NotNull Long customerId,
        @NotNull LoanType loanType,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal principalAmount,
        @NotNull @DecimalMin(value = "0.0000") BigDecimal interestRate,
        @NotNull RepaymentFrequency repaymentFrequency,
        @Size(max = 15) String weeklyDay,
        @NotNull LocalDate startDate,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal installmentAmount) {}
