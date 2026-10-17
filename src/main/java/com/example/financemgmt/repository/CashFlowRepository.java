package com.example.financemgmt.repository;

import com.example.financemgmt.model.CashFlow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashFlow c where c.id = 1")
    Optional<CashFlow> lockSingleton();
}
