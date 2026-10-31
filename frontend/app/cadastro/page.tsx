"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { Botao } from "@/components/Botao";
import { Campo } from "@/components/Campo";

const ATIVIDADES = [
  { valor: "SERVICOS", rotulo: "Presto serviço" },
  { valor: "COMERCIO", rotulo: "Vendo produto" },
  { valor: "COMERCIO_SERVICOS", rotulo: "Os dois" },
  { valor: "TRANSPORTE", rotulo: "Transporte" },
];

export default function Cadastro() {
  const router = useRouter();
  const [form, setForm] = useState({
    nome: "",
    nomeDoNegocio: "",
    email: "",
    senha: "",
    atividade: "SERVICOS",
  });
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const mudar = (campo: keyof typeof form) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [campo]: e.target.value }));

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);

    const r = await fetch("/api/cadastro", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form),
    });

    if (r.ok) {
      // Vai direto para o lançamento: a primeira coisa que a pessoa faz é
      // registrar algo, não configurar conta.
      router.replace("/lancar");
      router.refresh();
      return;
    }

    const corpo = await r.json().catch(() => null);
    setErro(corpo?.detail ?? "Não consegui criar a conta.");
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
          Criar conta
        </h1>
        <p style={{ color: "var(--ink-muted)", marginTop: "var(--space-2)" }}>
          Leva menos de um minuto. Depois é só lançar.
        </p>
      </div>

      <form onSubmit={enviar} style={{ display: "grid", gap: "var(--space-5)" }}>
        <Campo rotulo="Seu nome" required value={form.nome} onChange={mudar("nome")} />
        <Campo
          rotulo="Nome do seu negócio"
          required
          value={form.nomeDoNegocio}
          onChange={mudar("nomeDoNegocio")}
          ajuda="Pode ser o nome que os clientes usam."
        />

        <div style={{ display: "grid", gap: "var(--space-2)" }}>
          <label
            htmlFor="atividade"
            style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}
          >
            O que você faz
          </label>
          <select
            id="atividade"
            value={form.atividade}
            onChange={mudar("atividade")}
            style={{
              height: "var(--control-md)",
              padding: "0 var(--space-4)",
              borderRadius: "var(--radius-md)",
              border: "1px solid var(--border-strong)",
              background: "var(--surface-sunken)",
              color: "var(--ink)",
              fontFamily: "inherit",
              fontSize: "16px",
            }}
          >
            {ATIVIDADES.map((a) => (
              <option key={a.valor} value={a.valor}>
                {a.rotulo}
              </option>
            ))}
          </select>
        </div>

        <Campo
          rotulo="E-mail"
          type="email"
          inputMode="email"
          autoComplete="email"
          required
          value={form.email}
          onChange={mudar("email")}
        />
        <Campo
          rotulo="Senha"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          value={form.senha}
          onChange={mudar("senha")}
          ajuda="Pelo menos 8 letras ou números."
          erro={erro}
        />

        <Botao type="submit" disabled={enviando}>
          {enviando ? "Criando…" : "Criar conta"}
        </Botao>
      </form>

      <p style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)", margin: 0 }}>
        Já tem conta?{" "}
        <Link href="/entrar" style={{ color: "var(--ink)", textDecoration: "underline" }}>
          Entrar
        </Link>
      </p>
    </main>
  );
}
