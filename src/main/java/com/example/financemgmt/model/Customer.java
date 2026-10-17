package com.example.financemgmt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "mobile_number", length = 40)
    private String mobileNumber;
    @Column(length = 500)
    private String address;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, length = 30)
    private String status;

    protected Customer() {}

    public Customer(String name, String mobileNumber, String address) {
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getMobileNumber() { return mobileNumber; }
    public String getAddress() { return address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public void update(String name, String mobileNumber, String address, String status) {
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.status = status;
    }
}
