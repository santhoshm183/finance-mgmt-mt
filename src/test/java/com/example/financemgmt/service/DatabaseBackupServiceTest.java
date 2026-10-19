package com.example.financemgmt.service;

import com.example.financemgmt.dto.CustomerRequest;
import com.example.financemgmt.dto.CollectionRequest;
import com.example.financemgmt.dto.TransferRequest;
import com.example.financemgmt.model.LoanType;
import com.example.financemgmt.model.RepaymentFrequency;
import com.example.financemgmt.model.TransferSource;
import com.example.financemgmt.dto.LoanRequest;
import com.example.financemgmt.model.TransactionType;
import com.example.financemgmt.repository.CashTransferRepository;
import com.example.financemgmt.repository.CustomerRepository;
import com.example.financemgmt.repository.FinancialTransactionRepository;
import com.example.financemgmt.repository.LoanRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DatabaseBackupServiceTest {
    @Autowired DatabaseBackupService backupService;
    @Autowired CustomerService customerService;
    @Autowired LoanService loanService;
    @Autowired CollectionService collectionService;
    @Autowired CashFlowService cashFlowService;
    @Autowired CustomerRepository customerRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired CashTransferRepository transferRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void exportsAndRestoresSnapshotAndContinuesIdentitySequence() {
        var original = customerService.create(new CustomerRequest("Backup Customer", "5550101", "Main Street", null));
        cashFlowService.lock().setFinancialBalance(new BigDecimal("100.00"));
        customerRepository.flush();
        var loan = loanService.create(new LoanRequest(original.getId(), LoanType.INSTALLMENT_BASED,
                new BigDecimal("10.00"), new BigDecimal("10.00"), RepaymentFrequency.MONTHLY, null,
                LocalDate.of(2025, 1, 1), new BigDecimal("2.00")));
        collectionService.collect(new CollectionRequest(loan.getId(), loan.getNextInstallmentDate(),
                new BigDecimal("2.00"), TransactionType.COLLECTED));
        cashFlowService.transfer(new TransferRequest(TransferSource.INVESTMENT,
                BigDecimal.ONE, LocalDate.of(2025, 1, 3)));
        byte[] backup = backupService.exportSql();
        String sql = new String(backup, java.nio.charset.StandardCharsets.UTF_8);
        long previousTransactionId = transactionRepository.findAll().stream()
                .mapToLong(transaction -> transaction.getId()).max().orElseThrow();
        long previousTransferId = transferRepository.findAll().stream()
                .mapToLong(transfer -> transfer.getId()).max().orElseThrow();

        assertTrue(sql.startsWith("-- Ledgerly Finance Management database backup v1"));
        assertTrue(sql.contains("INSERT INTO finance_mgmt.customers"));

        customerService.create(new CustomerRequest("Post Backup Customer", null, null, null));
        customerRepository.flush();
        backupService.importSql(backup);
        entityManager.clear();

        assertEquals(1, customerRepository.count());
        assertEquals(1, loanRepository.count());
        assertEquals(2, transactionRepository.count());
        assertEquals(1, transferRepository.count());
        assertNotNull(customerRepository.findById(original.getId()).orElse(null));
        assertEquals(1L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM finance_mgmt.cash_flow WHERE id = 1", Long.class));

        var afterRestore = customerService.create(new CustomerRequest("After Restore Customer", null, null, null));
        assertTrue(afterRestore.getId() > original.getId());

        var restoredLoan = loanRepository.findById(loan.getId()).orElseThrow();
        collectionService.collect(new CollectionRequest(restoredLoan.getId(), restoredLoan.getNextInstallmentDate(),
                BigDecimal.ONE, TransactionType.COLLECTED));
        cashFlowService.transfer(new TransferRequest(TransferSource.INVESTMENT,
                BigDecimal.ONE, LocalDate.of(2025, 1, 4)));
        assertEquals(previousTransactionId + 1,
                jdbcTemplate.queryForObject("SELECT MAX(id) FROM finance_mgmt.financial_transactions", Long.class));
        assertEquals(previousTransferId + 1,
                jdbcTemplate.queryForObject("SELECT MAX(id) FROM finance_mgmt.cash_transfers", Long.class));
    }

    @Test
    void rejectsNonApplicationSqlWithoutChangingDatabase() {
        customerService.create(new CustomerRequest("Keep Me", null, null, null));
        customerRepository.flush();

        assertThrows(com.example.financemgmt.exception.BadRequestException.class,
                () -> backupService.importSql("DROP TABLE finance_mgmt.customers;".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(1L, customerRepository.count());
    }
}
