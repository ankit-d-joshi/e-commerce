package com.ecommerce.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Product Catalog service — the class Java actually runs when the
 * application starts.
 */
// @SpringBootApplication is a convenience annotation that bundles three others:
//  - @SpringBootConfiguration: marks this as a source of Spring bean definitions (a
//    specialized @Configuration), so this class itself can declare @Bean methods if ever
//    needed.
//  - @EnableAutoConfiguration: turns on Spring Boot's autoconfiguration — the mechanism
//    that inspects what's on the classpath (e.g. spring-boot-starter-data-jpa,
//    flyway-core) and automatically configures the matching beans (DataSource,
//    EntityManagerFactory, Flyway migration runner, etc.) without any of it being
//    written by hand here.
//  - @ComponentScan: tells Spring to scan this class's package and all sub-packages for
//    @Component/@Service/@Repository/@RestController-annotated classes and register them
//    as beans automatically. This is why ProductController, ProductService, and
//    ProductRepository are found and wired up even though nothing here explicitly lists
//    them — they all live under com.ecommerce.catalog.
@SpringBootApplication
public class ProductCatalogApplication {

	public static void main(String[] args) {
		// SpringApplication.run bootstraps the entire application: it builds the Spring
		// ApplicationContext (the container holding every bean), runs autoconfiguration,
		// starts the embedded Tomcat servlet container on the configured port, and runs
		// Flyway migrations before the app is considered "up." This call blocks — it
		// doesn't return until the application shuts down.
		SpringApplication.run(ProductCatalogApplication.class, args);
	}

}
