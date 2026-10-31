import { NextResponse } from "next/server";

/**
 * O navegador nunca fala com o Spring direto. Todo acesso passa por um route
 * handler como este, que no futuro carrega o cookie httpOnly de sessao — assim
 * o token nunca mora em localStorage, onde qualquer XSS o pega.
 */
export async function GET() {
  const base = process.env.BACKEND_URL ?? "http://localhost:8080";

  try {
    const r = await fetch(`${base}/api/ping`, { cache: "no-store" });
    if (!r.ok) {
      return NextResponse.json(
        { erro: `O backend respondeu ${r.status}.` },
        { status: 502 },
      );
    }
    return NextResponse.json(await r.json());
  } catch {
    return NextResponse.json(
      { erro: `Nao consegui falar com o backend em ${base}.` },
      { status: 502 },
    );
  }
}
