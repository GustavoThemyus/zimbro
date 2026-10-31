package br.com.zimbro.accounts.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zimbro.accounts.app.AccountService;
import br.com.zimbro.accounts.domain.Account;
import br.com.zimbro.platform.tenant.Sessao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/contas")
class AccountController {

    private final AccountService contas;

    AccountController(AccountService contas) {
        this.contas = contas;
    }

    record ContaResposta(UUID id, String nome, String tipo, String posse, long saldoInicialCents) {

        static ContaResposta de(Account c) {
            return new ContaResposta(c.id(), c.nome(), c.tipo(), c.posse(), c.saldoInicialCents());
        }
    }

    @GetMapping
    List<ContaResposta> listar(Sessao sessao) {
        return contas.listar(sessao.workspaceId()).stream().map(ContaResposta::de).toList();
    }

    record NovaConta(@NotBlank(message = "Dê um nome para a conta.") String nome,
            @NotBlank String tipo, @NotBlank String posse, long saldoInicialCents) {
    }

    @PostMapping
    ContaResposta criar(Sessao sessao, @Valid @RequestBody NovaConta r) {
        return ContaResposta.de(
                contas.criar(sessao.workspaceId(), r.nome(), r.tipo(), r.posse(), r.saldoInicialCents()));
    }

    record AlterarConta(@NotBlank String nome, String posse) {
    }

    @PatchMapping("/{id}")
    ContaResposta alterar(Sessao sessao, @PathVariable UUID id, @Valid @RequestBody AlterarConta r) {
        return ContaResposta.de(contas.alterar(sessao.workspaceId(), id, r.nome(), r.posse()));
    }

    @DeleteMapping("/{id}")
    void arquivar(Sessao sessao, @PathVariable UUID id) {
        contas.arquivar(sessao.workspaceId(), id);
    }
}
