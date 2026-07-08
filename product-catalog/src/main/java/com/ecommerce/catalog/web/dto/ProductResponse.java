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
public record ProductResponse(
		Long id,
		String sku,
		String name,
		String description,
		BigDecimal price,
		Instant createdAt,
		Instant updatedAt) {

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
