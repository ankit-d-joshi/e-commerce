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
// @RestController is shorthand for @Controller + @ResponseBody: it marks this class as a
// Spring MVC controller (its methods handle incoming HTTP requests) AND tells Spring to
// write every method's return value directly into the HTTP response body (as JSON, via
// Jackson) rather than treating the returned string as a view/template name to render.
// Plain @Controller is for classes that return view names (server-rendered HTML) — not
// what a JSON API needs.
@RestController
// @RequestMapping at the class level sets a base path every method's mapping is
// relative to — so @GetMapping below maps to "/api/products", and @GetMapping("/{id}")
// maps to "/api/products/{id}".
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	// Same constructor-injection pattern as ProductService: Spring supplies the
	// ProductService bean automatically since this is the class's only constructor.
	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	// @GetMapping is shorthand for @RequestMapping(method = GET) — maps HTTP GET requests
	// on this path to this method.
	@GetMapping
	// Pageable carries page number/size/sort, parsed automatically by Spring from query
	// params like ?page=0&size=20&sort=name. @PageableDefault(size = 20) supplies the
	// default page size when the client doesn't specify one, instead of falling back to
	// Spring Data's own default (20 as of this Spring Boot version, but pinning it here
	// makes the default an explicit part of this API rather than an implicit framework
	// default that could change between versions).
	public PagedModel<ProductResponse> list(@PageableDefault(size = 20) Pageable pageable) {
		// Spring Data's Page serializes to JSON with no documented/stable field layout —
		// Spring Boot logs a warning about exactly this at runtime. PagedModel is the
		// supported, stable wrapper: fixed "content" + "page" (size/number/totalElements/
		// totalPages) shape, safe to treat as a real API contract.
		return new PagedModel<>(productService.findAll(pageable));
	}

	@GetMapping("/{id}")
	// @PathVariable binds the {id} segment of the URL path to this method parameter —
	// Spring converts the path segment's String value to a Long automatically.
	public ProductResponse get(@PathVariable Long id) {
		return productService.findById(id);
	}
}
