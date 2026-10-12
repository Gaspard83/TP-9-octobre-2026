package com.boutique.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.boutique.customer.CustomerRepository;
import com.boutique.customer.CustomerService;
import com.boutique.customer.Role;
import com.boutique.customer.dto.CustomerRequest;

/**
 * Comptes de démonstration (mots de passe hachés au démarrage, d'où l'absence dans data.sql).
 * À désactiver en production : app.demo-data.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

    public DemoDataInitializer(CustomerService customerService, CustomerRepository customerRepository) {
        this.customerService = customerService;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.count() > 0) {
            return;
        }
        customerService.register(new CustomerRequest("Alice", "Martin", "alice@example.com", "0601020304", "1 rue de Paris"),
                "alice1234", Role.CUSTOMER);
        customerService.register(new CustomerRequest("Bob", "Durand", "bob@example.com", "0605060708", "2 avenue de Lyon"),
                "bob12345", Role.CUSTOMER);
        customerService.register(new CustomerRequest("Admin", "Boutique", "admin@boutique.com", null, null),
                "admin1234", Role.ADMIN);
    }
}
