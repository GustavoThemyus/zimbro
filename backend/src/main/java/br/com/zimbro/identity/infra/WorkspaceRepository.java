package br.com.zimbro.identity.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.zimbro.identity.domain.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
}
