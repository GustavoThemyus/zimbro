package br.com.financeiro.platform.web;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prova de vida ponta a ponta: o navegador chama o Next, o Next chama o Spring,
 * o Spring chama o Postgres. E o unico proposito deste controller — ele sai do
 * projeto quando a tela de lancamento rapido existir.
 */
@RestController
@RequestMapping("/api")
class PingController {

    private final JdbcClient jdbc;

    PingController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/ping")
    Map<String, Object> ping() {
        OffsetDateTime agora = jdbc.sql("select now()")
                .query(OffsetDateTime.class)
                .single();
        Integer migracoes = jdbc.sql("select count(*) from flyway_schema_history where success")
                .query(Integer.class)
                .single();
        return Map.of(
                "app", "financeiro",
                "horaDoBanco", agora,
                "migracoesAplicadas", migracoes);
    }
}
