package com.example.financemgmt.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_transfers")
public class CashTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferSource source;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CashTransfer() {}
    public CashTransfer(TransferSource source, BigDecimal amount, LocalDate transferDate) {
        this.source = source;
        this.amount = amount;
        this.transferDate = transferDate;
        this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public TransferSource getSource() { return source; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getTransferDate() { return transferDate; }
}
