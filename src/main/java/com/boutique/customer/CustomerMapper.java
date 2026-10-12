package com.boutique.customer;

import org.springframework.stereotype.Component;

import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request, String encodedPassword, Role role) {
        return new Customer(request.firstName().trim(), request.lastName().trim(), normalizeEmail(request.email()),
                request.phoneNumber(), request.address(), encodedPassword, role);
    }

    public void update(Customer customer, CustomerRequest request) {
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(normalizeEmail(request.email()));
        customer.setPhoneNumber(request.phoneNumber());
        customer.setAddress(request.address());
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(),
                customer.getEmail(), customer.getPhoneNumber(), customer.getAddress(), customer.getRole());
    }

    /** Les emails sont stockés en minuscules pour garantir l'unicité sans tenir compte de la casse. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}