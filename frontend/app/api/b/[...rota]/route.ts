import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const BASE = process.env.BACKEND_URL ?? "http://localhost:8080";

type Contexto = { params: Promise<{ rota: string[] }> };

/**
 * Proxy fino para o Spring. Ele existe por um motivo de seguranca: o cookie de
 * sessao e httpOnly, entao so o servidor consegue le-lo e virar o cabecalho
 * Authorization. O backend nunca e exposto direto ao navegador — o Caddy manda
 * tudo para ca.
 */
async function encaminhar(req: NextRequest, ctx: Contexto) {
  const token = (await cookies()).get("sessao")?.value;
  if (!token) {
    return NextResponse.json({ detail: "Entre na sua conta." }, { status: 401 });
  }

  const { rota } = await ctx.params;
  const corpo =
    req.method === "GET" || req.method === "DELETE" ? undefined : await req.text();

  const r = await fetch(`${BASE}/api/${rota.join("/")}${req.nextUrl.search}`, {
    method: req.method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: corpo,
    cache: "no-store",
  });

  const texto = await r.text();
  return new NextResponse(texto || null, {
    status: r.status,
    headers: { "Content-Type": r.headers.get("content-type") ?? "application/json" },
  });
}

export const GET = encaminhar;
export const POST = encaminhar;
export const PUT = encaminhar;
export const PATCH = encaminhar;
export const DELETE = encaminhar;
