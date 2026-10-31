package br.com.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Os tres testes da semana 0. Cada um prova uma promessa do plano, nao uma
 * linha de codigo.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FundacaoTest {

    @Autowired
    JdbcClient jdbc;

    @Test
    @DisplayName("O Flyway aplica as migracoes num banco vazio")
    void migracoesAplicam() {
        Integer aplicadas = jdbc.sql("select count(*) from flyway_schema_history where success")
                .query(Integer.class)
                .single();
        assertThat(aplicadas).isGreaterThanOrEqualTo(2);

        Integer falhas = jdbc.sql("select count(*) from flyway_schema_history where not success")
                .query(Integer.class)
                .single();
        assertThat(falhas).isZero();
    }

    @Test
    @DisplayName("O limite anual do MEI vem do banco, nao do codigo")
    void limiteAnualVemDoBanco() {
        Long centavos = parametro("mei.annual_revenue_limit", LocalDate.of(2026, 6, 15));
        assertThat(centavos).isEqualTo(8_100_000L);
    }

    @Test
    @DisplayName("O banco recusa dois valores da mesma chave vigentes no mesmo dia")
    void naoExistemDoisValoresVigentes() {
        assertThatThrownBy(() -> jdbc.sql("""
                insert into legal_parameter (param_key, valid_from, valid_to, value, source_note)
                values ('mei.annual_revenue_limit', '2026-07-01', null, '{"cents": 9900000}', 'valor concorrente')
                """).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Le o valor vigente de uma chave numa data — o jeito como todo calculo vai ler. */
    private Long parametro(String chave, LocalDate em) {
        return jdbc.sql("""
                select (value ->> 'cents')::bigint
                  from legal_parameter
                 where param_key = :chave
                   and valid_from <= :em
                   and (valid_to is null or valid_to >= :em)
                """)
                .param("chave", chave)
                .param("em", em)
                .query(Long.class)
                .single();
    }
}
