package br.com.zimbro.catalog.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.zimbro.catalog.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** As de fabrica mais as do proprio workspace. Nunca as de outro cliente. */
    @Query("""
            select c from Category c
             where c.archivedAt is null
               and (c.workspaceId is null or c.workspaceId = :workspaceId)
             order by c.sortOrder, c.name
            """)
    List<Category> doWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("""
            select c from Category c
             where c.id = :id
               and (c.workspaceId is null or c.workspaceId = :workspaceId)
            """)
    Optional<Category> buscar(@Param("id") UUID id, @Param("workspaceId") UUID workspaceId);
}
