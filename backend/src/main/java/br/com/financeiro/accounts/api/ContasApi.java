package br.com.financeiro.accounts.api;

import java.util.UUID;

/**
 * A unica porta de entrada do modulo `accounts` para outros modulos. Ninguem de
 * fora importa a entidade Account nem o repositorio: referencia cruzada e por id,
 * e conversa entre modulos passa por um pacote `api` como este.
 */
public interface ContasApi {

    /** Chamado pelo cadastro: o cliente precisa poder lancar antes de configurar nada. */
    void criarContasIniciais(UUID workspaceId);
}
