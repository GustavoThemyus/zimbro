package br.com.zimbro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Spring Boot 4 traz o Jackson 3: o pacote e tools.jackson, nao com.fasterxml.
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * O teste mais importante do projeto.
 *
 * Um cliente ver o extrato de outro acaba com um produto financeiro vendido de
 * boca em boca numa cidade onde todo mundo se conhece. Dois workspaces povoados,
 * e a afirmacao de que nenhum endpoint devolve — nem aceita — linha do vizinho.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IsolamentoEntreWorkspacesTest {

    @Autowired
    WebApplicationContext contexto;

    @Autowired
    ObjectMapper json;

    MockMvc mvc;

    String tokenA;
    String tokenB;
    String contaA;
    String contaB;
    String lancamentoB;

    @BeforeEach
    void povoar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        tokenA = cadastrar("ana-" + System.nanoTime() + "@teste.br", "Barbearia da Ana");
        tokenB = cadastrar("bruno-" + System.nanoTime() + "@teste.br", "Doces do Bruno");

        contaA = primeiraConta(tokenA);
        contaB = primeiraConta(tokenB);

        lancamentoB = lancar(tokenB, contaB, 15_000);
        lancar(tokenA, contaA, 7_700);
    }

    @Test
    @DisplayName("A lista do mês de um workspace não mostra lançamento do outro")
    void listaNaoVazaLancamento() throws Exception {
        JsonNode mesDeA = ler(mvc.perform(get("/api/mes").header("Authorization", "Bearer " + tokenA)));

        assertThat(mesDeA.get("lancamentos")).hasSize(1);
        assertThat(mesDeA.get("lancamentos").get(0).get("valorCents").asLong()).isEqualTo(7_700);
        assertThat(mesDeA.get("resumo").get("entrouCents").asLong()).isEqualTo(7_700);
    }

    @Test
    @DisplayName("A lista de contas de um workspace não mostra conta do outro")
    void listaNaoVazaConta() throws Exception {
        JsonNode contasDeA = ler(mvc.perform(get("/api/contas").header("Authorization", "Bearer " + tokenA)));

        assertThat(contasDeA).hasSize(2);
        for (JsonNode conta : contasDeA) {
            assertThat(conta.get("id").asText()).isNotEqualTo(contaB);
        }
    }

    @Test
    @DisplayName("Lançar usando a conta do vizinho é recusado")
    void naoLancaNaContaDoVizinho() throws Exception {
        mvc.perform(post("/api/lancamentos")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeLancamento(contaB, 1_000)))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @DisplayName("Apagar lançamento do vizinho é recusado")
    void naoApagaDoVizinho() throws Exception {
        mvc.perform(delete("/api/lancamentos/" + lancamentoB)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @DisplayName("Sem token, nenhum endpoint responde")
    void semTokenNaoPassa() throws Exception {
        for (String rota : new String[] { "/api/mes", "/api/contas", "/api/categorias", "/api/auth/eu" }) {
            mvc.perform(get(rota))
                    .andExpect(r -> assertThat(r.getResponse().getStatus())
                            .as(rota)
                            .isEqualTo(401));
        }
    }

    // ------------------------------------------------------------------ apoio

    private String cadastrar(String email, String negocio) throws Exception {
        String corpo = """
                {"email":"%s","senha":"segredo123","nome":"Teste","nomeDoNegocio":"%s","atividade":"SERVICOS"}
                """.formatted(email, negocio);
        return ler(mvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))).get("token").asText();
    }

    private String primeiraConta(String token) throws Exception {
        return ler(mvc.perform(get("/api/contas").header("Authorization", "Bearer " + token)))
                .get(0).get("id").asText();
    }

    private String lancar(String token, String contaId, long centavos) throws Exception {
        return ler(mvc.perform(post("/api/lancamentos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDeLancamento(contaId, centavos)))).get("id").asText();
    }

    private String corpoDeLancamento(String contaId, long centavos) {
        return """
                {"contaId":"%s","quando":"%s","valorCents":%d,"direcao":"INFLOW",
                 "descricao":"Venda","escopo":"BUSINESS"}
                """.formatted(contaId, LocalDate.now(), centavos);
    }

    private JsonNode ler(org.springframework.test.web.servlet.ResultActions acoes) throws Exception {
        MvcResult resultado = acoes.andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }
}
