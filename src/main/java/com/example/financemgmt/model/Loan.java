package com.example.financemgmt.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @Column(length = 220)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 30)
    private LoanType loanType;
    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;
    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal interestRate;
    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_frequency", nullable = false, length = 20)
    private RepaymentFrequency repaymentFrequency;
    @Column(name = "weekly_day", length = 15)
    private String weeklyDay;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "installment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal installmentAmount;
    @Column(name = "installment_count", nullable = false)
    private int installmentCount;
    @Column(name = "next_installment_date", nullable = false)
    private LocalDate nextInstallmentDate;

    protected Loan() {}

    public Loan(String id, Customer customer, LoanType loanType, BigDecimal principalAmount,
                BigDecimal interestRate, RepaymentFrequency repaymentFrequency, String weeklyDay,
                LocalDate startDate, BigDecimal installmentAmount, int installmentCount,
                LocalDate nextInstallmentDate) {
        this.id = id;
        this.customer = customer;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.repaymentFrequency = repaymentFrequency;
        this.weeklyDay = weeklyDay;
        this.startDate = startDate;
        this.installmentAmount = installmentAmount;
        this.installmentCount = installmentCount;
        this.nextInstallmentDate = nextInstallmentDate;
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public LoanType getLoanType() { return loanType; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public RepaymentFrequency getRepaymentFrequency() { return repaymentFrequency; }
    public String getWeeklyDay() { return weeklyDay; }
    public LocalDate getStartDate() { return startDate; }
    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public int getInstallmentCount() { return installmentCount; }
    public LocalDate getNextInstallmentDate() { return nextInstallmentDate; }
    public void advanceNextInstallmentDate(LocalDate nextInstallmentDate) {
        this.nextInstallmentDate = nextInstallmentDate;
    }
}
