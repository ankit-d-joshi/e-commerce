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
// webEnvironment = RANDOM_PORT means this test starts a real embedded Tomcat listening
// on an actual free TCP port (chosen at random so parallel test runs never collide on a
// fixed port like 8080) — as opposed to the default @SpringBootTest, which starts the
// full application context but no real HTTP server at all.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @Import here does the same job as in ProductRepositoryTest: pulls in the
// TestcontainersConfiguration bean so this test also runs against a real Postgres
// container rather than whatever application.yml's default datasource would otherwise
// point at.
@Import(TestcontainersConfiguration.class)
class ProductCatalogIntegrationTest {

	// @LocalServerPort injects the actual port number the embedded server
	// randomly bound to at startup. Since RANDOM_PORT means the port isn't known until
	// the server has already started, this is the only way test code can find out what
	// URL to actually send requests to — it can't be hardcoded.
	@LocalServerPort
	private int port;

	// TestRestTemplate is a Spring Boot HTTP client built specifically for
	// tests: unlike RestTemplate/RestClient used by production code, it does not throw an
	// exception when the server responds with a 4xx or 5xx status. That matters directly
	// below in getUnknownProductReturns404ProblemDetail — this test needs to receive and
	// assert on the 404 response itself, not have the client throw before the test gets a
	// chance to inspect it.
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
