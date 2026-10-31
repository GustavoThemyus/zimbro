package br.com.zimbro;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres de verdade num container durante o teste. Banco em memoria mente
 * sobre SQL — e aqui o SQL e o produto. A versao e fixa de proposito: 'latest'
 * faz o teste passar hoje e quebrar amanha sem ninguem ter mexido em nada.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

}
