package com.boutique.sale;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByCustomerIdOrderBySaleDateDesc(Long customerId);
}
