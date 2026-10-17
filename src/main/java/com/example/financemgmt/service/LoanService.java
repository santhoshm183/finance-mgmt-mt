package com.example.financemgmt.service;

import com.example.financemgmt.dto.LoanRequest;
import com.example.financemgmt.exception.BadRequestException;
import com.example.financemgmt.exception.ConflictException;
import com.example.financemgmt.exception.NotFoundException;
import com.example.financemgmt.model.*;
import com.example.financemgmt.repository.CustomerRepository;
import com.example.financemgmt.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class LoanService {
    private static final DateTimeFormatter ID_DATE = DateTimeFormatter.ofPattern("ddMMuuuu");
    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final CashFlowService cashFlowService;

    public LoanService(LoanRepository loanRepository, CustomerRepository customerRepository,
            CashFlowService cashFlowService) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
        this.cashFlowService = cashFlowService;
    }

    @Transactional(readOnly = true)
    public List<Loan> findAll(Long customerId) {
        if (customerId == null)
            return loanRepository.findAll();
        return loanRepository.findByCustomer_IdOrderByStartDateDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Loan findById(String id) {
        return loanRepository.findById(id).orElseThrow(() -> new NotFoundException("Loan " + id + " was not found"));
    }

    @Transactional(readOnly = true)
    public List<Loan> pending(LocalDate today) {
        System.out.println("Pending loans as of " + today + ":");
        return loanRepository.findByNextInstallmentDateLessThanEqualOrderByNextInstallmentDateAsc(today);
    }

    public Loan create(LoanRequest request) {
        DayOfWeek weeklyDay = parseWeeklyDay(request);
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer " + request.customerId() + " was not found"));
        CashFlow cashFlow = cashFlowService.lock();
        CashFlowService.requireAvailable(cashFlow.getFinancialBalance(), request.principalAmount(),
                "financial balance");
        String id = uniqueLoanId(customer.getName(), request.startDate());
        int count = ScheduleCalculator.installmentCount(request.principalAmount(), request.installmentAmount());
        LocalDate nextDate = ScheduleCalculator.firstDueDate(request.startDate(), request.repaymentFrequency(),
                weeklyDay);
        Loan loan = new Loan(id, customer, request.loanType(), request.principalAmount(), request.interestRate(),
                request.repaymentFrequency(), weeklyDay == null ? null : weeklyDay.name(), request.startDate(),
                request.installmentAmount(), count, nextDate);
        loanRepository.save(loan);
        cashFlow.setFinancialBalance(cashFlow.getFinancialBalance().subtract(request.principalAmount()));
        cashFlowService.addProfit(cashFlow,
                ScheduleCalculator.profit(request.principalAmount(), request.interestRate()), request.startDate(), id);
        return loan;
    }

    private DayOfWeek parseWeeklyDay(LoanRequest request) {
        if (request.repaymentFrequency() == RepaymentFrequency.WEEKLY) {
            if (request.weeklyDay() == null || request.weeklyDay().isBlank())
                throw new BadRequestException("weeklyDay is required for WEEKLY repaymentFrequency");
            try {
                return DayOfWeek.valueOf(request.weeklyDay().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("weeklyDay must be a valid day of week (MONDAY through SUNDAY)");
            }
        }
        if (request.weeklyDay() != null && !request.weeklyDay().isBlank())
            throw new BadRequestException("weeklyDay is only valid for WEEKLY repaymentFrequency");
        return null;
    }

    private String uniqueLoanId(String customerName, LocalDate startDate) {
        String slug = customerName.trim().replaceAll("[^\\p{L}\\p{M}\\p{N}]+", "-").replaceAll("^-|-$", "");
        if (slug.isBlank())
            slug = "Customer";
        String base = slug + "-" + ID_DATE.format(startDate);
        String candidate = base;
        int suffix = 2;
        while (loanRepository.existsById(candidate)) {
            candidate = base + "-" + suffix++;
        }
        if (candidate.length() > 220)
            throw new ConflictException("Generated loan ID exceeds the supported length");
        return candidate;
    }
}
