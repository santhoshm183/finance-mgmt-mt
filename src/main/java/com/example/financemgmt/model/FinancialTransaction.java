package com.example.financemgmt.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_type", nullable = false, length = 40)
    private String transactionType;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    @Column(name = "loan_id", length = 220)
    private String loanId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FinancialTransaction() {}
    public FinancialTransaction(String transactionType, BigDecimal amount, LocalDate transactionDate, String loanId) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.loanId = loanId;
        this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public String getLoanId() { return loanId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
