package com.ecommerce.catalog.web.dto;

import com.ecommerce.catalog.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outward-facing shape of a product. A record, not the {@link Product} entity itself —
 * keeping these separate means the JPA entity is free to evolve (new columns,
 * relationships) without automatically changing the public API contract, and rules out
 * accidentally serializing lazy-loaded associations or internal-only fields.
 */
// A Java record: a compact way to declare an immutable data carrier. Writing
// `record ProductResponse(Long id, String sku, ...)` auto-generates, for free, a
// constructor taking all these fields in order, a getter per field (named after the
// field, e.g. `.sku()` not `.getSku()`), plus equals()/hashCode()/toString() implemented
// by value. There is no way to mutate a record after construction — no setters exist,
// which is exactly what a DTO that just carries data out over HTTP needs; nothing should
// ever need to change it once built.
//
// This is a DTO (Data Transfer Object): a class whose only job is to define the shape of
// data crossing a boundary (here, the HTTP response body), separate from the @Entity
// that defines how the same data is stored in the database. Returning Product directly
// from the controller would leak persistence details (JPA proxies, lazy-loaded fields
// that throw if accessed outside a transaction) into the API and couple the public
// contract to the schema.
public record ProductResponse(
		Long id,
		String sku,
		String name,
		String description,
		BigDecimal price,
		Instant createdAt,
		Instant updatedAt) {

	// A static factory method, not a constructor override — records can't easily
	// overload their canonical (auto-generated) constructor to take a different type, so
	// a plain static method is the idiomatic way to build one record from another type of
	// object. Called as ProductResponse.from(product) from ProductService.
	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getSku(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}
}
