import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

import { COOKIE, guardarSessao } from "@/lib/sessao";

const BASE = process.env.BACKEND_URL ?? "http://localhost:8080";

/** Entrar. */
export async function POST(req: NextRequest) {
  const r = await fetch(`${BASE}/api/auth/entrar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await req.text(),
    cache: "no-store",
  });

  const corpo = await r.json();
  if (!r.ok) {
    return NextResponse.json(corpo, { status: r.status });
  }

  const resposta = NextResponse.json({ nome: corpo.nome, negocio: corpo.negocio });
  guardarSessao(req, resposta, corpo.token);
  return resposta;
}

/** Sair. */
export async function DELETE() {
  const token = (await cookies()).get(COOKIE)?.value;
  if (token) {
    await fetch(`${BASE}/api/auth/sair`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => undefined);
  }

  const resposta = NextResponse.json({ ok: true });
  resposta.cookies.delete(COOKIE);
  return resposta;
}
