package br.com.zimbro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import br.com.zimbro.notifications.api.Avisos;

/**
 * O que estes testes protegem: a recuperacao de senha hoje e a unica saida de
 * quem esquece a senha, e ela depende de o adaptador certo ser escolhido pela
 * configuracao. Um erro aqui nao da erro em nenhuma tela — simplesmente nenhum
 * e-mail sai, e isso so se descobre pelo cliente.
 *
 * Sem contexto completo nem banco: so o pedaco que decide quem envia.
 */
class EnvioDeEmailTest {

    /**
     * Um JavaMailSender que nao fala com servidor nenhum: guarda o que seria
     * enviado para o teste olhar.
     */
    static class CorreioDeMentira extends JavaMailSenderImpl {
        final List<SimpleMailMessage> enviadas = new ArrayList<>();

        @Override
        public void send(SimpleMailMessage... mensagens) {
            enviadas.addAll(List.of(mensagens));
        }
    }

    /** Um que sempre falha, para provar que a falha nao sobe para o caso de uso. */
    static class CorreioQuebrado extends JavaMailSenderImpl {
        @Override
        public void send(SimpleMailMessage... mensagens) {
            throw new MailException("servidor fora do ar") {
            };
        }
    }

    @Configuration
    @ComponentScan("br.com.zimbro.notifications")
    static class SoNotifications {
    }

    @Configuration
    static class ComCorreioDeMentira {
        @Bean
        JavaMailSender javaMailSender() {
            return new CorreioDeMentira();
        }
    }

    @Configuration
    static class ComCorreioQuebrado {
        @Bean
        JavaMailSender javaMailSender() {
            return new CorreioQuebrado();
        }
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(SoNotifications.class);

    @Test
    @DisplayName("Sem configuracao, o escolhido e o adaptador de log")
    void padraoEhLog() {
        contexto.run(ctx -> assertThat(ctx).getBean(Avisos.class)
                .extracting(a -> a.getClass().getSimpleName())
                .isEqualTo("AvisosPorLog"));
    }

    @Test
    @DisplayName("Com modo=smtp, o escolhido e o adaptador de SMTP — e so ele")
    void smtpSubstituiOLog() {
        contexto.withUserConfiguration(ComCorreioDeMentira.class)
                .withPropertyValues(
                        "zimbro.email.modo=smtp",
                        "spring.mail.host=smtp.exemplo.com",
                        "zimbro.email.remetente=Zimbro <nao-responda@exemplo.com.br>")
                .run(ctx -> {
                    assertThat(ctx).getBeans(Avisos.class).hasSize(1);
                    assertThat(ctx).getBean(Avisos.class)
                            .extracting(a -> a.getClass().getSimpleName())
                            .isEqualTo("AvisosPorEmail");
                });
    }

    @Test
    @DisplayName("modo=smtp sem SMTP_HOST derruba o app no startup, em vez de voltar calado para o log")
    void smtpSemHostNaoSobe() {
        contexto.withUserConfiguration(ComCorreioDeMentira.class)
                .withPropertyValues(
                        "zimbro.email.modo=smtp",
                        "spring.mail.host=",
                        "zimbro.email.remetente=Zimbro <nao-responda@exemplo.com.br>")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("SMTP_HOST"));
    }

    @Test
    @DisplayName("modo=smtp sem remetente tambem derruba o app no startup")
    void smtpSemRemetenteNaoSobe() {
        contexto.withUserConfiguration(ComCorreioDeMentira.class)
                .withPropertyValues(
                        "zimbro.email.modo=smtp",
                        "spring.mail.host=smtp.exemplo.com",
                        "zimbro.email.remetente=")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("EMAIL_REMETENTE"));
    }

    @Test
    @DisplayName("A mensagem leva o link, o destinatario e o remetente configurado")
    void mensagemTemOLink() {
        contexto.withUserConfiguration(ComCorreioDeMentira.class)
                .withPropertyValues(
                        "zimbro.email.modo=smtp",
                        "spring.mail.host=smtp.exemplo.com",
                        "zimbro.email.remetente=Zimbro <nao-responda@exemplo.com.br>")
                .run(ctx -> {
                    ctx.getBean(Avisos.class).recuperacaoDeSenha(
                            "barbeiro@exemplo.com",
                            "https://app.exemplo.com.br/nova-senha?token=abc123");

                    CorreioDeMentira correio = (CorreioDeMentira) ctx.getBean(JavaMailSender.class);
                    assertThat(correio.enviadas).hasSize(1);
                    SimpleMailMessage mensagem = correio.enviadas.getFirst();
                    assertThat(mensagem.getTo()).containsExactly("barbeiro@exemplo.com");
                    assertThat(mensagem.getFrom()).isEqualTo("Zimbro <nao-responda@exemplo.com.br>");
                    assertThat(mensagem.getSubject()).contains("senha");
                    assertThat(mensagem.getText())
                            .contains("https://app.exemplo.com.br/nova-senha?token=abc123");
                });
    }

    @Test
    @DisplayName("SMTP fora do ar nao estoura para quem pediu a recuperacao")
    void falhaDeEnvioNaoSobe() {
        contexto.withUserConfiguration(ComCorreioQuebrado.class)
                .withPropertyValues(
                        "zimbro.email.modo=smtp",
                        "spring.mail.host=smtp.exemplo.com",
                        "zimbro.email.remetente=Zimbro <nao-responda@exemplo.com.br>")
                .run(ctx -> {
                    Avisos avisos = ctx.getBean(Avisos.class);
                    // O token de recuperacao ja foi gravado quando esta chamada acontece.
                    // Estourar aqui daria 500 na tela e nenhum e-mail; engolir deixa o
                    // token valido e o usuario pede outro link.
                    assertThatNoException().isThrownBy(
                            () -> avisos.recuperacaoDeSenha("barbeiro@exemplo.com", "https://exemplo/x"));
                });
    }
}
