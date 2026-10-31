import { NextRequest, NextResponse } from "next/server";

import { COOKIE } from "@/lib/sessao";

const PUBLICAS = ["/entrar", "/cadastro", "/nova-senha", "/esqueci"];

export function middleware(req: NextRequest) {
  const caminho = req.nextUrl.pathname;
  const temSessao = req.cookies.has(COOKIE);

  if (!temSessao && !PUBLICAS.some((p) => caminho.startsWith(p))) {
    return NextResponse.redirect(new URL("/entrar", req.url));
  }
  if (temSessao && (caminho === "/entrar" || caminho === "/cadastro")) {
    return NextResponse.redirect(new URL("/", req.url));
  }
  return NextResponse.next();
}

export const config = {
  // Fora: as rotas de API (elas se protegem sozinhas), os arquivos do Next,
  // o manifest e os icones — senao o app nao instala sem estar logado.
  matcher: ["/((?!api|_next|icons|manifest.webmanifest|favicon.ico).*)"],
};
