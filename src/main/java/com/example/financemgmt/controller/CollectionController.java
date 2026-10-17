package com.example.financemgmt.controller;

import com.example.financemgmt.dto.CollectionRequest;
import com.example.financemgmt.dto.FinancialTransactionResponse;
import com.example.financemgmt.service.CashFlowService;
import com.example.financemgmt.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {
    private final CollectionService service;
    private final CashFlowService cashFlowService;
    public CollectionController(CollectionService service, CashFlowService cashFlowService) {
        this.service = service;
        this.cashFlowService = cashFlowService;
    }

    @GetMapping("/history")
    public java.util.List<FinancialTransactionResponse> history() {
        return cashFlowService.transactionHistory();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionRequest create(@Valid @RequestBody CollectionRequest request) {
        service.collect(request);
        return request;
    }
}
