package com.boutique.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import com.boutique.category.CategoryService;
import com.boutique.product.dto.ProductResponse;

@DisplayName("Caracterisation du comportement actuel de ProductService.findAll")
class ProductServiceFindAllCharacterizationTests {

    private ProductRepository repository;
    private ProductMapper mapper;
    private CategoryService categoryService;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        mapper = mock(ProductMapper.class);
        categoryService = mock(CategoryService.class);
        service = new ProductService(repository, mapper, categoryService);
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE})
    @DisplayName("Toute categorie non nulle est transmise telle quelle et prime sur le nom")
    void nonNullCategoryTakesPriorityWithoutValidatingItsValue(Long categoryId) {
        when(repository.findByCategoryId(categoryId)).thenReturn(List.of());

        assertEquals(List.of(), service.findAll(categoryId, "mail"));

        verify(repository).findByCategoryId(categoryId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper, categoryService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\r\n", " \t\n ", "\u2003"})
    @DisplayName("Sans categorie, un nom null, vide ou isBlank declenche findAll")
    void nullEmptyOrBlankNameSelectsAllProducts(String name) {
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), service.findAll(null, name));

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper, categoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "Telephone", " a ", "\ta\n", "\u00A0", "\u200B"})
    @DisplayName("Sans categorie, un nom non isBlank est transmis sans trim ni changement de casse")
    void nonBlankNameIsPassedUnchanged(String name) {
        when(repository.findByNameContainingIgnoreCase(name)).thenReturn(List.of());

        assertEquals(List.of(), service.findAll(null, name));

        verify(repository).findByNameContainingIgnoreCase(name);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper, categoryService);
    }

    @Test
    @DisplayName("Une categorie sans resultat ne provoque aucun repli sur le nom ou findAll")
    void emptyCategoryResultDoesNotFallBackToAnotherSearch() {
        when(repository.findByCategoryId(42L)).thenReturn(List.of());

        assertEquals(List.of(), service.findAll(42L, "telephone"));

        verify(repository).findByCategoryId(42L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper, categoryService);
    }

    @Test
    @DisplayName("Le mapping conserve l'ordre et les doublons sans filtrage supplementaire")
    void mapsEveryProductInEncounterOrderIncludingDuplicates() {
        Product first = mock(Product.class);
        Product second = mock(Product.class);
        ProductResponse firstResponse = response(2L, "Z");
        ProductResponse secondResponse = response(1L, "A");
        when(repository.findAll()).thenReturn(List.of(first, second, first));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        List<ProductResponse> result = service.findAll(null, null);

        assertEquals(List.of(firstResponse, secondResponse, firstResponse), result);
        InOrder order = inOrder(repository, mapper);
        order.verify(repository).findAll();
        order.verify(mapper).toResponse(first);
        order.verify(mapper).toResponse(second);
        order.verify(mapper).toResponse(first);
        verifyNoMoreInteractions(repository, mapper);
        verifyNoInteractions(categoryService);
        assertThrows(UnsupportedOperationException.class, () -> result.add(firstResponse));
    }

    @Test
    @DisplayName("La liste vide retournee est elle aussi non modifiable")
    void emptyResultIsUnmodifiable() {
        when(repository.findAll()).thenReturn(List.of());

        List<ProductResponse> result = service.findAll(null, null);

        assertThrows(UnsupportedOperationException.class, () -> result.add(response(1L, "A")));
    }

    @Test
    @DisplayName("Une reponse null du mapper est conservee dans la liste")
    void nullMapperResultIsNotFilteredOut() {
        Product product = mock(Product.class);
        when(repository.findAll()).thenReturn(List.of(product));
        when(mapper.toResponse(product)).thenReturn(null);

        assertEquals(Arrays.asList((ProductResponse) null), service.findAll(null, null));

        verify(mapper).toResponse(product);
    }

    @Test
    @DisplayName("Une liste null du repository provoque une NullPointerException")
    void nullRepositoryResultIsNotConvertedToAnEmptyList() {
        when(repository.findAll()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> service.findAll(null, null));

        verifyNoInteractions(mapper, categoryService);
    }

    @Test
    @DisplayName("Une erreur du repository est propagee sans repli ni conversion")
    void repositoryExceptionIsPropagatedUnchanged() {
        IllegalStateException failure = new IllegalStateException("repository failure");
        when(repository.findByCategoryId(42L)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.findAll(42L, "telephone")));

        verify(repository).findByCategoryId(42L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper, categoryService);
    }

    @Test
    @DisplayName("Une erreur du mapper interrompt le parcours et est propagee telle quelle")
    void mapperExceptionStopsMappingAndIsPropagatedUnchanged() {
        Product first = mock(Product.class);
        Product second = mock(Product.class);
        IllegalStateException failure = new IllegalStateException("mapping failure");
        when(repository.findAll()).thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.findAll(null, null)));

        verify(mapper).toResponse(first);
        verifyNoMoreInteractions(mapper);
        verifyNoInteractions(categoryService);
    }

    private ProductResponse response(Long id, String name) {
        return new ProductResponse(id, name, null, BigDecimal.ONE, 0, 1L, "Categorie");
    }
}
