package br.com.financeiro.reporting.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.financeiro.platform.tenant.Sessao;
import br.com.financeiro.platform.web.ErroDeNegocio;
import br.com.financeiro.reporting.infra.MesRepository;

@RestController
@RequestMapping("/api/mes")
class MesController {

    private final MesRepository mes;

    MesController(MesRepository mes) {
        this.mes = mes;
    }

    record MesResposta(int ano, int mes, MesRepository.Resumo resumo, List<MesRepository.Linha> lancamentos) {
    }

    @GetMapping
    MesResposta ver(Sessao sessao,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String escopo) {

        YearMonth alvo = (ano == null || mes == null)
                ? YearMonth.now()
                : YearMonth.of(ano, mes);

        if (escopo != null && !List.of("BUSINESS", "PERSONAL").contains(escopo)) {
            throw ErroDeNegocio.invalido("escopo-invalido", "Filtro de escopo desconhecido.");
        }

        LocalDate inicio = alvo.atDay(1);
        LocalDate fim = alvo.atEndOfMonth();

        return new MesResposta(alvo.getYear(), alvo.getMonthValue(),
                this.mes.resumo(sessao.workspaceId(), inicio, fim),
                this.mes.lancamentos(sessao.workspaceId(), inicio, fim, escopo));
    }
}
