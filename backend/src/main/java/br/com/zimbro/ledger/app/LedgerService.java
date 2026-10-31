package br.com.zimbro.ledger.app;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zimbro.accounts.app.AccountService;
import br.com.zimbro.catalog.domain.Category;
import br.com.zimbro.catalog.infra.CategoryRepository;
import br.com.zimbro.ledger.domain.Transaction;
import br.com.zimbro.ledger.infra.TransactionRepository;
import br.com.zimbro.platform.web.ErroDeNegocio;

@Service
public class LedgerService {

    private static final List<String> ESCOPOS = List.of("BUSINESS", "PERSONAL");

    private final TransactionRepository lancamentos;
    private final CategoryRepository categorias;
    private final AccountService contas;

    LedgerService(TransactionRepository lancamentos, CategoryRepository categorias, AccountService contas) {
        this.lancamentos = lancamentos;
        this.categorias = categorias;
        this.contas = contas;
    }

    public record Novo(UUID contaId, LocalDate quando, long valorCents, String direcao,
            String descricao, UUID categoriaId, String escopo) {
    }

    @Transactional
    public Transaction lancar(UUID workspaceId, Novo novo) {
        validar(workspaceId, novo);
        Category categoria = categoria(workspaceId, novo.categoriaId());

        return lancamentos.save(Transaction.lancar(
                workspaceId, novo.contaId(), novo.quando(), novo.valorCents(), novo.direcao(),
                novo.descricao(), novo.categoriaId(), novo.escopo(),
                contaComoFaturamento(categoria, novo)));
    }

    @Transactional
    public Transaction alterar(UUID workspaceId, UUID id, Novo novo) {
        validar(workspaceId, novo);
        Category categoria = categoria(workspaceId, novo.categoriaId());

        Transaction t = buscar(workspaceId, id);
        t.alterar(novo.contaId(), novo.quando(), novo.valorCents(), novo.direcao(),
                novo.descricao(), novo.categoriaId(), novo.escopo(),
                contaComoFaturamento(categoria, novo));
        return t;
    }

    @Transactional
    public void apagar(UUID workspaceId, UUID id) {
        buscar(workspaceId, id).apagar();
    }

    private Transaction buscar(UUID workspaceId, UUID id) {
        return lancamentos.findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Esse lançamento"));
    }

    /**
     * Nem toda entrada e faturamento. Transferencia entre contas proprias,
     * estorno e emprestimo entram dinheiro e nao contam para o teto dos R$ 81 mil.
     * Confundir isso da alarme falso de estouro de limite — e alarme falso em
     * coisa com consequencia legal acaba com a confianca de uma vez.
     */
    private boolean contaComoFaturamento(Category categoria, Novo novo) {
        return "INFLOW".equals(novo.direcao())
                && categoria != null
                && categoria.contaComoFaturamento(novo.escopo());
    }

    private Category categoria(UUID workspaceId, UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categorias.buscar(categoriaId, workspaceId)
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Essa categoria"));
    }

    private void validar(UUID workspaceId, Novo novo) {
        if (novo.valorCents() <= 0) {
            throw ErroDeNegocio.invalido("valor-invalido", "O valor precisa ser maior que zero.");
        }
        if (!"INFLOW".equals(novo.direcao()) && !"OUTFLOW".equals(novo.direcao())) {
            throw ErroDeNegocio.invalido("direcao-invalida", "Diga se entrou ou saiu.");
        }
        if (!ESCOPOS.contains(novo.escopo())) {
            throw ErroDeNegocio.invalido("escopo-invalido", "Diga se é do negócio ou pessoal.");
        }
        if (novo.quando() == null || novo.quando().isAfter(LocalDate.now().plusDays(1))) {
            throw ErroDeNegocio.invalido("data-invalida", "A data não pode estar no futuro.");
        }
        // So aceita conta do proprio workspace. Sem esta linha, um id de conta do
        // vizinho criaria lancamento no lugar errado.
        contas.buscar(workspaceId, novo.contaId());
    }
}
