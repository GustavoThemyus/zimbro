package br.com.financeiro.platform.tenant;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import br.com.financeiro.identity.app.IdentityService;
import br.com.financeiro.platform.web.ErroDeNegocio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolve o token do cabecalho Authorization numa Sessao e guarda na requisicao.
 * O token vem do cookie httpOnly que o Next escreve — o navegador nunca o ve em
 * JavaScript, entao um XSS nao o rouba.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    private final IdentityService identidade;

    AutenticacaoInterceptor(IdentityService identidade) {
        this.identidade = identidade;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith("Bearer ")) {
            throw ErroDeNegocio.naoAutenticado();
        }
        request.setAttribute(Sessao.ATRIBUTO, identidade.resolver(cabecalho.substring(7)));
        return true;
    }
}
