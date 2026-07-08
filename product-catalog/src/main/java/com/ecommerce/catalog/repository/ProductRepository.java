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
// This is an interface with no method bodies and, notably, no implementing class
// anywhere in this codebase — yet it's a fully working Spring bean at runtime. Spring
// Data JPA generates a proxy implementation for any interface that extends
// JpaRepository at application startup (reflection + dynamic proxy), wiring in a real
// EntityManager-backed implementation of every inherited method. The two type
// parameters, <Product, Long>, tell it which entity this repository manages and the
// Java type of that entity's @Id field — that's how it knows what SQL to generate for
// findAll/findById/save/etc.
public interface ProductRepository extends JpaRepository<Product, Long> {
}
