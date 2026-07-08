-- Flyway migration V1.
--
-- Flyway naming convention: "V<version>__<description>.sql"
--   - Prefix "V" = a *versioned* migration (applied exactly once, in version order).
--   - "<version>" here is 1 — versions can also be dotted (1.1, 2.3) for finer ordering.
--   - Double underscore "__" separates the version from the free-text description.
--   - Description becomes part of the row Flyway records in flyway_schema_history, so
--     write it as something a future engineer scanning history would understand.
--
-- Flyway computes a checksum of this file's contents the first time it applies it and
-- stores that checksum alongside the version number. If this file is edited *after* it
-- has already run somewhere (a teammate's machine, CI, prod), Flyway will refuse to run
-- on that environment next time — checksum mismatch. That's deliberate: a migration that
-- already ran is a historical fact, not a draft. Fixing a mistake means writing a NEW
-- migration (V2, V3, ...) that alters what V1 built, never editing V1 itself.

-- The core catalog table. Note what is intentionally NOT here: no stock/quantity column
-- (that belongs to the future Inventory service's own database — Step 7), no foreign
-- keys to other services' data (services don't share databases in this architecture).
CREATE TABLE products (
    -- BIGINT identity column: Postgres auto-increments this via a hidden sequence.
    -- BIGINT (not INT) because product catalogs in the real world can run past 2^31 rows
    -- over the years, and there's no meaningful cost to choosing the wider type upfront.
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- Stock Keeping Unit: the business/human-facing product identifier. UNIQUE + NOT NULL
    -- because two products can never legitimately share a SKU — this is a business
    -- invariant enforced by the database, not just application code (defense in depth:
    -- even a buggy or concurrent write can't violate it).
    sku             VARCHAR(64)     NOT NULL UNIQUE,

    name            VARCHAR(255)    NOT NULL,

    -- Nullable: not every product needs a description on day one.
    description     TEXT,

    -- NUMERIC (not FLOAT/DOUBLE) for money. Floating point can't represent most decimal
    -- fractions exactly (e.g. 0.1), which silently corrupts prices after enough
    -- arithmetic. NUMERIC(10,2) = up to 8 digits before the decimal point, exactly 2
    -- after — plenty for a product price, and exact.
    price           NUMERIC(10, 2)  NOT NULL CHECK (price >= 0),

    -- Populated by Hibernate's @CreationTimestamp / @UpdateTimestamp at the application
    -- layer (not a DB trigger/default) — Step 1 keeps this logic in one place (Java) and
    -- introduces DB-side automation only if a real need for it shows up later (YAGNI).
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL
);

-- Products are typically looked up by SKU in real systems (returns, support, integration
-- with external catalogs). UNIQUE above already creates an index enforcing uniqueness, so
-- this lookup is already fast — no separate index needed. (Documented here so it's clear
-- this wasn't simply forgotten.)
