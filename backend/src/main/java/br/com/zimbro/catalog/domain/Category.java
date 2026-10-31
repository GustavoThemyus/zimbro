package br.com.zimbro.catalog.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * workspace_id nulo = categoria de fabrica, compartilhada por todos os clientes.
 * Na semana 1 o catalogo e so leitura; categoria propria do cliente entra junto
 * com o motor de regras.
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    /** INCOME, EXPENSE, TRANSFER, OWNER_DRAW, TAX. */
    @Column(nullable = false)
    private String kind;

    @Column(name = "default_scope", nullable = false)
    private String defaultScope;

    @Column(name = "color_token", nullable = false)
    private String colorToken;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    protected Category() {
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return name;
    }

    public String slug() {
        return slug;
    }

    public String tipo() {
        return kind;
    }

    public String escopoPadrao() {
        return defaultScope;
    }

    public String corToken() {
        return colorToken;
    }

    public int ordem() {
        return sortOrder;
    }

    /** Entrada do negocio conta para o teto anual. Transferencia e estorno, nao. */
    public boolean contaComoFaturamento(String escopo) {
        return "INCOME".equals(kind) && "BUSINESS".equals(escopo);
    }
}
