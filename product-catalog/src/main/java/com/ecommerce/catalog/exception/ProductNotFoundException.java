package com.ecommerce.catalog.exception;

/**
 * Thrown when a product is looked up by id and doesn't exist.
 * Translated to an HTTP 404 by {@link com.ecommerce.catalog.web.GlobalExceptionHandler}.
 */
// Extends RuntimeException, not Exception — this makes it an *unchecked* exception,
// meaning calling code (ProductController) is not forced by the compiler to catch it or
// declare `throws ProductNotFoundException`. That's deliberate here: this exception isn't
// meant to be caught by ordinary business logic at all — it's meant to propagate all the
// way up to GlobalExceptionHandler, which is registered globally to catch it and convert
// it into an HTTP response. Checked exceptions (extending Exception directly) exist for
// errors the caller is expected to recover from inline; that's not the case here.
public class ProductNotFoundException extends RuntimeException {

	// super(message) sets RuntimeException's built-in message field, retrievable later
	// via getMessage() — used both in logs and, here, as the "detail" text of the 404
	// ProblemDetail response body.
	public ProductNotFoundException(Long id) {
		super("Product not found: " + id);
	}
}
