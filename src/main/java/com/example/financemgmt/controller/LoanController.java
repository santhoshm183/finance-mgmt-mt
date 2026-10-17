package com.example.financemgmt.controller;

import com.example.financemgmt.dto.LoanRequest;
import com.example.financemgmt.dto.LoanResponse;
import com.example.financemgmt.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;
    public LoanController(LoanService service) { this.service = service; }

    @GetMapping
    public List<LoanResponse> getLoans(@RequestParam(required = false) Long customerId) {
        return service.findAll(customerId).stream().map(LoanResponse::from).toList();
    }
    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable String id) { return LoanResponse.from(service.findById(id)); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse create(@Valid @RequestBody LoanRequest request) {
        return LoanResponse.from(service.create(request));
    }
}
