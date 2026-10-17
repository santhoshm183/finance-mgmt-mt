package com.example.financemgmt.service;

import com.example.financemgmt.dto.*;
import com.example.financemgmt.exception.BadRequestException;
import com.example.financemgmt.exception.ConflictException;
import com.example.financemgmt.model.*;
import com.example.financemgmt.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FinanceWorkflowTest {
    @Autowired CustomerService customerService;
    @Autowired LoanService loanService;
    @Autowired CollectionService collectionService;
    @Autowired CashFlowService cashFlowService;
    @Autowired CustomerRepository customerRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired Validator validator;
    @Autowired ObjectMapper objectMapper;

    @Test
    void initializesCashFlowAccountForTransfers() {
        CashFlow account = cashFlowService.get();

        assertEquals(1L, account.getId());
        assertEquals(new BigDecimal("0.00"), account.getInvestmentAmount());
        assertEquals(new BigDecimal("0.00"), account.getFinancialBalance());
    }

    @Test
    void createsLoanAtomicallyAndAdvancesCollectedInstallment() {
        Customer customer = customerService.create(new CustomerRequest("Ada Lovelace", "5551234", "Main St", null));
        CashFlow cashFlow = cashFlowService.lock();
        cashFlow.setInvestmentAmount(new BigDecimal("1000.00"));
        cashFlow.setFinancialBalance(new BigDecimal("1000.00"));
        LocalDate start = LocalDate.of(2025, 1, 31);

        Loan loan = loanService.create(new LoanRequest(customer.getId(), LoanType.INSTALLMENT_BASED,
                new BigDecimal("100"), new BigDecimal("10"), RepaymentFrequency.MONTHLY, null, start,
                new BigDecimal("30")));

        assertEquals("Ada-Lovelace-31012025", loan.getId());
        assertEquals(4, loan.getInstallmentCount());
        assertEquals(LocalDate.of(2025, 2, 28), loan.getNextInstallmentDate());
        assertEquals(new BigDecimal("900.00"), cashFlowService.get().getFinancialBalance());
        assertEquals(new BigDecimal("10.00"), cashFlowService.get().getProfitAmount());
        assertEquals(1, transactionRepository.count());

        collectionService.collect(new CollectionRequest(loan.getId(), LocalDate.of(2025, 2, 28),
                new BigDecimal("30"), TransactionType.COLLECTED));
        assertEquals(new BigDecimal("30.00"), cashFlowService.get().getCollectionAmount());
        assertEquals(LocalDate.of(2025, 3, 31), loanRepository.findById(loan.getId()).orElseThrow().getNextInstallmentDate());
        assertEquals(2, transactionRepository.count());
    }

    @Test
    void preservesUnicodeCustomerNamesInLoansAndJson() throws Exception {
        String customerName = "மாலதி";
        Customer customer = customerService.create(new CustomerRequest(customerName, null, null, null));
        assertEquals(customerName, customer.getName());
        assertTrue(objectMapper.writeValueAsString(CustomerResponse.from(customer)).contains(customerName));

        cashFlowService.lock().setFinancialBalance(new BigDecimal("100.00"));
        Loan loan = loanService.create(new LoanRequest(customer.getId(), LoanType.INSTALLMENT_BASED,
                BigDecimal.TEN, BigDecimal.ZERO, RepaymentFrequency.DAILY, null,
                LocalDate.of(2025, 1, 1), BigDecimal.ONE));

        assertEquals(customerName + "-01012025", loan.getId());
        assertEquals(customerName, LoanResponse.from(loan).customerName());
    }

    @Test
    void validatesWeeklyDayAndCustomerReference() {
        Customer customer = customerService.create(new CustomerRequest("Grace", "5551111", "Oak", null));
        LoanRequest invalidDay = new LoanRequest(customer.getId(), LoanType.MONTHLY_INTEREST,
                BigDecimal.TEN, BigDecimal.ONE, RepaymentFrequency.WEEKLY, "FUNDAY",
                LocalDate.of(2025, 1, 1), BigDecimal.ONE);
        assertThrows(BadRequestException.class, () -> loanService.create(invalidDay));
        assertThrows(com.example.financemgmt.exception.NotFoundException.class, () -> loanService.create(new LoanRequest(999999L,
                LoanType.MONTHLY_INTEREST, BigDecimal.TEN, BigDecimal.ONE, RepaymentFrequency.DAILY, null,
                LocalDate.of(2025, 1, 1), BigDecimal.ONE)));
    }

    @Test
    void rejectsNonPositiveLoanAmountsAndInsufficientTransfers() {
        LoanRequest invalidAmount = new LoanRequest(1L, LoanType.INSTALLMENT_BASED,
                BigDecimal.ZERO, BigDecimal.ONE, RepaymentFrequency.DAILY, null,
                LocalDate.of(2025, 1, 1), BigDecimal.ONE);
        assertTrue(validator.validate(invalidAmount).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("principalAmount")));

        assertThrows(ConflictException.class, () -> cashFlowService.transfer(
                new TransferRequest(TransferSource.PROFIT, BigDecimal.ONE, LocalDate.of(2025, 1, 1))));
    }

    @Test
    void customerOptionalFieldsAndZeroInterestAreValid() {
        CustomerRequest nameOnly = new CustomerRequest("Only Name", null, "", null);
        assertTrue(validator.validate(nameOnly).isEmpty());
        Customer customer = customerService.create(nameOnly);
        assertNull(customer.getMobileNumber());
        assertNull(customer.getAddress());

        LoanRequest zeroInterestLoan = new LoanRequest(customer.getId(), LoanType.INSTALLMENT_BASED,
                BigDecimal.TEN, BigDecimal.ZERO, RepaymentFrequency.DAILY, null,
                LocalDate.of(2025, 1, 1), BigDecimal.ONE);
        assertTrue(validator.validate(zeroInterestLoan).isEmpty());
        CashFlow account = cashFlowService.lock();
        account.setFinancialBalance(new BigDecimal("100.00"));
        Loan loan = loanService.create(zeroInterestLoan);
        assertEquals(new BigDecimal("0.00"), cashFlowService.get().getProfitAmount());
        assertEquals(1, transactionRepository.findAll().size());
        assertEquals("PROFIT", transactionRepository.findAll().getFirst().getTransactionType());
        assertEquals(new BigDecimal("0.00"), transactionRepository.findAll().getFirst().getAmount());
    }

    @Test
    void acceptsFrontendFromAccountTransferContractAndLegacyAlias() throws Exception {
        TransferRequest request = objectMapper.readValue(
                """
                {"fromAccount":"INVESTMENT","amount":500,"date":"2025-01-01"}
                """, TransferRequest.class);
        assertEquals(TransferSource.INVESTMENT, request.fromAccount());
        TransferRequest legacy = objectMapper.readValue(
                """
                {"source":"PROFIT","amount":1,"date":"2025-01-01"}
                """, TransferRequest.class);
        assertEquals(TransferSource.PROFIT, legacy.fromAccount());
    }
}
