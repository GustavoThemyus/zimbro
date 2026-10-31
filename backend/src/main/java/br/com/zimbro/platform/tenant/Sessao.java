package br.com.zimbro.platform.tenant;

import java.util.UUID;

/**
 * Quem esta falando com a API e de quem sao os dados que ele pode ver.
 *
 * O workspaceId e passado explicitamente para TODO metodo de repositorio. Filtro
 * automatico do Hibernate nunca esquece, mas quando falha o iniciante nao tem
 * onde olhar; parametro explicito pode ser esquecido, e por isso existe um teste
 * que povoa dois workspaces e exige que nenhum endpoint devolva linha do vizinho.
 */
public record Sessao(UUID userId, UUID workspaceId) {

    public static final String ATRIBUTO = "zimbro.sessao";
}
