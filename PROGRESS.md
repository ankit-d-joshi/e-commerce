# Progress Tracker

Read this file first at the start of any new session to resume cleanly — Claude Code
does not retain memory between sessions.

## Decisions log

- 2026-07-07 — Java version: pinned to **Java 25 LTS** (already installed locally),
  instead of the brief's original Java 21 LTS baseline. User chose this since it's newer
  and nothing extra needed to install.
- 2026-07-07 (Step 1) — **Spring Boot 4.1.0**, not the originally-pinned 3.x: Spring Boot
  3.5 reached open-source end-of-life 2026-06-30 (last release 3.5.16); 4.1.0 is the
  current GA line (Spring Framework 7) and explicitly supports Java 25.
- 2026-07-07 (Step 1) — **Flyway** chosen for database migrations (SQL-first, most
  popular, best teaching vehicle) over Liquibase.
- 2026-07-07 (Step 1) — **Standalone per-service directory** layout (`product-catalog/`
  with its own self-contained `pom.xml`), not a multi-module Maven parent — each service
  stays independently buildable/deployable, mirroring real microservices. No parent pom
  until/unless a real cross-service need for one appears.
- 2026-07-07 (Step 1) — Two new durable operating rules added to `CLAUDE.md`:
  comprehensive test coverage is required for all application logic, and every future
  decision defaults to the industry-standard/professionally-defensible option over the
  expedient shortcut (see CLAUDE.md operating rules 8–9 for full wording).
- 2026-07-07 (Step 1) — **JaCoCo 0.8.14** pinned for coverage (first release with
  official, non-experimental Java 25 bytecode support).
- 2026-07-07 (Step 1) — **Testcontainers** (real Postgres, pinned `postgres:18.4`) used
  for all tests that touch persistence, instead of H2 — proves the actual Flyway
  migrations run and matches production behavior exactly.
- 2026-07-07 (Step 1) — Toolchain rough edge: `spring-boot-resttestclient:4.1.0`'s
  published POM is missing two of its own runtime dependencies
  (`spring-boot-restclient`, `spring-boot-http-client`) — `TestRestTemplate` throws
  `NoClassDefFoundError` without them. Worked around by declaring both explicitly as
  test-scope dependencies in `product-catalog/pom.xml`. Worth rechecking on the next
  Spring Boot 4.1.x patch release — may be fixed upstream.
- 2026-07-08 (Step 1) — **Teaching contract rewritten**: the "senior Java engineer,
  build freely" split is gone. Java/Spring application code now gets the same
  teach-while-writing + quiz treatment as cloud-native work, and the commenting rule was
  strengthened to cover *every* annotation/API/construct with no "obvious" exception —
  triggered by 8 concrete gaps the user found in Step 1's test files (e.g. `@MockitoBean`
  vs `@Mock`, `@Import`, `@LocalServerPort`, `proxyBeanMethods`). All of Step 1's Java
  files (main + test) were retrofitted with inline comments closing those gaps before
  first commit; `CLAUDE.md`'s "What this project is" and "teaching contract" sections
  were rewritten to match. See `CLAUDE.md` for the current wording.

## Status: Step 1 — First Spring service (Product Catalog)

**Step 0 complete (2026-07-07).** Toolchain installed and verified:

| Tool | Version |
|------|---------|
| Java | OpenJDK 25 (LTS) |
| git | 2.43.0 |
| Maven | 3.8.7 |
| Docker | 29.6.1 (+ Compose v5.3.1) |
| `kind` | 0.32.0 |
| `kubectl` | 1.36.2 |
| `helm` | 4.2.2 |

OS: Linux Mint 22.3 (Ubuntu/Debian-based).

A local `kind` cluster named `ecommerce-dev` is up (`kubectl cluster-info --context
kind-ecommerce-dev`, `kubectl get nodes` both confirmed working). Concepts covered:
`kind` nodes as Docker containers, kubeconfig/contexts and why `kubectl` is always
scoped to a single current-context (verified hands-on: `current-context` switches to a
newly-created cluster automatically, it doesn't aggregate across clusters). Quiz passed.

Git repo initialized at repo root, default branch renamed `master` → `main`, minimal
`.gitignore` added (Editor only — no Java-specific entries yet since no Maven
project exists until Step 1).

**Step 1 build complete (2026-07-07), pending quiz + commit.** Built `product-catalog/`:
a standalone Spring Boot 4.1.0 service with `GET /api/products` (paginated, via
`PagedModel`) and `GET /api/products/{id}`, backed by PostgreSQL. Domain: `Product`
entity + `ProductRepository`; schema owned entirely by two Flyway migrations
(`V1__create_products_table.sql`, `V2__insert_seed_products.sql`), Hibernate set to
`ddl-auto: validate` so it never touches the schema itself. Errors surface as RFC 9457
`ProblemDetail` via a `@RestControllerAdvice`.

Four-tier test suite (12 tests, 0 failures): Mockito unit test on `ProductService`;
`@WebMvcTest` slice on `ProductController`; `@DataJpaTest` + Testcontainers
(`postgres:18.4`) proving the Flyway migrations run against a real database; full
`@SpringBootTest(RANDOM_PORT)` end-to-end test over real HTTP. JaCoCo 0.8.14 reports
**100% instruction coverage on every application class** (the only gap is
`ProductCatalogApplication.main()`, which only executes when the app is actually
launched). `mvn clean verify` passes end-to-end.

Flyway + Testcontainers quiz passed (2026-07-08) — user correctly explained
`ddl-auto: validate` as the ORM/migration-tool responsibility split, why a shipped
migration is edited via a new version rather than in place, what
`flyway_schema_history` checksums protect against, why Testcontainers-over-H2 avoids
the works-locally-not-in-prod dialect trap, and what `@ServiceConnection` auto-wires.

**Next action:** commit Step 1, then wait for explicit confirmation before starting
Step 2 (containerize with Docker).

## Roadmap checklist

- [x] Step 0 — Environment & "see it work first"
- [x] Step 1 — First Spring service (Product Catalog)
- [ ] Step 2 — Containerize it (Dockerfile + Compose)
- [ ] Step 3 — Onto Kubernetes (hand-written manifests)
- [ ] Step 4 — Make it observable (Actuator, Prometheus, Grafana)
- [ ] Step 5 — Second service + database-per-service (Order)
- [ ] Step 6 — Single entry point (gateway/ingress)
- [ ] Step 7 — Asynchronous messaging (Kafka)
- [ ] Step 8 — Distributed tracing
- [ ] Step 9 — Reliability mechanics
- [ ] Step 10 — Package with Helm
- [ ] Step 11 — CI/CD
- [ ] Step 12 — Security & hardening
- [ ] Step 13 — SLOs & alerting
- [ ] Step 14 — Documentation & graduation artifacts
- [ ] Step 15 — (Optional, paid) Cloud deployment
