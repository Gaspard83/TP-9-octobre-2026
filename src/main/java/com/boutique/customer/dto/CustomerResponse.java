package com.boutique.customer.dto;

import com.boutique.customer.Role;

public record CustomerResponse(Long id, String firstName, String lastName, String email, String phoneNumber,
                               String address, Role role) {
}