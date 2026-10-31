package br.com.financeiro.identity.app;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.financeiro.accounts.api.ContasApi;
import br.com.financeiro.identity.domain.AppUser;
import br.com.financeiro.identity.domain.PasswordResetToken;
import br.com.financeiro.identity.domain.UserSession;
import br.com.financeiro.identity.domain.Workspace;
import br.com.financeiro.identity.infra.AppUserRepository;
import br.com.financeiro.identity.infra.MembroRepository;
import br.com.financeiro.identity.infra.PasswordResetTokenRepository;
import br.com.financeiro.identity.infra.UserSessionRepository;
import br.com.financeiro.identity.infra.WorkspaceRepository;
import br.com.financeiro.notifications.api.Avisos;
import br.com.financeiro.platform.Tokens;
import br.com.financeiro.platform.tenant.Sessao;
import br.com.financeiro.platform.web.ErroDeNegocio;

@Service
public class IdentityService {

    private static final List<String> ATIVIDADES =
            List.of("COMERCIO", "SERVICOS", "COMERCIO_SERVICOS", "TRANSPORTE");
    private static final int DIAS_DE_SESSAO = 60;
    private static final int MINIMO_DE_SENHA = 8;

    private final AppUserRepository usuarios;
    private final WorkspaceRepository workspaces;
    private final MembroRepository membros;
    private final UserSessionRepository sessoes;
    private final PasswordResetTokenRepository recuperacoes;
    private final ContasApi contas;
    private final Avisos avisos;
    private final PasswordEncoder senhas = new BCryptPasswordEncoder();
    private final String enderecoDoApp;

    IdentityService(AppUserRepository usuarios, WorkspaceRepository workspaces, MembroRepository membros,
            UserSessionRepository sessoes, PasswordResetTokenRepository recuperacoes,
            ContasApi contas, Avisos avisos,
            @Value("${app.endereco:http://localhost:3000}") String enderecoDoApp) {
        this.usuarios = usuarios;
        this.workspaces = workspaces;
        this.membros = membros;
        this.sessoes = sessoes;
        this.recuperacoes = recuperacoes;
        this.contas = contas;
        this.avisos = avisos;
        this.enderecoDoApp = enderecoDoApp;
    }

    public record Cadastro(String email, String senha, String nome, String nomeDoNegocio, String atividade) {
    }

    public record Entrada(String token, String nome, String negocio, LocalDate pagoAte) {
    }

    @Transactional
    public Entrada cadastrar(Cadastro c) {
        if (c.senha() == null || c.senha().length() < MINIMO_DE_SENHA) {
            throw ErroDeNegocio.invalido("senha-curta", "A senha precisa ter pelo menos 8 letras ou números.");
        }
        if (!ATIVIDADES.contains(c.atividade())) {
            throw ErroDeNegocio.invalido("atividade-invalida", "Escolha o tipo da sua atividade.");
        }
        if (usuarios.porEmail(c.email()).isPresent()) {
            throw new ErroDeNegocio(HttpStatus.CONFLICT, "email-em-uso",
                    "Já existe uma conta com esse e-mail. Tente entrar.");
        }

        // saveAndFlush, nao save: a insercao do membro e feita com SQL direto, e o
        // JPA so manda os inserts dele no fim da transacao. Sem o flush aqui, o
        // insert de workspace_member chega antes do workspace existir e a chave
        // estrangeira recusa. E a armadilha classica de misturar JPA com SQL.
        AppUser usuario = usuarios.saveAndFlush(
                AppUser.criar(c.email(), senhas.encode(c.senha()), c.nome(), null));
        Workspace workspace = workspaces.saveAndFlush(Workspace.criar(c.nomeDoNegocio(), c.atividade()));
        membros.inserir(workspace.id(), usuario.id(), "OWNER");
        contas.criarContasIniciais(workspace.id());

        return new Entrada(abrirSessao(usuario), usuario.nome(), workspace.nome(), workspace.pagoAte());
    }

    @Transactional
    public Entrada entrar(String email, String senha) {
        AppUser usuario = usuarios.porEmail(email)
                // Mesma mensagem para e-mail inexistente e senha errada: dizer qual
                // dos dois falhou entrega a lista de quem tem conta aqui.
                .orElseThrow(() -> new ErroDeNegocio(HttpStatus.UNAUTHORIZED, "credenciais",
                        "E-mail ou senha não conferem."));

        if (!senhas.matches(senha, usuario.passwordHash())) {
            throw new ErroDeNegocio(HttpStatus.UNAUTHORIZED, "credenciais", "E-mail ou senha não conferem.");
        }

        usuario.marcarLogin();
        Workspace workspace = workspaces.findById(workspaceDe(usuario.id())).orElseThrow();
        return new Entrada(abrirSessao(usuario), usuario.nome(), workspace.nome(), workspace.pagoAte());
    }

    @Transactional
    public void sair(String token) {
        sessoes.findByTokenHash(Tokens.hash(token)).ifPresent(UserSession::revogar);
    }

    /** Chamado a cada requisicao pelo interceptor. */
    @Transactional
    public Sessao resolver(String token) {
        UserSession sessao = sessoes.findByTokenHash(Tokens.hash(token))
                .filter(UserSession::valida)
                .orElseThrow(ErroDeNegocio::naoAutenticado);
        sessao.vista();
        return new Sessao(sessao.userId(), workspaceDe(sessao.userId()));
    }

    @Transactional
    public void pedirRecuperacao(String email) {
        // Responde igual mesmo se o e-mail nao existir: a tela nao pode servir de
        // teste de "essa pessoa e cliente?".
        usuarios.porEmail(email).ifPresent(usuario -> {
            String token = Tokens.gerar();
            recuperacoes.save(PasswordResetToken.criar(usuario.id(), Tokens.hash(token)));
            avisos.recuperacaoDeSenha(usuario.email(), enderecoDoApp + "/nova-senha?token=" + token);
        });
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < MINIMO_DE_SENHA) {
            throw ErroDeNegocio.invalido("senha-curta", "A senha precisa ter pelo menos 8 letras ou números.");
        }
        PasswordResetToken pedido = recuperacoes.findByTokenHash(Tokens.hash(token))
                .filter(PasswordResetToken::valido)
                .orElseThrow(() -> ErroDeNegocio.invalido("link-expirado",
                        "Esse link não vale mais. Peça outro."));

        AppUser usuario = usuarios.findById(pedido.userId()).orElseThrow();
        usuario.trocarSenha(senhas.encode(novaSenha));
        pedido.usar();
        // Trocar a senha derruba as sessoes abertas: se alguem estava dentro, sai.
        sessoes.findAll().stream()
                .filter(s -> s.userId().equals(usuario.id()))
                .forEach(UserSession::revogar);
    }

    public record Perfil(String nome, String email, String negocio, LocalDate pagoAte) {
    }

    @Transactional(readOnly = true)
    public Perfil perfil(Sessao sessao) {
        AppUser usuario = usuarios.findById(sessao.userId()).orElseThrow();
        Workspace workspace = workspaces.findById(sessao.workspaceId()).orElseThrow();
        return new Perfil(usuario.nome(), usuario.email(), workspace.nome(), workspace.pagoAte());
    }

    private String abrirSessao(AppUser usuario) {
        String token = Tokens.gerar();
        sessoes.save(UserSession.criar(usuario.id(), Tokens.hash(token), DIAS_DE_SESSAO));
        return token;
    }

    private UUID workspaceDe(UUID userId) {
        return membros.workspaceDoUsuario(userId).orElseThrow(ErroDeNegocio::naoAutenticado);
    }
}
