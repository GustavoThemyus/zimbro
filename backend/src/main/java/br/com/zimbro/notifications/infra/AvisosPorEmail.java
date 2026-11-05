package br.com.zimbro.notifications.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import br.com.zimbro.notifications.api.Avisos;

/**
 * Envio real, por SMTP. Entra no lugar do {@link AvisosPorLog} quando
 * {@code zimbro.email.modo=smtp}.
 *
 * Duas decisoes aqui:
 *
 * Texto puro, sem HTML. Um link de recuperacao nao precisa de layout, e HTML mal
 * montado e um dos motivos classicos de cair em spam. Quando houver digesto de
 * pendencias (semana 4) vale revisitar.
 *
 * Falha de envio nao derruba o caso de uso. A chamada acontece dentro da
 * transacao que gravou o token de recuperacao; se o SMTP estiver fora do ar, o
 * token continua valido e o erro vira log + Sentry, em vez de o usuario receber
 * 500 e nenhum e-mail. O custo e que o usuario pede outro link; a alternativa
 * seria desfazer a gravacao, que nao melhora nada para ele.
 */
@Component
@ConditionalOnProperty(name = "zimbro.email.modo", havingValue = "smtp")
class AvisosPorEmail implements Avisos {

    private static final Logger log = LoggerFactory.getLogger(AvisosPorEmail.class);

    private final JavaMailSender correio;
    private final String remetente;

    AvisosPorEmail(JavaMailSender correio,
            @Value("${zimbro.email.remetente:}") String remetente,
            @Value("${spring.mail.host:}") String servidor) {
        // Fail-fast. O @ConditionalOnProperty do Spring trata 'spring.mail.host='
        // como presente, entao sem esta checagem o app subiria com host vazio e a
        // falha so apareceria no primeiro envio — possivelmente semanas depois,
        // no primeiro cliente que esquecesse a senha. Erro de configuracao tem que
        // quebrar o deploy.
        if (servidor.isBlank()) {
            throw new IllegalStateException(
                    "zimbro.email.modo=smtp mas spring.mail.host esta vazio (variavel SMTP_HOST). "
                            + "Configure o servidor de e-mail ou volte para EMAIL_MODO=log.");
        }
        if (remetente.isBlank()) {
            throw new IllegalStateException(
                    "zimbro.email.modo=smtp mas zimbro.email.remetente esta vazio (variavel EMAIL_REMETENTE).");
        }
        this.correio = correio;
        this.remetente = remetente;
        log.info("E-mail em modo SMTP via {}, remetente {}", servidor, remetente);
    }

    @Override
    public void recuperacaoDeSenha(String email, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Recuperar sua senha do Zimbro");
        mensagem.setText("""
                Alguém pediu uma nova senha para esta conta do Zimbro.

                Para escolher uma senha nova, abra este link:

                %s

                O link vale por uma hora e só pode ser usado uma vez.

                Se não foi você que pediu, pode ignorar esta mensagem — nada muda
                na sua conta e sua senha atual continua valendo.

                — Zimbro
                """.formatted(link));

        try {
            correio.send(mensagem);
            log.info("Link de recuperação enviado para {}", email);
        } catch (MailException e) {
            // Nao relanca: ver o javadoc da classe.
            log.error("Falha ao enviar link de recuperação para {}. O token continua valido; "
                    + "o usuario pode pedir outro.", email, e);
        }
    }
}
