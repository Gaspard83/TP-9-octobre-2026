package com.boutique.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.dto.CustomerCreateRequest;
import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;
import com.boutique.security.CurrentUserService;

@DisplayName("CustomerService")
class CustomerServiceTests {

    private CustomerRepository repository;
    private CustomerMapper mapper;
    private PasswordEncoder passwordEncoder;
    private CurrentUserService currentUser;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        mapper = mock(CustomerMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        currentUser = mock(CurrentUserService.class);
        service = new CustomerService(repository, mapper, passwordEncoder, currentUser);
    }

    @Test
    @DisplayName("findAll retourne les clients mappes dans l'ordre")
    void findAllMapsEveryCustomerInOrder() {
        Customer first = customer(1L);
        Customer second = customer(2L);
        CustomerResponse firstResponse = response(1L);
        CustomerResponse secondResponse = response(2L);
        when(repository.findAll()).thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), service.findAll());

        InOrder order = inOrder(repository, mapper);
        order.verify(repository).findAll();
        order.verify(mapper).toResponse(first);
        order.verify(mapper).toResponse(second);
    }

    @Test
    @DisplayName("findAll accepte une liste vide")
    void findAllReturnsAnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), service.findAll());

        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("findById verifie l'acces avant de charger et mapper le client")
    void findByIdChecksAccessThenReturnsTheMappedCustomer() {
        Customer customer = customer(12L);
        CustomerResponse expected = response(12L);
        when(repository.findById(12L)).thenReturn(Optional.of(customer));
        when(mapper.toResponse(customer)).thenReturn(expected);

        assertSame(expected, service.findById(12L));

        InOrder order = inOrder(currentUser, repository, mapper);
        order.verify(currentUser).checkAccessTo(12L);
        order.verify(repository).findById(12L);
        order.verify(mapper).toResponse(customer);
    }

    @Test
    @DisplayName("findById ne charge pas le client si le controle d'acces echoue")
    void findByIdPropagatesAccessDenial() {
        AccessDeniedException failure = new AccessDeniedException("denied");
        org.mockito.Mockito.doThrow(failure).when(currentUser).checkAccessTo(12L);

        assertSame(failure, assertThrows(AccessDeniedException.class, () -> service.findById(12L)));

        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("findCurrent charge le client de l'utilisateur connecte")
    void findCurrentReturnsTheAuthenticatedCustomer() {
        Customer customer = customer(7L);
        CustomerResponse expected = response(7L);
        when(currentUser.currentCustomerId()).thenReturn(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(customer));
        when(mapper.toResponse(customer)).thenReturn(expected);

        assertSame(expected, service.findCurrent());

        verify(currentUser).currentCustomerId();
        verify(repository).findById(7L);
        verify(mapper).toResponse(customer);
    }

    @Test
    @DisplayName("create applique le role CUSTOMER par defaut et encode le mot de passe")
    void createDefaultsRoleAndEncodesPassword() {
        CustomerCreateRequest request = createRequest(null);
        CustomerRequest customerRequest = request.toCustomerRequest();
        Customer entity = customer(21L);
        CustomerResponse expected = response(21L);
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret-pass")).thenReturn("encoded-pass");
        when(mapper.toEntity(customerRequest, "encoded-pass", Role.CUSTOMER)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(expected);

        assertSame(expected, service.create(request));

        verify(passwordEncoder).encode("secret-pass");
        verify(repository).save(entity);
        verify(mapper).toEntity(customerRequest, "encoded-pass", Role.CUSTOMER);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("create conserve le role explicitement fourni")
    void createUsesExplicitRole(Role role) {
        CustomerCreateRequest request = createRequest(role);
        CustomerRequest customerRequest = request.toCustomerRequest();
        Customer entity = customer(22L);
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret-pass")).thenReturn("encoded-pass");
        when(mapper.toEntity(customerRequest, "encoded-pass", role)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response(22L));

        assertEquals(response(22L), service.create(request));

        verify(mapper).toEntity(customerRequest, "encoded-pass", role);
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 72})
    @DisplayName("create transmet les mots de passe aux bornes et accepte les champs facultatifs absents")
    void createAcceptsPasswordBoundariesAndMissingOptionalFields(int passwordLength) {
        String password = "p".repeat(passwordLength);
        CustomerCreateRequest request = new CustomerCreateRequest("Ada", "Lovelace",
                "ada@example.com", null, null, password, null);
        CustomerMapper realMapper = new CustomerMapper();
        CustomerService serviceWithRealMapper =
                new CustomerService(repository, realMapper, passwordEncoder, currentUser);
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded-pass");
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer entity = invocation.getArgument(0);
            assertEquals("encoded-pass", entity.getPassword());
            assertEquals(Role.CUSTOMER, entity.getRole());
            return entity;
        });

        assertEquals(new CustomerResponse(null, "Ada", "Lovelace", "ada@example.com",
                null, null, Role.CUSTOMER), serviceWithRealMapper.create(request));

        verify(passwordEncoder).encode(password);
        verify(repository).save(any(Customer.class));
    }

    @Test
    @DisplayName("register normalise l'email avant verification et persiste le mot de passe encode")
    void registerNormalizesEmailAndSavesEncodedPassword() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", " ADA@Example.com ",
                "+33123456789", "Paris");
        Customer entity = customer(null);
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret-pass")).thenReturn("encoded-pass");
        when(mapper.toEntity(request, "encoded-pass", Role.CUSTOMER)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        assertSame(entity, service.register(request, "secret-pass", Role.CUSTOMER));

        verify(repository).findByEmail("ada@example.com");
        verify(passwordEncoder).encode("secret-pass");
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("register refuse une adresse email deja utilisee")
    void registerRejectsDuplicateEmail() {
        Customer existing = customer(3L);
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));
        CustomerRequest request = request("ada@example.com");

        assertThrows(BusinessException.class, () -> service.register(request, "password", Role.CUSTOMER));

        verifyNoInteractions(passwordEncoder, mapper);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update autorise le maintien de l'email du client")
    void updateAllowsTheCustomersCurrentEmail() {
        Customer customer = customer(8L);
        CustomerRequest request = request("ADA@example.com");
        CustomerResponse expected = response(8L);
        when(repository.findById(8L)).thenReturn(Optional.of(customer));
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.of(customer));
        when(mapper.toResponse(customer)).thenReturn(expected);

        assertSame(expected, service.update(8L, request));

        InOrder order = inOrder(currentUser, repository, mapper);
        order.verify(currentUser).checkAccessTo(8L);
        order.verify(repository).findById(8L);
        order.verify(repository).findByEmail("ada@example.com");
        order.verify(mapper).update(customer, request);
        order.verify(mapper).toResponse(customer);
    }

    @Test
    @DisplayName("update accepte une nouvelle adresse email libre")
    void updateAllowsAnUnusedEmail() {
        Customer customer = customer(8L);
        CustomerRequest request = request(" NEW@example.com ");
        CustomerResponse expected = new CustomerResponse(8L, "Ada", "Lovelace",
                "new@example.com", "+33123456789", "Paris", Role.CUSTOMER);
        when(repository.findById(8L)).thenReturn(Optional.of(customer));
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(mapper.toResponse(customer)).thenReturn(expected);

        assertSame(expected, service.update(8L, request));

        InOrder order = inOrder(currentUser, repository, mapper);
        order.verify(currentUser).checkAccessTo(8L);
        order.verify(repository).findById(8L);
        order.verify(repository).findByEmail("new@example.com");
        order.verify(mapper).update(customer, request);
        order.verify(mapper).toResponse(customer);
    }

    @Test
    @DisplayName("update refuse l'email d'un autre client")
    void updateRejectsEmailUsedByAnotherCustomer() {
        Customer current = customer(8L);
        Customer existing = customer(9L);
        when(repository.findById(8L)).thenReturn(Optional.of(current));
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> service.update(8L, request("ada@example.com")));

        verify(mapper, never()).update(any(), any());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("update ne charge pas le client si le controle d'acces echoue")
    void updatePropagatesAccessDenial() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(currentUser).checkAccessTo(8L);

        assertThrows(AccessDeniedException.class, () -> service.update(8L, request("ada@example.com")));

        verifyNoInteractions(repository, mapper);
    }

    @Test
    @DisplayName("delete supprime le client puis vide le contexte de persistance")
    void deleteRemovesTheCustomerAndFlushes() {
        Customer customer = customer(15L);
        when(repository.findById(15L)).thenReturn(Optional.of(customer));

        service.delete(15L);

        InOrder order = inOrder(repository);
        order.verify(repository).findById(15L);
        order.verify(repository).delete(customer);
        order.verify(repository).flush();
    }

    @Test
    @DisplayName("getEntity retourne le client trouve")
    void getEntityReturnsTheFoundCustomer() {
        Customer expected = customer(4L);
        when(repository.findById(4L)).thenReturn(Optional.of(expected));

        assertSame(expected, service.getEntity(4L));
    }

    @Test
    @DisplayName("getEntity et les operations de lecture propagent l'absence de client")
    void missingCustomerRaisesResourceNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException failure =
                assertThrows(ResourceNotFoundException.class, () -> service.getEntity(404L));

        assertEquals("Client introuvable avec l'id 404", failure.getMessage());
    }

    private Customer customer(Long id) {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(id);
        return customer;
    }

    private CustomerRequest request(String email) {
        return new CustomerRequest("Ada", "Lovelace", email, "+33123456789", "Paris");
    }

    private CustomerCreateRequest createRequest(Role role) {
        return new CustomerCreateRequest("Ada", "Lovelace", "ada@example.com", "+33123456789",
                "Paris", "secret-pass", role);
    }

    private CustomerResponse response(Long id) {
        return new CustomerResponse(id, "Ada", "Lovelace", "ada@example.com",
                "+33123456789", "Paris", Role.CUSTOMER);
    }
}
