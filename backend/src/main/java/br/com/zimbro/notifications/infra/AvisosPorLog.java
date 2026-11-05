package br.com.zimbro.notifications.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.zimbro.notifications.api.Avisos;

/**
 * Modo de desenvolvimento: o link vai para o log em vez de ir por e-mail.
 *
 * Isto e deliberadamente um buraco visivel, e ele grita no startup. Antes do
 * primeiro cliente real, {@code zimbro.email.modo=smtp} e obrigatorio — senao
 * quem esquece a senha fica trancado para fora.
 */
@Component
@ConditionalOnProperty(name = "zimbro.email.modo", havingValue = "log", matchIfMissing = true)
class AvisosPorLog implements Avisos {

    private static final Logger log = LoggerFactory.getLogger(AvisosPorLog.class);

    AvisosPorLog() {
        log.warn("E-mail em modo de log (zimbro.email.modo=log). Nenhuma mensagem sai daqui. "
                + "Nao suba isto com cliente real: quem esquecer a senha fica trancado fora.");
    }

    @Override
    public void recuperacaoDeSenha(String email, String link) {
        log.warn("[SEM E-MAIL CONFIGURADO] Link de recuperação para {}: {}", email, link);
    }
}
