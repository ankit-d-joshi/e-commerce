package com.ecommerce.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ecommerce.catalog.exception.ProductNotFoundException;
import com.ecommerce.catalog.service.ProductService;
import com.ecommerce.catalog.web.dto.ProductResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Web-slice test: {@code @WebMvcTest} boots only the MVC layer (controllers, exception
 * handlers, JSON conversion) for this one controller — no database, no service
 * implementation, no full application context. {@link ProductService} is replaced with a
 * Mockito mock via {@code @MockitoBean}. This tier exists to verify the HTTP contract
 * (status codes, URL paths, JSON shape, error responses) independent of business logic
 * or persistence, which are already covered by the other tiers.
 */
// @WebMvcTest(ProductController.class) starts a *slice* of the full application context
// — just the Spring MVC machinery (this one controller, @RestControllerAdvice classes,
// JSON message converters) — instead of every bean in the app. No DataSource, no
// ProductRepository, no real database is created at all; that's what makes this tier
// fast compared to the full @SpringBootTest below.
@WebMvcTest(ProductController.class)
class ProductControllerTest {

	// @Autowired asks the Spring test context (the slice @WebMvcTest started) to inject
	// an existing bean of this type — MockMvcTester is a bean @WebMvcTest's
	// autoconfiguration registers for exactly this purpose: driving HTTP requests through
	// the MVC layer in-process (no real network socket) and asserting on the response.
	@Autowired
	private MockMvcTester mvc;

	// @MockitoBean creates a Mockito mock exactly like
	// plain @Mock does, but then goes one step further — it registers that mock as a real
	// bean *inside the Spring test context* @WebMvcTest started, replacing whatever real
	// ProductService bean would otherwise have been created there. ProductController gets
	// this mock injected into it by Spring, the same way it would get the real
	// ProductService in production. Plain @Mock (used in ProductServiceTest) has no
	// concept of a Spring context at all — there, ProductService is built by hand with
	// `new`, so there's no bean to replace. @MockitoBean only makes sense, and is only
	// available, in a test that boots some Spring context to replace a bean inside.
	@MockitoBean
	private ProductService productService;

	@Test
	void getReturnsProductWhenFound() {
		ProductResponse response = new ProductResponse(
				1L, "SKU-1", "Widget", "A widget", new BigDecimal("9.99"), Instant.now(), Instant.now());
		when(productService.findById(1L)).thenReturn(response);

		mvc.get().uri("/api/products/1")
				.assertThat()
				.hasStatusOk()
				.hasContentType(MediaType.APPLICATION_JSON)
				.bodyJson()
				.extractingPath("$.sku").isEqualTo("SKU-1");
	}

	@Test
	void getReturns404ProblemDetailWhenMissing() {
		when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

		mvc.get().uri("/api/products/99")
				.assertThat()
				.hasStatus(404)
				.bodyJson()
				.extractingPath("$.status").isEqualTo(404);
	}

	@Test
	void listReturnsPageOfProducts() {
		ProductResponse response = new ProductResponse(
				1L, "SKU-1", "Widget", "A widget", new BigDecimal("9.99"), Instant.now(), Instant.now());
		when(productService.findAll(any())).thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

		mvc.get().uri("/api/products")
				.assertThat()
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content[0].sku").isEqualTo("SKU-1");
	}
}
