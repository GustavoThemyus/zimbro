package br.com.zimbro.platform.tenant;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacao;

    WebConfig(AutenticacaoInterceptor autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        // Tudo sob /api exige sessao, menos as quatro portas de entrada. A lista
        // e curta e explicita de proposito: esquecer de proteger uma rota nova e
        // mais dificil quando a excecao precisa ser escrita a mao aqui.
        registro.addInterceptor(autenticacao)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/cadastro",
                        "/api/auth/entrar",
                        "/api/auth/recuperar",
                        "/api/auth/nova-senha");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SessaoArgumentResolver());
    }
}
