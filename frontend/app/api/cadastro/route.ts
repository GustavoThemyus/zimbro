import { NextRequest, NextResponse } from "next/server";

import { guardarSessao } from "@/lib/sessao";

const BASE = process.env.BACKEND_URL ?? "http://localhost:8080";

export async function POST(req: NextRequest) {
  const r = await fetch(`${BASE}/api/auth/cadastro`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await req.text(),
    cache: "no-store",
  });

  const corpo = await r.json();
  if (!r.ok) {
    return NextResponse.json(corpo, { status: r.status });
  }

  // Cadastrou, ja entrou. Pedir para a pessoa fazer login logo depois de criar
  // a conta e uma porta a mais para ela desistir.
  const resposta = NextResponse.json({ nome: corpo.nome, negocio: corpo.negocio });
  guardarSessao(req, resposta, corpo.token);
  return resposta;
}
