package com.example.financemgmt.dto;

import com.example.financemgmt.model.TransferSource;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(
        @JsonAlias("source") @NotNull TransferSource fromAccount,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
        @NotNull LocalDate date) {}
