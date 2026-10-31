package br.com.financeiro.ledger.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.financeiro.platform.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * O evento central do produto.
 *
 * Valor sempre positivo em centavos, com a direcao num campo proprio. Valor com
 * sinal encurta a soma e abre a porta para o bug de sinal invertido, que e o
 * mais comum em financas e o mais dificil de notar — o numero errado parece
 * plausivel. A coluna gerada signed_amount_cents, calculada pelo banco, da a
 * soma curta de graca para os relatorios.
 */
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    /** INFLOW ou OUTFLOW. */
    @Column(nullable = false)
    private String direction;

    @Column(name = "description_raw")
    private String descriptionRaw;

    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    /** BUSINESS, PERSONAL ou SPLIT. A separacao central do produto. */
    @Column(nullable = false)
    private String scope;

    @Column(name = "counts_as_revenue", nullable = false)
    private boolean countsAsRevenue;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String source;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "categorization_confidence")
    private Short categorizationConfidence;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Transaction() {
    }

    public static Transaction lancar(UUID workspaceId, UUID accountId, LocalDate quando,
            long valorCents, String direcao, String descricao, UUID categoriaId,
            String escopo, boolean contaComoFaturamento) {
        Transaction t = new Transaction();
        t.id = Ids.v7();
        t.workspaceId = workspaceId;
        t.accountId = accountId;
        t.occurredOn = quando;
        t.amountCents = valorCents;
        t.direction = direcao;
        t.description = descricao;
        t.descriptionRaw = descricao;
        t.categoryId = categoriaId;
        t.scope = escopo;
        t.countsAsRevenue = contaComoFaturamento;
        t.status = "CONFIRMED";
        t.source = "MANUAL";
        // Lancado a mao pela pessoa: confianca 100 e decisao do usuario. Isso
        // congela o lancamento contra reclassificacao automatica na semana 3.
        t.decidedBy = "USER";
        t.decidedAt = OffsetDateTime.now();
        t.categorizationConfidence = 100;
        t.createdAt = OffsetDateTime.now();
        t.updatedAt = t.createdAt;
        return t;
    }

    public void alterar(UUID accountId, LocalDate quando, long valorCents, String direcao,
            String descricao, UUID categoriaId, String escopo, boolean contaComoFaturamento) {
        this.accountId = accountId;
        this.occurredOn = quando;
        this.amountCents = valorCents;
        this.direction = direcao;
        this.description = descricao;
        this.categoryId = categoriaId;
        this.scope = escopo;
        this.countsAsRevenue = contaComoFaturamento;
        this.decidedBy = "USER";
        this.decidedAt = OffsetDateTime.now();
        this.categorizationConfidence = 100;
    }

    /** Apagar e marcar a data: o desfazer da semana 1 e so limpar este campo. */
    public void apagar() {
        this.deletedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void aoAtualizar() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID id() {
        return id;
    }

    public UUID workspaceId() {
        return workspaceId;
    }

    public boolean apagado() {
        return deletedAt != null;
    }
}
