package br.com.financeiro.identity.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.financeiro.platform.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    protected PasswordResetToken() {
    }

    public static PasswordResetToken criar(UUID userId, String tokenHash) {
        PasswordResetToken t = new PasswordResetToken();
        t.id = Ids.v7();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.createdAt = OffsetDateTime.now();
        // Uma hora. Link de recuperacao que dura um dia e uma chave extra da conta.
        t.expiresAt = t.createdAt.plusHours(1);
        return t;
    }

    public boolean valido() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    public void usar() {
        this.usedAt = OffsetDateTime.now();
    }

    public UUID userId() {
        return userId;
    }
}
