package com.example.financemgmt.repository;

import com.example.financemgmt.model.CashTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CashTransferRepository extends JpaRepository<CashTransfer, Long> {
    List<CashTransfer> findAllByOrderByTransferDateDescIdDesc();
}
