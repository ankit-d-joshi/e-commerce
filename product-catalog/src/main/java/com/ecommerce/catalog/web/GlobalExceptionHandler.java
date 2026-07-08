package com.ecommerce.catalog.web;

import com.ecommerce.catalog.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception -> HTTP response mapping for the whole controller layer.
 *
 * <p>Returns {@link ProblemDetail}, the RFC 9457 ("Problem Details for HTTP APIs")
 * standard error body — {@code {"type":..., "title":..., "status":..., "detail":...}} —
 * rather than a hand-rolled error JSON shape. It's the modern Spring Boot default and
 * gives API clients a consistent, self-describing error format across every endpoint and
 * every future service in this project.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(ProductNotFoundException.class)
	ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}
}
