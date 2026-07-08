package com.ecommerce.catalog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers setup for the whole test suite.
 *
 * <p>{@code @ServiceConnection} is Spring Boot's Testcontainers integration: it starts a
 * real Postgres container per test run and automatically wires the datasource URL,
 * username and password Spring needs to connect to it — no manual
 * {@code spring.datasource.*} overrides required in test config. Any test class that
 * imports this configuration (directly or via {@code @SpringBootTest}'s component scan)
 * gets a real database instead of a mock or an in-memory substitute like H2.
 *
 * <p>Why a real Postgres instead of H2: H2's SQL dialect and behavior differ from
 * Postgres in ways that matter (constraint handling, JSON types, sequence behavior,
 * upsert syntax). Testing against the exact engine used in production is the only way to
 * be confident the Flyway migrations and JPA mappings actually work — "tests pass but
 * prod breaks" is a classic H2-vs-real-database trap.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// Pinned to the same major.minor as the README's throwaway dev container and
		// (later) the Docker Compose / Kubernetes Postgres image — one Postgres version
		// across the whole project, not "whatever :latest happens to resolve to today".
		//
		// Note: Testcontainers 2.x's PostgreSQLContainer is no longer a bare generic type
		// (it self-binds its fluent-builder type parameter internally), so no <> here —
		// a change from the Testcontainers 1.x API most existing tutorials still show.
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18.4"));
	}

}
