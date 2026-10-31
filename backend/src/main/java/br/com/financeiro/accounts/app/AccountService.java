package br.com.financeiro.accounts.app;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.financeiro.accounts.api.ContasApi;
import br.com.financeiro.accounts.domain.Account;
import br.com.financeiro.accounts.infra.AccountRepository;
import br.com.financeiro.platform.web.ErroDeNegocio;

@Service
public class AccountService implements ContasApi {

    private static final List<String> TIPOS = List.of("CHECKING", "SAVINGS", "CASH", "CREDIT_CARD", "WALLET");
    private static final List<String> POSSES = List.of("BUSINESS", "PERSONAL", "MIXED");

    private final AccountRepository contas;

    AccountService(AccountRepository contas) {
        this.contas = contas;
    }

    @Override
    @Transactional
    public void criarContasIniciais(UUID workspaceId) {
        // Duas contas, nenhuma pergunta. Dinheiro vivo e conta de primeira classe:
        // barbearia e confeitaria recebem muito em especie, e produto que trata
        // dinheiro na mao como caso especial nao serve a esse publico.
        contas.save(Account.criar(workspaceId, "Conta do banco", "CHECKING", "MIXED", 0));
        contas.save(Account.criar(workspaceId, "Dinheiro", "CASH", "MIXED", 0));
    }

    @Transactional(readOnly = true)
    public List<Account> listar(UUID workspaceId) {
        return contas.findByWorkspaceIdAndArchivedAtIsNullOrderByName(workspaceId);
    }

    @Transactional
    public Account criar(UUID workspaceId, String nome, String tipo, String posse, long saldoInicialCents) {
        validar(tipo, posse);
        return contas.save(Account.criar(workspaceId, nome, tipo, posse, saldoInicialCents));
    }

    @Transactional
    public Account alterar(UUID workspaceId, UUID id, String nome, String posse) {
        Account conta = buscar(workspaceId, id);
        conta.renomear(nome);
        if (posse != null) {
            validar(conta.tipo(), posse);
            conta.mudarPosse(posse);
        }
        return conta;
    }

    @Transactional
    public void arquivar(UUID workspaceId, UUID id) {
        // Arquivar, nunca apagar: os lancamentos antigos continuam apontando para ela.
        buscar(workspaceId, id).arquivar();
    }

    public Account buscar(UUID workspaceId, UUID id) {
        return contas.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> ErroDeNegocio.naoEncontrado("Essa conta"));
    }

    private void validar(String tipo, String posse) {
        if (!TIPOS.contains(tipo)) {
            throw ErroDeNegocio.invalido("tipo-invalido", "Tipo de conta desconhecido.");
        }
        if (!POSSES.contains(posse)) {
            throw ErroDeNegocio.invalido("posse-invalida", "Diga se a conta é do negócio, pessoal ou das duas coisas.");
        }
    }
}
