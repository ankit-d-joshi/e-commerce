package com.ecommerce.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ecommerce.catalog.domain.Product;
import com.ecommerce.catalog.exception.ProductNotFoundException;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.catalog.web.dto.ProductResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit test: no Spring context is started at all, the repository is a Mockito mock.
 * This is the fastest, cheapest tier of the test pyramid — it exists to pin down
 * ProductService's own logic (entity-to-DTO mapping, the not-found path) in isolation
 * from Spring wiring or a real database. Slower tiers (below) re-verify the same
 * behavior end-to-end; this tier's job is to fail fast and precisely when just the
 * service's logic breaks.
 */
// @ExtendWith(MockitoExtension.class) is JUnit 5's plugin mechanism: it registers
// Mockito's JUnit extension so that @Mock-annotated fields below get initialized
// automatically before each test method runs. Without this, `productRepository` would
// just be null — the annotation alone does nothing; something has to process it.
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	// @Mock creates a plain Mockito test double — a fake ProductRepository whose methods
	// do nothing and return null/empty by default until you tell it what to return with
	// when(...).thenReturn(...). Crucially, no Spring context exists anywhere in this
	// test: Mockito and Spring are entirely separate libraries here. Contrast this with
	// @MockitoBean in ProductControllerTest, which also creates a Mockito mock but then
	// additionally registers it *into a running Spring ApplicationContext* in place of
	// the real bean — that only makes sense in a test that boots Spring at all, which
	// this one deliberately doesn't (see the class comment above: no Spring context, by
	// design, for speed and isolation).
	@Mock
	private ProductRepository productRepository;

	private ProductService productService;

	@Test
	void findByIdReturnsMappedResponseWhenProductExists() {
		// Constructed with `new`, by hand, in plain Java — no Spring involved. Since
		// ProductService takes its one dependency via constructor injection (see
		// ProductService.java), any caller can supply that dependency directly; a test
		// doesn't need a DI container just to build the object under test.
		productService = new ProductService(productRepository);
		Product product = newProduct(1L, "SKU-1", "Widget", new BigDecimal("9.99"));
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		ProductResponse response = productService.findById(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.sku()).isEqualTo("SKU-1");
		assertThat(response.name()).isEqualTo("Widget");
		assertThat(response.price()).isEqualByComparingTo("9.99");
	}

	@Test
	void findByIdThrowsWhenProductMissing() {
		productService = new ProductService(productRepository);
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.findById(99L))
				.isInstanceOf(ProductNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void findAllMapsEachPageEntryToAResponse() {
		productService = new ProductService(productRepository);
		Pageable pageable = PageRequest.of(0, 20);
		Product product = newProduct(1L, "SKU-1", "Widget", new BigDecimal("9.99"));
		Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
		when(productRepository.findAll(pageable)).thenReturn(page);

		Page<ProductResponse> result = productService.findAll(pageable);

		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).sku()).isEqualTo("SKU-1");
	}

	/**
	 * Product's id/createdAt/updatedAt are populated by JPA/Hibernate at persist time, not
	 * by application code — there's deliberately no setter for them (see Product's
	 * Javadoc). Spring's ReflectionTestUtils is the standard way to poke those fields
	 * directly in a unit test that has no real persistence layer to populate them for us.
	 */
	private static Product newProduct(Long id, String sku, String name, BigDecimal price) {
		Product product = new Product(sku, name, "description", price);
		ReflectionTestUtils.setField(product, "id", id);
		ReflectionTestUtils.setField(product, "createdAt", Instant.now());
		ReflectionTestUtils.setField(product, "updatedAt", Instant.now());
		return product;
	}
}
