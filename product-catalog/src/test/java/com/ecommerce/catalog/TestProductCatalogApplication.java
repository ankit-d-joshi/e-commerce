package com.ecommerce.catalog;

import org.springframework.boot.SpringApplication;

/**
 * When is this class actually called? Answer — never, by the automated
 * build. It is not referenced by any test class, {@code mvn test}/{@code verify} never
 * touches it, and there is no CI or production path that runs it. It exists purely as a
 * developer convenience: an alternate {@code main()} an IDE can run directly (right-click
 * -&gt; Run) to start the full application locally, wired to the same real Testcontainers
 * Postgres the automated tests use, without needing a separately-managed database
 * running on the machine first.
 *
 * <p>{@code SpringApplication.from(ProductCatalogApplication::main)} takes the real
 * application's own {@code main} method reference and re-runs it, but
 * {@code .with(TestcontainersConfiguration.class)} first layers in the extra
 * Testcontainers bean definition — so this is the production {@code ProductCatalogApplication}
 * plus one extra test-only bean, not a separate/parallel application.
 */
public class TestProductCatalogApplication {

	public static void main(String[] args) {
		SpringApplication.from(ProductCatalogApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
