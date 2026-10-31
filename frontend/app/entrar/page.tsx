"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { Botao } from "@/components/Botao";
import { Campo } from "@/components/Campo";

export default function Entrar() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);

    const r = await fetch("/api/sessao", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, senha }),
    });

    if (r.ok) {
      router.replace("/");
      router.refresh();
      return;
    }

    const corpo = await r.json().catch(() => null);
    setErro(corpo?.detail ?? "Não consegui entrar. Tente de novo.");
    setEnviando(false);
  }

  return (
    <main
      style={{
        maxWidth: 420,
        margin: "0 auto",
        padding: "var(--space-12) var(--space-4)",
        display: "grid",
        gap: "var(--space-8)",
      }}
    >
      <div>
        <h1
          style={{
            fontSize: "var(--text-title-size)",
            fontWeight: 500,
            letterSpacing: "-0.02em",
            margin: 0,
          }}
        >
          Entrar
        </h1>
        <p style={{ color: "var(--ink-muted)", marginTop: "var(--space-2)" }}>
          Quanto entrou, quanto saiu, quanto sobrou.
        </p>
      </div>

      <form onSubmit={enviar} style={{ display: "grid", gap: "var(--space-5)" }}>
        <Campo
          rotulo="E-mail"
          type="email"
          inputMode="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <Campo
          rotulo="Senha"
          type="password"
          autoComplete="current-password"
          required
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          erro={erro}
        />
        <Botao type="submit" disabled={enviando}>
          {enviando ? "Entrando…" : "Entrar"}
        </Botao>
      </form>

      <p style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)", margin: 0 }}>
        Ainda não tem conta?{" "}
        <Link href="/cadastro" style={{ color: "var(--ink)", textDecoration: "underline" }}>
          Criar agora
        </Link>
      </p>
    </main>
  );
}
