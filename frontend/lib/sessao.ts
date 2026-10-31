import type { NextRequest, NextResponse } from "next/server";

export const COOKIE = "sessao";

const SESSENTA_DIAS = 60 * 60 * 24 * 60;

/**
 * `secure` sai do PROTOCOLO da requisição, não de NODE_ENV.
 *
 * A imagem de produção roda com NODE_ENV=production inclusive quando o compose
 * sobe em http://localhost — amarrar `secure` ao ambiente faz o navegador
 * guardar o cookie e nunca mais devolvê-lo, e o app parece deslogar sozinho.
 * Atrás do Caddy com HTTPS, o X-Forwarded-Proto chega como https e o cookie
 * volta a ser `secure`, que é o que importa.
 */
export function guardarSessao(req: NextRequest, resposta: NextResponse, token: string) {
  const porHttps =
    req.headers.get("x-forwarded-proto") === "https" || req.nextUrl.protocol === "https:";

  resposta.cookies.set(COOKIE, token, {
    httpOnly: true,
    sameSite: "lax",
    secure: porHttps,
    path: "/",
    maxAge: SESSENTA_DIAS,
  });
}
