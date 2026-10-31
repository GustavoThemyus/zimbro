package br.com.zimbro.identity.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.zimbro.platform.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    protected AppUser() {
    }

    public static AppUser criar(String email, String passwordHash, String nome, String telefone) {
        AppUser u = new AppUser();
        u.id = Ids.v7();
        // Guardado sempre em minusculas: o indice unico e sobre lower(email), e
        // ninguem perde a conta por ter digitado Joao@ em vez de joao@.
        u.email = email.trim().toLowerCase();
        u.passwordHash = passwordHash;
        u.name = nome.trim();
        u.phone = telefone;
        u.createdAt = OffsetDateTime.now();
        return u;
    }

    public void marcarLogin() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void trocarSenha(String novoHash) {
        this.passwordHash = novoHash;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String nome() {
        return name;
    }

    public String passwordHash() {
        return passwordHash;
    }
}
