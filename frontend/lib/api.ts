/**
 * Toda chamada do navegador passa por /api/b/..., que e o proxy do Next. O
 * token de sessao vive num cookie httpOnly e nunca chega ao JavaScript — um XSS
 * nesta pagina nao consegue roubar a sessao de ninguem.
 */

export class ErroDaApi extends Error {
  constructor(
    readonly status: number,
    mensagem: string,
    readonly codigo?: string,
  ) {
    super(mensagem);
  }
}

export async function api<T>(rota: string, opcoes: RequestInit = {}): Promise<T> {
  const r = await fetch(`/api/b/${rota}`, {
    ...opcoes,
    headers: { "Content-Type": "application/json", ...opcoes.headers },
  });

  const texto = await r.text();
  const corpo = texto ? JSON.parse(texto) : null;

  if (!r.ok) {
    throw new ErroDaApi(
      r.status,
      corpo?.detail ?? "Alguma coisa deu errado. Tente de novo.",
      corpo?.codigo,
    );
  }
  return corpo as T;
}
