package com.boutique.security.dto;

import com.boutique.customer.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email String email,
        @Pattern(regexp = Customer.PHONE_REGEX, message = "doit contenir 10 à 15 chiffres, avec un + optionnel")
        String phoneNumber,
        @Size(max = 255) String address,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
