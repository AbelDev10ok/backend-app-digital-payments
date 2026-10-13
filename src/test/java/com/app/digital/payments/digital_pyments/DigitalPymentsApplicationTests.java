package com.app.digital.payments.digital_pyments;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.app.digital.payments.digital_pyments.repositories.ISaleRepository;

/**
 * Prueba de integracion: levanta el contexto completo contra un Postgres real.
 * Se salta automaticamente si Docker no esta disponible (disabledWithoutDocker).
 * Valida que el esquema generado por las entidades
 * (spring.jpa.hibernate.ddl-auto=update) quede listo para operacion.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DigitalPymentsApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private ISaleRepository saleRepository;

	@Test
	void contextLoadsWithSchemaFromEntities() {
		assertThat(saleRepository.count()).isZero();
	}

}
