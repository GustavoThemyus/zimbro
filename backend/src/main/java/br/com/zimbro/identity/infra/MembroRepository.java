package br.com.zimbro.identity.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * workspace_member tem chave composta e, no MVP, so dois usos. Mapear uma
 * entidade com @EmbeddedId para isso seria mais codigo do que a tabela merece.
 */
@Repository
public class MembroRepository {

    private final JdbcClient jdbc;

    MembroRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(UUID workspaceId, UUID userId, String papel) {
        jdbc.sql("insert into workspace_member (workspace_id, user_id, role) values (?, ?, ?)")
                .params(workspaceId, userId, papel)
                .update();
    }

    public Optional<UUID> workspaceDoUsuario(UUID userId) {
        return jdbc.sql("select workspace_id from workspace_member where user_id = ? order by created_at limit 1")
                .param(userId)
                .query(UUID.class)
                .optional();
    }
}
