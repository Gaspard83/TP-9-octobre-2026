package com.boutique.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;

@DisplayName("CustomerMapper")
class CustomerMapperTests {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    @DisplayName("toEntity nettoie les noms et normalise l'email")
    void toEntityTrimsNamesAndNormalizesEmail() {
        CustomerRequest request = new CustomerRequest(" Ada ", " Lovelace ", " ADA@Example.COM ",
                "+33123456789", "Paris");

        Customer customer = mapper.toEntity(request, "encoded-password", Role.CUSTOMER);

        assertEquals("Ada", customer.getFirstName());
        assertEquals("Lovelace", customer.getLastName());
        assertEquals("ada@example.com", customer.getEmail());
        assertEquals("+33123456789", customer.getPhoneNumber());
        assertEquals("Paris", customer.getAddress());
        assertEquals("encoded-password", customer.getPassword());
        assertEquals(Role.CUSTOMER, customer.getRole());
    }

    @Test
    @DisplayName("update remplace les donnees modifiables sans toucher au mot de passe ni au role")
    void updateChangesProfileButKeepsCredentialsAndRole() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com",
                "+33123456789", "Paris", "encoded-password", Role.ADMIN);
        CustomerRequest request = new CustomerRequest(" Grace ", " Hopper ", " GRACE@Example.COM ",
                "+12025550123", "New York");

        mapper.update(customer, request);

        assertEquals("Grace", customer.getFirstName());
        assertEquals("Hopper", customer.getLastName());
        assertEquals("grace@example.com", customer.getEmail());
        assertEquals("+12025550123", customer.getPhoneNumber());
        assertEquals("New York", customer.getAddress());
        assertEquals("encoded-password", customer.getPassword());
        assertEquals(Role.ADMIN, customer.getRole());
    }

    @Test
    @DisplayName("toResponse expose les champs du profil et le role")
    void toResponseMapsCustomerFields() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com",
                "+33123456789", "Paris", "encoded-password", Role.CUSTOMER);
        CustomerResponse response = mapper.toResponse(customer);

        assertNull(response.id());
        assertEquals("Ada", response.firstName());
        assertEquals("Lovelace", response.lastName());
        assertEquals("ada@example.com", response.email());
        assertEquals("+33123456789", response.phoneNumber());
        assertEquals("Paris", response.address());
        assertEquals(Role.CUSTOMER, response.role());
    }

    @Test
    @DisplayName("normalizeEmail gere une valeur nulle")
    void normalizeEmailPreservesNull() {
        assertNull(CustomerMapper.normalizeEmail(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("toEntity et toResponse conservent les champs facultatifs absents ou vides")
    void mapsOptionalFieldsWithoutInventingDefaults(String address) {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace",
                "ada@example.com", null, address);

        Customer customer = mapper.toEntity(request, "encoded-password", Role.ADMIN);
        CustomerResponse response = mapper.toResponse(customer);

        assertNull(customer.getPhoneNumber());
        assertEquals(address, customer.getAddress());
        assertEquals("encoded-password", customer.getPassword());
        assertEquals(new CustomerResponse(null, "Ada", "Lovelace", "ada@example.com",
                null, address, Role.ADMIN), response);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("update permet d'effacer le telephone et l'adresse sans changer les identifiants")
    void updateClearsOptionalFields(String address) {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com",
                "+33123456789", "Paris", "encoded-password", Role.ADMIN);
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace",
                "ada@example.com", null, address);

        mapper.update(customer, request);

        assertNull(customer.getPhoneNumber());
        assertEquals(address, customer.getAddress());
        assertEquals("encoded-password", customer.getPassword());
        assertEquals(Role.ADMIN, customer.getRole());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "ADA@Example.COM|ada@example.com",
            "  ADA@Example.COM  |ada@example.com",
            "ada@example.com|ada@example.com",
            "''|''",
            "'   '|''"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    @DisplayName("normalizeEmail traite les emails nominaux et les chaines vides ou blanches")
    void normalizeEmailHandlesNominalAndBoundaryValues(String email, String expected) {
        assertEquals(expected, CustomerMapper.normalizeEmail(email));
    }
}
