package br.com.financeiro.identity.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.financeiro.identity.app.IdentityService;
import br.com.financeiro.identity.app.IdentityService.Entrada;
import br.com.financeiro.identity.app.IdentityService.Perfil;
import br.com.financeiro.platform.tenant.Sessao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final IdentityService identidade;

    AuthController(IdentityService identidade) {
        this.identidade = identidade;
    }

    record CadastroRequest(
            @Email(message = "Confira o e-mail.") @NotBlank String email,
            @NotBlank(message = "Escolha uma senha.") String senha,
            @NotBlank(message = "Como você se chama?") String nome,
            @NotBlank(message = "Qual o nome do seu negócio?") String nomeDoNegocio,
            @NotBlank String atividade) {
    }

    @PostMapping("/cadastro")
    Entrada cadastrar(@Valid @RequestBody CadastroRequest r) {
        return identidade.cadastrar(new IdentityService.Cadastro(
                r.email(), r.senha(), r.nome(), r.nomeDoNegocio(), r.atividade()));
    }

    record EntrarRequest(@NotBlank String email, @NotBlank String senha) {
    }

    @PostMapping("/entrar")
    Entrada entrar(@Valid @RequestBody EntrarRequest r) {
        return identidade.entrar(r.email(), r.senha());
    }

    @PostMapping("/sair")
    void sair(@RequestHeader("Authorization") String cabecalho) {
        identidade.sair(cabecalho.substring(7));
    }

    @GetMapping("/eu")
    Perfil eu(Sessao sessao) {
        return identidade.perfil(sessao);
    }

    record RecuperarRequest(@Email @NotBlank String email) {
    }

    @PostMapping("/recuperar")
    void recuperar(@Valid @RequestBody RecuperarRequest r) {
        identidade.pedirRecuperacao(r.email());
    }

    record NovaSenhaRequest(@NotBlank String token, @NotBlank String senha) {
    }

    @PostMapping("/nova-senha")
    void novaSenha(@Valid @RequestBody NovaSenhaRequest r) {
        identidade.redefinirSenha(r.token(), r.senha());
    }
}
