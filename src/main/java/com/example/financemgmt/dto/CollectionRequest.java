package com.example.financemgmt.dto;

import com.example.financemgmt.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectionRequest(
        @NotBlank String loanId,
        @NotNull LocalDate collectionDate,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
        @NotNull TransactionType transactionType) {}
