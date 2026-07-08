package com.ecommerce.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The top of the test pyramid for this service: boots the entire application
 * ({@code webEnvironment = RANDOM_PORT} starts a real embedded servlet container on a
 * free port) against the real Postgres Testcontainer, then drives it purely over HTTP
 * with no mocks anywhere — controller, service, repository, Flyway migrations and the
 * database are all real. This is what actually proves the service works end-to-end;
 * the other three tiers exist to make failures here fast and precise to diagnose.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ProductCatalogIntegrationTest {

	@LocalServerPort
	private int port;

	private final TestRestTemplate restTemplate = new TestRestTemplate();

	@Test
	void listProductsReturnsTheFlywaySeedData() {
		ResponseEntity<String> response = restTemplate.getForEntity(url("/api/products"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("SKU-0001");
	}

	@Test
	void getKnownProductReturnsIt() {
		ResponseEntity<String> response = restTemplate.getForEntity(url("/api/products/1"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("SKU-0001");
	}

	@Test
	void getUnknownProductReturns404ProblemDetail() {
		ResponseEntity<String> response = restTemplate.getForEntity(url("/api/products/999999"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).contains("Product not found");
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}
}
