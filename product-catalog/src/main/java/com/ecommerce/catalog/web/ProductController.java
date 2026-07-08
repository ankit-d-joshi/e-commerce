package com.ecommerce.catalog.web;

import com.ecommerce.catalog.service.ProductService;
import com.ecommerce.catalog.web.dto.ProductResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST API for the product catalog. Step 1 ships list + get-by-id only; writes
 * (create/update/delete) aren't part of this step's scope (YAGNI) and will show up when a
 * later step actually needs them (e.g. an admin/ops path).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public PagedModel<ProductResponse> list(@PageableDefault(size = 20) Pageable pageable) {
		// Spring Data's Page serializes to JSON with no documented/stable field layout —
		// Spring Boot logs a warning about exactly this at runtime. PagedModel is the
		// supported, stable wrapper: fixed "content" + "page" (size/number/totalElements/
		// totalPages) shape, safe to treat as a real API contract.
		return new PagedModel<>(productService.findAll(pageable));
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable Long id) {
		return productService.findById(id);
	}
}
