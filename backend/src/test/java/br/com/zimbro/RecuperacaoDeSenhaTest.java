package br.com.zimbro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.zimbro.notifications.api.Avisos;

/**
 * O caminho inteiro de quem esqueceu a senha: pedir, receber o link, entrar com
 * a senha nova.
 *
 * Existe porque hoje este e o unico jeito de recuperar uma conta — nao ha tela
 * de admin, nao ha suporte por telefone. Se ele quebrar, o cliente fica
 * trancado fora do proprio dinheiro e descobre isso no pior momento possivel.
 * O EnvioDeEmailTest cobre o adaptador (qual modo, que mensagem sai); este
 * cobre o caso de uso, que e coisa diferente e e a que o cliente sente.
 *
 * O e-mail nao e enviado de verdade: um Avisos de mentira, @Primary, intercepta
 * a chamada e guarda o link. E dele que o teste tira o token — exatamente como
 * o usuario tiraria do e-mail, sem ler o banco por baixo. Se o link sair
 * quebrado, o teste quebra junto, que e o que se quer.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RecuperacaoDeSenhaTest {

    private static final String SENHA_ANTIGA = "senha-antiga-1";
    private static final String SENHA_NOVA = "senha-nova-9876";

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    CaixaDeEntrada caixa;

    MockMvc mvc;
    String email;

    @BeforeEach
    void cadastrarAlguem() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        caixa.limpar();

        email = "esquecida-" + System.nanoTime() + "@teste.br";
        mvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","senha":"%s","nome":"Teste",
                         "nomeDoNegocio":"Barbearia","atividade":"SERVICOS"}
                        """.formatted(email, SENHA_ANTIGA)))
                .andExpect(status -> assertThat(status.getResponse().getStatus()).isEqualTo(200));
    }

    @Test
    @DisplayName("Pedir, trocar e entrar com a senha nova")
    void caminhoCompleto() throws Exception {
        pedirRecuperacao(email);

        assertThat(caixa.cartas).as("ninguem recebeu link nenhum").hasSize(1);
        assertThat(caixa.cartas.get(0).para()).isEqualTo(email);

        trocarSenha(tokenDoLink(), SENHA_NOVA)
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isEqualTo(200));

        assertThat(entrar(email, SENHA_NOVA).getResponse().getStatus())
                .as("a senha nova tinha que entrar")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Depois da troca, a senha antiga não entra mais")
    void senhaAntigaMorre() throws Exception {
        pedirRecuperacao(email);
        trocarSenha(tokenDoLink(), SENHA_NOVA);

        assertThat(entrar(email, SENHA_ANTIGA).getResponse().getStatus())
                .as("senha trocada que continua valendo nao e troca, e adicao")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("O link vale uma vez só")
    void linkEhDeUsoUnico() throws Exception {
        pedirRecuperacao(email);
        String token = tokenDoLink();

        trocarSenha(token, SENHA_NOVA);

        // Link de recuperacao fica em caixa de e-mail, que vaza mais facil que
        // senha. Reusavel, ele e uma chave permanente da conta.
        assertThat(trocarSenha(token, "outra-senha-5555").andReturn().getResponse().getStatus())
                .isEqualTo(422);

        assertThat(entrar(email, "outra-senha-5555").getResponse().getStatus())
                .as("a segunda troca nao podia ter pegado")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Trocar a senha derruba a sessão que estava aberta")
    void sessaoAbertaCai() throws Exception {
        String tokenDeSessao = entrarComToken(email, SENHA_ANTIGA);

        assertThat(perfil(tokenDeSessao).getResponse().getStatus())
                .as("a sessao tinha que estar valendo antes da troca")
                .isEqualTo(200);

        pedirRecuperacao(email);
        trocarSenha(tokenDoLink(), SENHA_NOVA);

        // Quem troca a senha costuma estar trocando justamente porque desconfia
        // que alguem entrou. Deixar a sessao do invasor de pe anula o gesto.
        assertThat(perfil(tokenDeSessao).getResponse().getStatus())
                .as("a sessao antiga continuou de pe depois da troca de senha")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Senha curta é recusada e o link continua valendo")
    void senhaCurtaNaoQueimaOLink() throws Exception {
        pedirRecuperacao(email);
        String token = tokenDoLink();

        assertThat(trocarSenha(token, "123").andReturn().getResponse().getStatus()).isEqualTo(422);

        // Se a validacao de tamanho consumisse o token, um erro de digitacao
        // trancaria o usuario fora e o obrigaria a pedir outro link. A ordem das
        // checagens no servico e o que garante isto; o teste e que a mantem.
        trocarSenha(token, SENHA_NOVA);
        assertThat(entrar(email, SENHA_NOVA).getResponse().getStatus())
                .as("o link tinha que continuar valendo depois da senha curta")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("E-mail que não existe responde igual e não manda nada")
    void emailDesconhecidoNaoVazaNada() throws Exception {
        pedirRecuperacao("ninguem-" + System.nanoTime() + "@teste.br")
                .andExpect(r -> assertThat(r.getResponse().getStatus())
                        .as("resposta diferente aqui transforma a tela num teste de "
                                + "'essa pessoa e cliente do Zimbro?'")
                        .isEqualTo(200));

        assertThat(caixa.cartas).isEmpty();
    }

    // --- ferramentas ---

    private ResultActions pedirRecuperacao(String paraQuem) throws Exception {
        return mvc.perform(post("/api/auth/recuperar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(paraQuem)));
    }

    private ResultActions trocarSenha(String token, String senha) throws Exception {
        return mvc.perform(post("/api/auth/nova-senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"senha\":\"%s\"}".formatted(token, senha)));
    }

    private org.springframework.test.web.servlet.MvcResult entrar(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/entrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andReturn();
    }

    private String entrarComToken(String email, String senha) throws Exception {
        String corpo = entrar(email, senha).getResponse().getContentAsString();
        int inicio = corpo.indexOf("\"token\":\"") + 9;
        return corpo.substring(inicio, corpo.indexOf('"', inicio));
    }

    private org.springframework.test.web.servlet.MvcResult perfil(String tokenDeSessao) throws Exception {
        return mvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + tokenDeSessao)).andReturn();
    }

    /**
     * Tira o token do link do e-mail, como o usuario faria ao clicar. De
     * proposito nao le o password_reset_token no banco: um link mal montado
     * (endereco errado, parametro com outro nome) tem que reprovar aqui.
     */
    private String tokenDoLink() {
        String link = caixa.cartas.get(caixa.cartas.size() - 1).link();
        assertThat(link).contains("/nova-senha?token=");
        return link.substring(link.indexOf("token=") + 6);
    }

    /** Guarda o que teria sido enviado, no lugar do adaptador de verdade. */
    record Carta(String para, String link) {
    }

    static class CaixaDeEntrada implements Avisos {

        final List<Carta> cartas = new ArrayList<>();

        @Override
        public void recuperacaoDeSenha(String email, String link) {
            cartas.add(new Carta(email, link));
        }

        void limpar() {
            cartas.clear();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SemEnviarEmail {

        @Bean
        @Primary
        CaixaDeEntrada caixaDeEntrada() {
            return new CaixaDeEntrada();
        }
    }
}
