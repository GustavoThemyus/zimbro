package br.com.zimbro.platform.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Um unico lugar traduz excecao em resposta. Formato problem+json (RFC 7807):
 * mensagem em portugues para a pessoa, e um codigo estavel para o frontend
 * decidir o que fazer sem depender do texto.
 */
@RestControllerAdvice
class TratadorDeErros {

    @ExceptionHandler(ErroDeNegocio.class)
    ProblemDetail negocio(ErroDeNegocio e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
        p.setType(URI.create("urn:zimbro:" + e.codigo()));
        p.setProperty("codigo", e.codigo());
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Confira os campos.");
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, detalhe);
        p.setProperty("codigo", "campo-invalido");
        return p;
    }
}
