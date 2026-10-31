package br.com.zimbro.platform.tenant;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Faz `Sessao sessao` funcionar como parametro de metodo de controller. */
class SessaoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return Sessao.class.equals(parametro.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer mav,
            NativeWebRequest request, WebDataBinderFactory binder) {
        return request.getAttribute(Sessao.ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
    }
}
