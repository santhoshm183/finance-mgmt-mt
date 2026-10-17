package com.example.financemgmt.controller;

import com.example.financemgmt.dto.CustomerRequest;
import com.example.financemgmt.dto.CustomerResponse;
import com.example.financemgmt.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }

    @GetMapping
    public List<CustomerResponse> getCustomers(@RequestParam(required = false) String search) {
        return service.findAll(search).stream().map(CustomerResponse::from).toList();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(service.create(request));
    }
    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) { return CustomerResponse.from(service.findById(id)); }
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(service.update(id, request));
    }
}
