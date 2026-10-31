package br.com.financeiro.platform.web;

import org.springframework.http.HttpStatus;

/**
 * Erro previsto, com mensagem que o usuario pode ler. Tudo que nao for isto vira
 * 500 e vai para o Sentry — e sinal de bug nosso, nao de uso errado.
 */
public class ErroDeNegocio extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ErroDeNegocio(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public static ErroDeNegocio naoEncontrado(String oQue) {
        return new ErroDeNegocio(HttpStatus.NOT_FOUND, "nao-encontrado", oQue + " não existe.");
    }

    public static ErroDeNegocio invalido(String codigo, String mensagem) {
        return new ErroDeNegocio(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensagem);
    }

    public static ErroDeNegocio naoAutenticado() {
        return new ErroDeNegocio(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Entre na sua conta.");
    }

    public HttpStatus status() {
        return status;
    }

    public String codigo() {
        return codigo;
    }
}
