package br.com.zimbro.identity.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.zimbro.platform.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sessao opaca: o token vive no cookie httpOnly que o Next escreve, e aqui so
 * fica o hash dele. Banco vazado nao vira chave de entrada.
 */
@Entity
@Table(name = "user_session")
public class UserSession {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected UserSession() {
    }

    public static UserSession criar(UUID userId, String tokenHash, int diasDeValidade) {
        UserSession s = new UserSession();
        s.id = Ids.v7();
        s.userId = userId;
        s.tokenHash = tokenHash;
        s.createdAt = OffsetDateTime.now();
        s.lastSeenAt = s.createdAt;
        s.expiresAt = s.createdAt.plusDays(diasDeValidade);
        return s;
    }

    public boolean valida() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    public void revogar() {
        this.revokedAt = OffsetDateTime.now();
    }

    public void vista() {
        this.lastSeenAt = OffsetDateTime.now();
    }

    public UUID userId() {
        return userId;
    }
}
