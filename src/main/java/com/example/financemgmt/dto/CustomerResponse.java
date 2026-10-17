package com.example.financemgmt.dto;

import com.example.financemgmt.model.Customer;
import java.time.LocalDateTime;

public record CustomerResponse(Long id, String name, String mobileNumber, String address,
                               LocalDateTime createdAt, String status) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getMobileNumber(),
                customer.getAddress(), customer.getCreatedAt(), customer.getStatus());
    }
}
