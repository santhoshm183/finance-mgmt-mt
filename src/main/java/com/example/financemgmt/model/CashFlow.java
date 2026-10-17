package com.example.financemgmt.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_flow")
public class CashFlow {
    @Id
    private Long id;
    @Column(name = "investment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal investmentAmount;
    @Column(name = "financial_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal financialBalance;
    @Column(name = "profit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal profitAmount;
    @Column(name = "collection_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal collectionAmount;

    protected CashFlow() {}
    public CashFlow(Long id, BigDecimal investmentAmount, BigDecimal financialBalance,
                    BigDecimal profitAmount, BigDecimal collectionAmount) {
        this.id = id;
        this.investmentAmount = investmentAmount;
        this.financialBalance = financialBalance;
        this.profitAmount = profitAmount;
        this.collectionAmount = collectionAmount;
    }
    public Long getId() { return id; }
    public BigDecimal getInvestmentAmount() { return investmentAmount; }
    public BigDecimal getFinancialBalance() { return financialBalance; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public BigDecimal getCollectionAmount() { return collectionAmount; }
    public void setInvestmentAmount(BigDecimal value) { investmentAmount = value; }
    public void setFinancialBalance(BigDecimal value) { financialBalance = value; }
    public void setProfitAmount(BigDecimal value) { profitAmount = value; }
    public void setCollectionAmount(BigDecimal value) { collectionAmount = value; }
}
