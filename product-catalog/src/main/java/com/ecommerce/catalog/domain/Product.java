package com.ecommerce.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A sellable product in the catalog.
 *
 * <p>Deliberately does NOT track stock/quantity — that's owned by the Inventory service
 * once it exists (Step 7). Keeping catalog data and inventory data in separate services
 * with separate databases is the database-per-service pattern this whole build is
 * teaching; mixing them back into one entity here would undo that lesson before it starts.
 *
 * <p>{@code ddl-auto=validate} means this mapping must exactly match the schema Flyway
 * built in {@code V1__create_products_table.sql} — column names, nullability, types. If
 * they drift apart, the app fails fast at startup instead of silently misbehaving.
 */
@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// JPA requires a no-arg constructor to build entities via reflection.
	protected Product() {
	}

	public Product(String sku, String name, String description, BigDecimal price) {
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
