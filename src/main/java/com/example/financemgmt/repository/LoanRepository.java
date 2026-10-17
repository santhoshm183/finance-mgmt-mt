package com.example.financemgmt.repository;

import com.example.financemgmt.model.Loan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, String> {
    @Override
    @EntityGraph(attributePaths = "customer")
    List<Loan> findAll();

    @Override
    @EntityGraph(attributePaths = "customer")
    Optional<Loan> findById(String id);

    @EntityGraph(attributePaths = "customer")
    List<Loan> findByCustomer_IdOrderByStartDateDesc(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findLockedById(@Param("id") String id);

    @EntityGraph(attributePaths = "customer")
    List<Loan> findByNextInstallmentDateLessThanEqualOrderByNextInstallmentDateAsc(LocalDate date);
}
