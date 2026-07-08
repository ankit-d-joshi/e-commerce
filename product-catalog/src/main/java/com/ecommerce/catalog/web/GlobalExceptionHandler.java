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
// @RestControllerAdvice is @ControllerAdvice + @ResponseBody combined (the same
// relationship @RestController has to @Controller): it makes this class apply globally
// to every @RestController in the application — Spring intercepts any matching exception
// thrown from any controller method and routes it here, instead of each controller
// needing its own try/catch or its own @ExceptionHandler methods.
@RestControllerAdvice
class GlobalExceptionHandler {

	// @ExceptionHandler registers this method as the handler for the given exception
	// type (and its subclasses). When ANY controller method in the app lets a
	// ProductNotFoundException propagate uncaught, Spring intercepts it here instead of
	// it reaching the client as a raw 500 stack trace.
	@ExceptionHandler(ProductNotFoundException.class)
	ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
		// ProblemDetail.forStatusAndDetail is a Spring-provided factory that builds the
		// RFC 9457 body, filling in "status" from the HttpStatus given and "detail" from
		// the string given; Spring then also sets the actual HTTP response status code to
		// match (404 here), so the status in the JSON body and the real HTTP status
		// always agree.
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}
}
