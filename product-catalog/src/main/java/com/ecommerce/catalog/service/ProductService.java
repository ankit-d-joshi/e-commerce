package com.ecommerce.catalog.service;

import com.ecommerce.catalog.domain.Product;
import com.ecommerce.catalog.exception.ProductNotFoundException;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.catalog.web.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Business logic for reading products. Deliberately thin for Step 1 (two read-only
 * operations, no writes yet) — mapping happens here so the controller and repository
 * never deal with each other's concerns directly.
 */
// @Service is a Spring "stereotype" annotation — a marker that tells Spring's component
// scan "this class is a bean, create one instance of it and manage its lifecycle."
// @Component is the base annotation all stereotypes build on; @Service, @Repository, and
// @Controller/@RestController are the same mechanism with different names, used purely
// to communicate each class's role (service = business logic) to readers and tooling.
@Service
public class ProductService {

	private final ProductRepository productRepository;

	// Constructor injection: Spring sees this is the only constructor and automatically
	// calls it with a ProductRepository bean from its context — no @Autowired annotation
	// needed on the constructor itself (that's only required when a class has more than
	// one constructor and you must tell Spring which one to use for injection). This is
	// the recommended style over field injection (@Autowired directly on a field) because
	// it makes `productRepository` `final` (assigned exactly once, never reassigned) and
	// lets this class be constructed with a real or mock repository in plain Java (see
	// ProductServiceTest), with no Spring involved at all.
	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Page<ProductResponse> findAll(Pageable pageable) {
		// Page<Product> from the repository -> Page<ProductResponse> for the API.
		// Page.map() applies a function to every element while preserving the page's
		// metadata (total elements, page number, etc.) — it doesn't re-run the query.
		// ProductResponse::from is a *method reference*: shorthand for the lambda
		// `product -> ProductResponse.from(product)`, pointing directly at the static
		// factory method declared on ProductResponse.
		return productRepository.findAll(pageable).map(ProductResponse::from);
	}

	public ProductResponse findById(Long id) {
		// JpaRepository.findById returns Optional<Product> rather than Product directly
		// or null — Optional forces the caller to explicitly decide what happens when
		// nothing is found, instead of risking a NullPointerException somewhere later.
		// orElseThrow(supplier) either unwraps the Product if present, or invokes the
		// supplier and throws whatever exception it returns — here, the custom
		// ProductNotFoundException, which the web layer later translates into a 404.
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));
		return ProductResponse.from(product);
	}
}
