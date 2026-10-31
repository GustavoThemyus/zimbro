package br.com.zimbro.reporting.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Leitura do mes. SQL direto, sem montar em Java: o fechamento e uma soma, e
 * soma e o que banco faz melhor. Se este numero mentir, nada mais importa.
 */
@Repository
public class MesRepository {

    private final JdbcClient jdbc;

    MesRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Resumo(long entrouCents, long saiuCents, long retiradaCents,
            long pessoalCents, long sobrouCents) {
    }

    public record Linha(UUID id, LocalDate quando, long valorCents, String direcao,
            String descricao, String escopo, UUID categoriaId, String categoria,
            String cor, String tipoCategoria, UUID contaId, String conta) {
    }

    public Resumo resumo(UUID workspaceId, LocalDate inicio, LocalDate fim) {
        record Bruto(long entrou, long saiu, long retirada, long pessoal) {
        }

        Bruto b = jdbc.sql("""
                select
                  coalesce(sum(t.amount_cents) filter (
                      where t.direction = 'INFLOW' and t.scope = 'BUSINESS'
                        and coalesce(c.kind, 'EXPENSE') <> 'TRANSFER'), 0) as entrou,
                  coalesce(sum(t.amount_cents) filter (
                      where t.direction = 'OUTFLOW' and t.scope = 'BUSINESS'
                        and coalesce(c.kind, 'EXPENSE') not in ('TRANSFER', 'OWNER_DRAW')), 0) as saiu,
                  coalesce(sum(t.amount_cents) filter (
                      where coalesce(c.kind, '') = 'OWNER_DRAW'), 0) as retirada,
                  coalesce(sum(t.amount_cents) filter (
                      where t.direction = 'OUTFLOW' and t.scope = 'PERSONAL'
                        and coalesce(c.kind, 'EXPENSE') <> 'TRANSFER'), 0) as pessoal
                  from transaction t
                  left join category c on c.id = t.category_id
                 where t.workspace_id = :ws
                   and t.deleted_at is null
                   and t.occurred_on between :inicio and :fim
                """)
                .param("ws", workspaceId)
                .param("inicio", inicio)
                .param("fim", fim)
                .query(Bruto.class)
                .single();

        // Sobrou = o que o negocio gerou menos o que ele gastou menos o que o dono
        // ja tirou. A retirada do dono NUNCA entra nas despesas do negocio: se
        // entrasse, "quanto sobrou" misturaria o que o dono levou com o que o
        // negocio consumiu, e essa e a pergunta numero um do produto.
        long sobrou = b.entrou() - b.saiu() - b.retirada();
        return new Resumo(b.entrou(), b.saiu(), b.retirada(), b.pessoal(), sobrou);
    }

    public List<Linha> lancamentos(UUID workspaceId, LocalDate inicio, LocalDate fim, String escopo) {
        String filtro = escopo == null ? "" : " and t.scope = :escopo";

        var consulta = jdbc.sql("""
                select t.id,
                       t.occurred_on   as quando,
                       t.amount_cents  as valor_cents,
                       t.direction     as direcao,
                       t.description   as descricao,
                       t.scope         as escopo,
                       t.category_id   as categoria_id,
                       c.name          as categoria,
                       c.color_token   as cor,
                       c.kind          as tipo_categoria,
                       t.account_id    as conta_id,
                       a.name          as conta
                  from transaction t
                  join account a on a.id = t.account_id
                  left join category c on c.id = t.category_id
                 where t.workspace_id = :ws
                   and t.deleted_at is null
                   and t.occurred_on between :inicio and :fim
                """ + filtro + """
                 order by t.occurred_on desc, t.created_at desc
                """)
                .param("ws", workspaceId)
                .param("inicio", inicio)
                .param("fim", fim);

        if (escopo != null) {
            consulta = consulta.param("escopo", escopo);
        }
        return consulta.query(Linha.class).list();
    }
}
