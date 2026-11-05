package br.com.zimbro;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * O /actuator/health e o que o healthcheck do container e o UptimeRobot olham.
 * Se ele devolve DOWN, o Compose marca o backend como 'unhealthy' e o alerta
 * dispara — ainda que o produto esteja funcionando.
 *
 * Este teste existe por causa de uma regressao real: ao adicionar o
 * spring-boot-starter-mail, o MailHealthIndicator do Boot entrou sozinho no
 * /health e passou a tentar conectar no SMTP a cada checagem. Sem credencial
 * configurada (o estado normal em desenvolvimento) o health virou 503 DOWN.
 *
 * A suite nao pegou isso porque nenhum teste batia no /health. Agora pega.
 *
 * Usa o cliente HTTP do JDK de proposito: e o mesmo que o 'wget' do healthcheck
 * do compose faz, e nao depende de classe de teste do Spring que troca de pacote
 * entre versoes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SaudeTest {

    @Value("${local.server.port}")
    int porta;

    @Test
    @DisplayName("Sem SMTP configurado, o /health continua UP")
    void saudeNaoDependeDeEmail() throws Exception {
        HttpResponse<String> resposta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + porta + "/actuator/health"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode())
                .as("503 aqui deixa o container 'unhealthy' e dispara alerta a toa")
                .isEqualTo(200);
        assertThat(resposta.body()).contains("\"status\":\"UP\"");
    }
}
