package com.boutique.customer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.dto.CustomerCreateRequest;
import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;
import com.boutique.security.CurrentUserService;

@Service
@Transactional
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;

    public CustomerService(CustomerRepository repository, CustomerMapper mapper,
                           PasswordEncoder passwordEncoder, CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        log.info("Entrée {}", "findAll");
        List<CustomerResponse> customers = repository.findAll().stream().map(mapper::toResponse).toList();
        log.debug("Clients récupérés : count={}", customers.size());
        log.info("Sortie {} : count={}", "findAll", customers.size());
        return customers;
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        log.info("Entrée {} : id={}", "findById", id);
        currentUser.checkAccessTo(id);
        CustomerResponse response = mapper.toResponse(getEntity(id));
        log.info("Sortie {} : id={}", "findById", id);
        return response;
    }

    @Transactional(readOnly = true)
    public CustomerResponse findCurrent() {
        log.info("Entrée {}", "findCurrent");
        Long customerId = currentUser.currentCustomerId();
        CustomerResponse response = mapper.toResponse(getEntity(customerId));
        log.info("Sortie {} : id={}", "findCurrent", customerId);
        return response;
    }

    /** Création par un ADMIN (POST /api/customers). */
    public CustomerResponse create(CustomerCreateRequest request) {
        log.info("Entrée {} : role={}", "create", request.role());
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (request.role() == null) {
            log.debug("Rôle par défaut appliqué : role={}", role);
        }
        Customer customer = register(request.toCustomerRequest(), request.password(), role);
        CustomerResponse response = mapper.toResponse(customer);
        log.info("Sortie {} : id={}", "create", customer.getId());
        return response;
    }

    /** Crée un compte client avec mot de passe haché (création ADMIN et inscription publique). */
    public Customer register(CustomerRequest request, String rawPassword, Role role) {
        log.info("Entrée {} : role={}", "register", role);
        ensureEmailAvailable(request.email(), null);
        Customer customer = repository.save(mapper.toEntity(request, passwordEncoder.encode(rawPassword), role));
        log.debug("Compte client créé : id={}, role={}", customer.getId(), role);
        log.info("Sortie {} : id={}", "register", customer.getId());
        return customer;
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        log.info("Entrée {} : id={}", "update", id);
        currentUser.checkAccessTo(id);
        Customer customer = getEntity(id);
        ensureEmailAvailable(request.email(), id);
        mapper.update(customer, request);
        CustomerResponse response = mapper.toResponse(customer);
        log.debug("Client mis à jour : id={}", id);
        log.info("Sortie {} : id={}", "update", id);
        return response;
    }

    public void delete(Long id) {
        log.info("Entrée {} : id={}", "delete", id);
        repository.delete(getEntity(id));
        repository.flush();
        log.debug("Client supprimé : id={}", id);
        log.info("Sortie {} : id={}", "delete", id);
    }

    @Transactional(readOnly = true)
    public Customer getEntity(Long id) {
        log.info("Entrée {} : id={}", "getEntity", id);
        Customer customer = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Client introuvable : id={}", id);
                    return new ResourceNotFoundException("Client", id);
                });
        log.debug("Client récupéré : id={}", id);
        log.info("Sortie {} : id={}", "getEntity", id);
        return customer;
    }

    /** Lève une 409 si l'email appartient déjà à un autre client que {@code currentId}. */
    private void ensureEmailAvailable(String email, Long currentId) {
        log.debug("Vérification de disponibilité de l'email : currentId={}", currentId);
        repository.findByEmail(CustomerMapper.normalizeEmail(email))
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    log.warn("Adresse email déjà utilisée par un autre client : currentId={}, existingId={}",
                            currentId, existing.getId());
                    throw new BusinessException("L'email '" + email + "' est déjà utilisé");
                });
    }
}