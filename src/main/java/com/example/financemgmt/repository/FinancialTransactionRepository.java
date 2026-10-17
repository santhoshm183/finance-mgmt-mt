package com.example.financemgmt.repository;

import com.example.financemgmt.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    @Query("""
            select new com.example.financemgmt.dto.FinancialTransactionResponse(
                t.id, t.transactionType, t.amount, t.transactionDate, t.loanId, c.name
            )
            from FinancialTransaction t
            left join Loan l on l.id = t.loanId
            left join l.customer c
            order by t.transactionDate desc, t.id desc
            """)
    List<com.example.financemgmt.dto.FinancialTransactionResponse> findHistory();
}
