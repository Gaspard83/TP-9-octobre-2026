package com.boutique.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.customer.Customer;
import com.boutique.customer.CustomerMapper;
import com.boutique.customer.CustomerRepository;
import com.boutique.customer.CustomerService;
import com.boutique.customer.Role;
import com.boutique.customer.dto.CustomerRequest;
import com.boutique.security.dto.AuthResponse;
import com.boutique.security.dto.LoginRequest;
import com.boutique.security.dto.RegisterRequest;

@Service
public class AuthService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(CustomerService customerService, CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Inscription publique : le rôle est toujours CUSTOMER. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        CustomerRequest customer = new CustomerRequest(request.firstName(), request.lastName(),
                request.email(), request.phoneNumber(), request.address());
        Customer saved = customerService.register(customer, request.password(), Role.CUSTOMER);
        return issueToken(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(CustomerMapper.normalizeEmail(request.email()))
                .filter(c -> passwordEncoder.matches(request.password(), c.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));
        return issueToken(customer);
    }

    private AuthResponse issueToken(Customer customer) {
        return AuthResponse.bearer(jwtService.generateToken(customer), jwtService.expirationInSeconds());
    }
}
