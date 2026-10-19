package com.example.financemgmt.service;

import com.example.financemgmt.dto.CollectionRequest;
import com.example.financemgmt.exception.BadRequestException;
import com.example.financemgmt.exception.NotFoundException;
import com.example.financemgmt.model.CashFlow;
import com.example.financemgmt.model.Loan;
import com.example.financemgmt.model.LoanType;
import com.example.financemgmt.model.TransactionType;
import com.example.financemgmt.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@Transactional
public class CollectionService {
    private final LoanRepository loanRepository;
    private final CashFlowService cashFlowService;
    public CollectionService(LoanRepository loanRepository, CashFlowService cashFlowService) {
        this.loanRepository = loanRepository;
        this.cashFlowService = cashFlowService;
    }

    public void collect(CollectionRequest request) {
        CashFlow account = cashFlowService.lock();
        Loan loan = loanRepository.findLockedById(request.loanId())
                .orElseThrow(() -> new NotFoundException("Loan " + request.loanId() + " was not found"));
        if (loan.getLoanType() == LoanType.INSTALLMENT_BASED
                && request.transactionType() != TransactionType.COLLECTED) {
            throw new BadRequestException("Transaction type for an installment-based loan must be COLLECTED");
        }
        cashFlowService.collect(account, request.amount(), request.collectionDate(), loan.getId(), request.transactionType());
        boolean advancesDueDate = loan.getLoanType() == LoanType.MONTHLY_INTEREST
                ? request.transactionType() == TransactionType.PROFIT
                : request.transactionType() == TransactionType.COLLECTED;
        if (advancesDueDate) {
            DayOfWeek day = loan.getWeeklyDay() == null ? null : DayOfWeek.valueOf(loan.getWeeklyDay());
            LocalDate next = ScheduleCalculator.advanceDueDate(loan.getNextInstallmentDate(), loan.getStartDate(),
                    loan.getRepaymentFrequency(), day);
            loan.advanceNextInstallmentDate(next);
        }
    }
}
