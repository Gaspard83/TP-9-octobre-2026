package com.boutique.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.boutique.customer.dto.CustomerCreateRequest;
import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("Validation Customer : cas nominaux et limites valides")
class CustomerValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validProfiles")
    @DisplayName("CustomerRequest accepte les profils nominaux et aux bornes")
    void requestAcceptsValidProfiles(String scenario, CustomerRequest request) {
        assertValid(request);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validProfiles")
    @DisplayName("CustomerCreateRequest accepte les memes profils et les convertit sans perte")
    void createRequestAcceptsAndConvertsValidProfiles(String scenario, CustomerRequest request) {
        CustomerCreateRequest creation = new CustomerCreateRequest(request.firstName(),
                request.lastName(), request.email(), request.phoneNumber(), request.address(),
                "password", null);

        assertValid(creation);
        assertEquals(request, creation.toCustomerRequest());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validProfiles")
    @DisplayName("Customer accepte les profils valides apres mapping")
    void entityAcceptsValidProfiles(String scenario, CustomerRequest request) {
        CustomerMapper mapper = new CustomerMapper();
        Customer customer = mapper.toEntity(request, "encoded-password", Role.CUSTOMER);

        assertValid(customer);
        assertEquals(new CustomerResponse(null, request.firstName(),
                request.lastName(), request.email(), request.phoneNumber(), request.address(),
                Role.CUSTOMER), mapper.toResponse(customer));
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    @DisplayName("CustomerCreateRequest accepte les bornes de longueur du mot de passe")
    void createRequestAcceptsPasswordBoundaries(int length) {
        CustomerCreateRequest request = new CustomerCreateRequest("Ada", "Lovelace",
                "ada@example.com", null, null, "p".repeat(length), Role.CUSTOMER);

        assertEquals(length, request.password().length());
        assertValid(request);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("CustomerCreateRequest et Customer acceptent chaque role defini")
    void acceptsExplicitRoles(Role role) {
        CustomerCreateRequest request = new CustomerCreateRequest("Ada", "Lovelace",
                "ada@example.com", null, null, "password", role);
        Customer customer = new CustomerMapper().toEntity(request.toCustomerRequest(),
                "encoded-password", role);

        assertValid(request);
        assertValid(customer);
        assertEquals(role, request.role());
        assertEquals(role, customer.getRole());
    }

    private static Stream<Arguments> validProfiles() {
        String maxEmail = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(21);
        return Stream.of(
                profile("profil nominal", "Ada", "Lovelace", "ada@example.com",
                        "+33123456789", "Paris"),
                profile("champs facultatifs absents", "Ada", "Lovelace",
                        "ada@example.com", null, null),
                profile("adresse vide", "Ada", "Lovelace", "ada@example.com", null, ""),
                profile("noms d'un caractere", "A", "L", "ada@example.com", null, null),
                profile("noms de 100 caracteres", "a".repeat(100), "b".repeat(100),
                        "ada@example.com", null, null),
                profile("email de 150 caracteres", "Ada", "Lovelace", maxEmail, null, null),
                profile("adresse de 255 caracteres", "Ada", "Lovelace",
                        "ada@example.com", null, "a".repeat(255)),
                profile("telephone de 10 chiffres", "Ada", "Lovelace",
                        "ada@example.com", "0123456789", null),
                profile("telephone de 15 chiffres", "Ada", "Lovelace",
                        "ada@example.com", "123456789012345", null),
                profile("telephone de 10 chiffres avec +", "Ada", "Lovelace",
                        "ada@example.com", "+0123456789", null),
                profile("telephone de 15 chiffres avec +", "Ada", "Lovelace",
                        "ada@example.com", "+123456789012345", null));
    }

    private static Arguments profile(String scenario, String firstName, String lastName,
                                     String email, String phoneNumber, String address) {
        return Arguments.of(scenario,
                new CustomerRequest(firstName, lastName, email, phoneNumber, address));
    }

    private <T> void assertValid(T value) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        assertTrue(violations.isEmpty(), () -> "Violations inattendues : " + violations);
    }
}
