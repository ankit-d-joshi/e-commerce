package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Product}.
 *
 * <p>{@code JpaRepository} already provides paginated {@code findAll(Pageable)} and
 * {@code findById(Long)} — exactly what Step 1's two endpoints need. No custom query
 * methods are added yet (YAGNI): {@code findBySku} and similar will show up when a
 * feature actually needs them.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
