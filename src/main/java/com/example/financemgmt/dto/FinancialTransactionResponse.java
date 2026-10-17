package com.example.financemgmt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionResponse(Long id, String transactionType, BigDecimal amount,
                                           LocalDate transactionDate, String loanId, String customerName) {}
