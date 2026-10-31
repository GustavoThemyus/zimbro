package br.com.financeiro.notifications.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.financeiro.notifications.api.Avisos;

/**
 * Sem SMTP ainda. O link vai para o log, o que basta para desenvolver e testar.
 *
 * Isto e deliberadamente um buraco visivel: antes do primeiro cliente real,
 * trocar por um envio de verdade e obrigatorio — senao quem esquece a senha
 * fica trancado para fora.
 */
@Component
class AvisosPorLog implements Avisos {

    private static final Logger log = LoggerFactory.getLogger(AvisosPorLog.class);

    @Override
    public void recuperacaoDeSenha(String email, String link) {
        log.warn("[SEM E-MAIL CONFIGURADO] Link de recuperação para {}: {}", email, link);
    }
}
