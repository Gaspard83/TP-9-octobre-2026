package com.boutique.customer.dto;

import com.boutique.customer.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Données modifiables d'un client (création et mise à jour). */
public record CustomerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @Pattern(regexp = Customer.PHONE_REGEX, message = "doit contenir 10 à 15 chiffres, avec un + optionnel")
        String phoneNumber,
        @Size(max = 255) String address) {
}