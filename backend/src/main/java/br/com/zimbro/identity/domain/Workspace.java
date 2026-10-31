package br.com.zimbro.identity.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.zimbro.platform.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace")
public class Workspace {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String cnpj;

    /** Data de abertura do MEI: no ano de abertura o limite anual e proporcional. */
    @Column(name = "mei_opened_on")
    private LocalDate meiOpenedOn;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(nullable = false)
    private String timezone;

    /** Cobranca manual via Pix: atualizada a mao depois do pagamento. */
    @Column(name = "paid_until")
    private LocalDate paidUntil;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Workspace() {
    }

    public static Workspace criar(String nome, String tipoDeAtividade) {
        Workspace w = new Workspace();
        w.id = Ids.v7();
        w.name = nome.trim();
        w.activityType = tipoDeAtividade;
        w.timezone = "America/Sao_Paulo";
        // 14 dias de teste. Depois disso o Pix entra na conversa.
        w.paidUntil = LocalDate.now().plusDays(14);
        w.createdAt = OffsetDateTime.now();
        return w;
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return name;
    }

    public LocalDate pagoAte() {
        return paidUntil;
    }
}
