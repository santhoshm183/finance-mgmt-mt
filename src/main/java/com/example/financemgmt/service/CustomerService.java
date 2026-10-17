package com.example.financemgmt.service;

import com.example.financemgmt.dto.CustomerRequest;
import com.example.financemgmt.exception.NotFoundException;
import com.example.financemgmt.model.Customer;
import com.example.financemgmt.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CustomerService {
    private final CustomerRepository repository;
    public CustomerService(CustomerRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<Customer> findAll(String search) {
        return search == null || search.isBlank() ? repository.findAll() : repository.search(search.trim());
    }
    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " was not found"));
    }
    public Customer create(CustomerRequest request) {
        return repository.save(new Customer(request.name().trim(), cleanOptional(request.mobileNumber()), cleanOptional(request.address())));
    }
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        customer.update(request.name().trim(), cleanOptional(request.mobileNumber()), cleanOptional(request.address()),
                request.status() == null || request.status().isBlank() ? customer.getStatus() : request.status().trim());
        return customer;
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
