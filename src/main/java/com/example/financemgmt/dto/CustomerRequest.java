package com.example.financemgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 40) String mobileNumber,
        @Size(max = 500) String address,
        @Size(max = 30) String status) {}
