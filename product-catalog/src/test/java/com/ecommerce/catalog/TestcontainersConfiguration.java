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
// @TestConfiguration marks this as a @Configuration class meant only for
// tests — critically, Spring's normal @ComponentScan (the one @SpringBootApplication
// turns on, which would otherwise pick up every @Configuration class under
// com.ecommerce.catalog) deliberately skips classes annotated @TestConfiguration. That
// means this class's @Bean method only runs when a test explicitly asks for it via
// @Import(TestcontainersConfiguration.class) (as ProductRepositoryTest and
// ProductCatalogIntegrationTest both do) — it can never accidentally leak into the real
// application or into a test that didn't opt in.
//
// proxyBeanMethods = false: by default, Spring wraps a @Configuration
// class in a CGLIB subclass proxy so that if one @Bean method calls another @Bean method
// directly in Java, Spring intercepts that call and returns the shared singleton instance
// rather than a second freshly-constructed object. That proxying costs a little startup
// time and only matters when @Bean methods call each other. This class has exactly one
// @Bean method, which calls no other @Bean method, so the proxy would do nothing useful
// — turning it off here is both correct and marginally faster to start.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// @Bean tells Spring "call this method once and manage the object it returns as a
	// bean in the context" — the standard way to hand Spring an object you construct
	// yourself (as opposed to @Component/@Service, which mark a whole class to be
	// instantiated by Spring). Needed here because PostgreSQLContainer is a third-party
	// Testcontainers type Spring knows nothing about; there's no way to put @Component on
	// it.
	@Bean
	// @ServiceConnection — see the class Javadoc above for what it does; placed on this
	// specific @Bean method because that's how Spring Boot knows *which* bean to wire the
	// connection details from, when a @Configuration class could in principle declare
	// more than one container/bean.
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
