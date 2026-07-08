package com.ecommerce.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ecommerce.catalog.TestcontainersConfiguration;
import com.ecommerce.catalog.domain.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

/**
 * {@code @DataJpaTest} boots just the JPA/repository slice (entities, repositories,
 * DataSource) rather than the whole application. By default it swaps in an embedded
 * in-memory database (H2) — {@code @AutoConfigureTestDatabase(replace = NONE)} turns that
 * off so the real Postgres container from {@link TestcontainersConfiguration} (wired via
 * {@code @ServiceConnection}) is used instead.
 *
 * <p>This is the tier that actually proves the Flyway migrations work: on startup, Flyway
 * runs V1/V2 against the container exactly as it would in any real environment, then
 * Hibernate validates {@link Product}'s mapping against the resulting schema
 * ({@code ddl-auto=validate}). If the entity and the SQL migrations have drifted apart,
 * this test fails here — not silently in production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	@Test
	void migrationsRunAndSeedDataIsQueryable() {
		// V2__insert_seed_products.sql inserts 5 demo rows; this exercises the real
		// Flyway-migrated schema rather than test-authored fixtures.
		var page = productRepository.findAll(PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isEqualTo(5);
		assertThat(page.getContent())
				.extracting(p -> p.getSku())
				.contains("SKU-0001", "SKU-0002");
	}

	@Test
	void savingAProductPersistsAndGeneratesIdAndTimestamps() {
		Product saved = productRepository.save(new Product("SKU-TEST", "Test Product", "desc", new BigDecimal("1.00")));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	void skuUniquenessIsEnforcedByTheDatabase() {
		productRepository.save(new Product("SKU-DUP", "First", "desc", new BigDecimal("1.00")));
		productRepository.flush();

		// The UNIQUE constraint from V1__create_products_table.sql is enforced by
		// Postgres itself, not application code — this proves the constraint actually
		// exists in the real schema, not just in the entity mapping.
		assertThrows(DataIntegrityViolationException.class, () -> {
			productRepository.save(new Product("SKU-DUP", "Second", "desc", new BigDecimal("2.00")));
			productRepository.flush();
		});
	}
}
