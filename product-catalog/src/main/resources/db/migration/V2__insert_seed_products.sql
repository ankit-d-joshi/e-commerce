-- Flyway migration V2 — seed data.
--
-- This is a *versioned* migration (V2, not R__), which matters: seed data that should
-- exist exactly once (demo/reference data checked into source control) belongs in a
-- versioned migration, run once, just like the schema change in V1. Flyway's other kind
-- of migration — "repeatable" migrations, named "R__<description>.sql" — re-run every
-- time their file content changes and are for things like recreating views or stored
-- procedures, not for inserting rows (re-running an INSERT on every change would either
-- duplicate rows or require the migration to be idempotent in a way that fights the
-- simplicity Flyway is going for). Bulk/generated fixture data for local dev, as opposed
-- to this handful of demo rows, would more commonly live outside Flyway entirely (a
-- seed script run manually, or a testing-only data loader) — but for a small, permanent,
-- reviewable set of demo products, a plain versioned migration is the industry-standard,
-- simplest choice, and exactly what "insert seed products" describes.
--
-- id is intentionally omitted: it's GENERATED ALWAYS AS IDENTITY (see V1), so Postgres
-- assigns it. Hardcoding IDs in seed data is a common source of collisions once real
-- rows start getting inserted through the application.
INSERT INTO products (sku, name, description, price, created_at, updated_at) VALUES
    ('SKU-0001', 'Mechanical Keyboard',       'Tactile brown switches, hot-swappable.',      89.99,  now(), now()),
    ('SKU-0002', 'Wireless Mouse',            'Ergonomic, 2.4GHz + Bluetooth.',              29.99,  now(), now()),
    ('SKU-0003', '27" 4K Monitor',            'IPS panel, USB-C with 90W power delivery.',   399.00, now(), now()),
    ('SKU-0004', 'USB-C Docking Station',     '12-in-1, dual HDMI, gigabit ethernet.',       119.50, now(), now()),
    ('SKU-0005', 'Standing Desk Converter',   'Height-adjustable, fits dual monitors.',      249.00, now(), now());
