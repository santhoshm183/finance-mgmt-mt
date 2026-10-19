package com.example.financemgmt.controller;

import com.example.financemgmt.dto.PendingPaymentResponse;
import com.example.financemgmt.service.LoanService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final LoanService service;
    public PaymentController(LoanService service) { this.service = service; }

    @GetMapping("/pending")
    public List<PendingPaymentResponse> pending() {
        return service.pending(LocalDate.now());
    }
}
