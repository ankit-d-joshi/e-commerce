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
@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Page<ProductResponse> findAll(Pageable pageable) {
		return productRepository.findAll(pageable).map(ProductResponse::from);
	}

	public ProductResponse findById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));
		return ProductResponse.from(product);
	}
}
