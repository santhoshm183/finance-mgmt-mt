package com.example.financemgmt.repository;

import com.example.financemgmt.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByNormalizedName(String normalizedName);
    boolean existsByNormalizedNameAndIdNot(String normalizedName, Long id);

    @Query("select c from Customer c where lower(c.name) like lower(concat('%', :search, '%')) " +
           "or lower(c.mobileNumber) like lower(concat('%', :search, '%')) " +
           "or lower(c.address) like lower(concat('%', :search, '%')) order by c.id")
    List<Customer> search(@Param("search") String search);
}
