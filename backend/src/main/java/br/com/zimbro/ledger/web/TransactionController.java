package br.com.zimbro.ledger.web;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zimbro.ledger.app.LedgerService;
import br.com.zimbro.ledger.domain.Transaction;
import br.com.zimbro.platform.tenant.Sessao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/lancamentos")
class TransactionController {

    private final LedgerService ledger;

    TransactionController(LedgerService ledger) {
        this.ledger = ledger;
    }

    record NovoLancamento(
            @NotNull(message = "Escolha a conta.") UUID contaId,
            @NotNull(message = "Qual o dia?") LocalDate quando,
            long valorCents,
            @NotNull String direcao,
            String descricao,
            UUID categoriaId,
            @NotNull String escopo) {

        LedgerService.Novo paraServico() {
            return new LedgerService.Novo(contaId, quando, valorCents, direcao, descricao, categoriaId, escopo);
        }
    }

    record LancamentoCriado(UUID id) {
    }

    @PostMapping
    LancamentoCriado lancar(Sessao sessao, @Valid @RequestBody NovoLancamento r) {
        Transaction t = ledger.lancar(sessao.workspaceId(), r.paraServico());
        return new LancamentoCriado(t.id());
    }

    @PutMapping("/{id}")
    LancamentoCriado alterar(Sessao sessao, @PathVariable UUID id, @Valid @RequestBody NovoLancamento r) {
        Transaction t = ledger.alterar(sessao.workspaceId(), id, r.paraServico());
        return new LancamentoCriado(t.id());
    }

    @DeleteMapping("/{id}")
    void apagar(Sessao sessao, @PathVariable UUID id) {
        ledger.apagar(sessao.workspaceId(), id);
    }
}
