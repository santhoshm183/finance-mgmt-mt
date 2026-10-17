package com.example.financemgmt.service;

import com.example.financemgmt.dto.TransferRequest;
import com.example.financemgmt.exception.ConflictException;
import com.example.financemgmt.exception.NotFoundException;
import com.example.financemgmt.model.*;
import com.example.financemgmt.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CashFlowService {
    private final CashFlowRepository cashFlowRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final CashTransferRepository transferRepository;

    public CashFlowService(CashFlowRepository cashFlowRepository,
                           FinancialTransactionRepository transactionRepository,
                           CashTransferRepository transferRepository) {
        this.cashFlowRepository = cashFlowRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    public CashFlow lock() {
        return cashFlowRepository.lockSingleton()
                .orElseThrow(() -> new NotFoundException("Cash flow account is not initialized"));
    }
    @Transactional(readOnly = true)
    public CashFlow get() {
        return cashFlowRepository.findById(1L)
                .orElseThrow(() -> new NotFoundException("Cash flow account is not initialized"));
    }
    public void addProfit(CashFlow cashFlow, BigDecimal amount, java.time.LocalDate date, String loanId) {
        cashFlow.setProfitAmount(cashFlow.getProfitAmount().add(amount));
        transactionRepository.save(new FinancialTransaction("PROFIT", amount, date, loanId));
    }
    public void collect(CashFlow cashFlow, BigDecimal amount, java.time.LocalDate date,
                        String loanId, TransactionType type) {
        if (type == TransactionType.COLLECTED) cashFlow.setCollectionAmount(cashFlow.getCollectionAmount().add(amount));
        else cashFlow.setProfitAmount(cashFlow.getProfitAmount().add(amount));
        transactionRepository.save(new FinancialTransaction(type.name(), amount, date, loanId));
    }
    public CashTransfer transfer(TransferRequest request) {
        CashFlow account = lock();
        BigDecimal amount = request.amount();
        switch (request.fromAccount()) {
            case INVESTMENT -> {
                account.setInvestmentAmount(account.getInvestmentAmount().add(amount));
                account.setFinancialBalance(account.getFinancialBalance().add(amount));
            }
            case FINANCIAL_BALANCE -> requireAvailable(account.getFinancialBalance(), amount, "financial balance");
            case PROFIT -> {
                requireAvailable(account.getProfitAmount(), amount, "profit amount");
                account.setProfitAmount(account.getProfitAmount().subtract(amount));
                account.setFinancialBalance(account.getFinancialBalance().add(amount));
            }
            case COLLECTION -> {
                requireAvailable(account.getCollectionAmount(), amount, "collection amount");
                account.setCollectionAmount(account.getCollectionAmount().subtract(amount));
                account.setFinancialBalance(account.getFinancialBalance().add(amount));
            }
        }
        return transferRepository.save(new CashTransfer(request.fromAccount(), amount, request.date()));
    }
    public static void requireAvailable(BigDecimal available, BigDecimal requested, String account) {
        if (available.compareTo(requested) < 0) throw new ConflictException("Insufficient " + account);
    }
}
