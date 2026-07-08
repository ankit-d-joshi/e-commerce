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
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	private ProductService productService;

	@Test
	void findByIdReturnsMappedResponseWhenProductExists() {
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
