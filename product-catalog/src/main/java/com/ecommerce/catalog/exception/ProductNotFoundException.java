package com.ecommerce.catalog.exception;

/**
 * Thrown when a product is looked up by id and doesn't exist.
 * Translated to an HTTP 404 by {@link com.ecommerce.catalog.web.GlobalExceptionHandler}.
 */
public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(Long id) {
		super("Product not found: " + id);
	}
}
