package br.com.financeiro.accounts.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.financeiro.platform.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    /** CHECKING, SAVINGS, CASH, CREDIT_CARD, WALLET. */
    @Column(nullable = false)
    private String type;

    /** BUSINESS, PERSONAL, MIXED — MIXED e a situacao real da maioria. */
    @Column(nullable = false)
    private String ownership;

    @Column(name = "institution_code")
    private String institutionCode;

    @Column(name = "opening_balance_cents", nullable = false)
    private long openingBalanceCents;

    @Column(nullable = false)
    private String currency;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Account() {
    }

    public static Account criar(UUID workspaceId, String nome, String tipo, String posse, long saldoInicialCents) {
        Account a = new Account();
        a.id = Ids.v7();
        a.workspaceId = workspaceId;
        a.name = nome.trim();
        a.type = tipo;
        a.ownership = posse;
        a.openingBalanceCents = saldoInicialCents;
        a.currency = "BRL";
        a.createdAt = OffsetDateTime.now();
        return a;
    }

    public void renomear(String nome) {
        this.name = nome.trim();
    }

    public void mudarPosse(String posse) {
        this.ownership = posse;
    }

    public void arquivar() {
        this.archivedAt = OffsetDateTime.now();
    }

    public UUID id() {
        return id;
    }

    public UUID workspaceId() {
        return workspaceId;
    }

    public String nome() {
        return name;
    }

    public String tipo() {
        return type;
    }

    public String posse() {
        return ownership;
    }

    public long saldoInicialCents() {
        return openingBalanceCents;
    }

    public boolean arquivada() {
        return archivedAt != null;
    }
}
