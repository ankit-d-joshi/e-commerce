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
// @Entity tells Hibernate (the JPA implementation Spring Boot wires up by default) that
// instances of this class are persisted as rows in a table — Hibernate generates the SQL
// (SELECT/INSERT/UPDATE/DELETE) for us based on this class's fields and annotations.
@Entity
// @Table pins the table name explicitly. Without it, Hibernate would derive one from the
// class name ("Product" -> "product") — spelling it out here avoids relying on that
// implicit naming convention and keeps the mapping obvious when reading either side.
@Table(name = "products")
public class Product {

	// @Id marks this field as the table's primary key.
	@Id
	// @GeneratedValue says the database generates the id's value, not application code.
	// GenerationType.IDENTITY means "use the column's IDENTITY property" — i.e. Postgres's
	// auto-incrementing serial/identity column (see the `id` column in
	// V1__create_products_table.sql). Hibernate does not assign the id itself; it inserts
	// the row without one and reads back whatever Postgres generated.
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column maps a field to a table column. nullable/unique here describe Java-side
	// expectations Hibernate validates against the real schema at startup (because
	// application.yml sets ddl-auto=validate) — they do NOT create the constraint
	// themselves. The actual NOT NULL / UNIQUE constraint lives in the Flyway migration
	// (V1__create_products_table.sql); if the two ever disagree, startup fails fast rather
	// than the app silently allowing rows the real schema would reject.
	@Column(nullable = false, unique = true)
	private String sku;

	@Column(nullable = false)
	private String name;

	// No nullable/unique here because this column allows NULL in the schema (a product
	// description is optional) — the default @Column with no attributes just maps the
	// field to a same-named column with no extra constraint expectations.
	@Column
	private String description;

	// BigDecimal, not double/float, for money. Doubles are binary floating-point and
	// cannot represent most decimal fractions exactly (0.1 + 0.2 != 0.3 in binary
	// floating-point) — that rounding error is unacceptable for currency. BigDecimal
	// stores an exact decimal value and is the standard Java type for money.
	@Column(nullable = false)
	private BigDecimal price;

	// @CreationTimestamp is a Hibernate-specific annotation (not part of the JPA spec)
	// that sets this field to the current time automatically the moment the entity is
	// first inserted — application code never sets it.
	@CreationTimestamp
	// updatable = false means Hibernate never includes this column in an UPDATE
	// statement, even if the in-memory field were somehow changed — a creation timestamp
	// should never move once written.
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	// @UpdateTimestamp is the sibling annotation: Hibernate refreshes this field to the
	// current time on every UPDATE (and on the initial INSERT too).
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// JPA/Hibernate builds entities via reflection when reading rows back from the
	// database (it needs to construct a bare Product, then populate fields itself) — it
	// requires a no-arg constructor to do that. `protected`, not `public`, because
	// application code should never call this directly; only the persistence framework
	// (and subclasses, of which there are none here) needs it. The public constructor
	// below is the one real code uses.
	protected Product() {
	}

	public Product(String sku, String name, String description, BigDecimal price) {
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.price = price;
	}

	// Getters only, deliberately — no setters. id/createdAt/updatedAt are populated by
	// the database/Hibernate, not application code, so giving them setters would invite
	// code that overwrites values it has no business changing. sku/name/description/price
	// also have no setters yet because Step 1 has no update endpoint (YAGNI) — if/when an
	// update use case appears, add exactly the mutation it needs then, not speculatively
	// now.
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
