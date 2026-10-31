package br.com.zimbro.catalog.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zimbro.catalog.domain.Category;
import br.com.zimbro.catalog.infra.CategoryRepository;
import br.com.zimbro.platform.tenant.Sessao;

@RestController
@RequestMapping("/api/categorias")
class CategoryController {

    private final CategoryRepository categorias;

    CategoryController(CategoryRepository categorias) {
        this.categorias = categorias;
    }

    record CategoriaResposta(UUID id, String nome, String slug, String tipo,
            String escopoPadrao, String cor) {

        static CategoriaResposta de(Category c) {
            return new CategoriaResposta(c.id(), c.nome(), c.slug(), c.tipo(),
                    c.escopoPadrao(), c.corToken());
        }
    }

    @GetMapping
    List<CategoriaResposta> listar(Sessao sessao) {
        return categorias.doWorkspace(sessao.workspaceId()).stream()
                .map(CategoriaResposta::de)
                .toList();
    }
}
