package br.com.zimbro.accounts.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.zimbro.accounts.domain.Account;

/**
 * Todo metodo carrega workspaceId. Nao existe findById sozinho neste projeto:
 * buscar por id sem o dono e como deixar a porta encostada.
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByWorkspaceIdAndArchivedAtIsNullOrderByName(UUID workspaceId);

    Optional<Account> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
