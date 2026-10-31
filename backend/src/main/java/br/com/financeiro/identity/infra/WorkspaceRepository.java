package br.com.financeiro.identity.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.financeiro.identity.domain.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
}
