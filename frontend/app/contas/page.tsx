"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { Botao } from "@/components/Botao";
import { Campo } from "@/components/Campo";
import { api } from "@/lib/api";
import type { Conta } from "@/lib/tipos";

const TIPOS = [
  { valor: "CHECKING", rotulo: "Conta no banco" },
  { valor: "CASH", rotulo: "Dinheiro na mão" },
  { valor: "WALLET", rotulo: "Carteira digital" },
  { valor: "CREDIT_CARD", rotulo: "Cartão de crédito" },
  { valor: "SAVINGS", rotulo: "Poupança" },
];

const POSSES = [
  { valor: "MIXED", rotulo: "As duas coisas" },
  { valor: "BUSINESS", rotulo: "Só do negócio" },
  { valor: "PERSONAL", rotulo: "Só pessoal" },
];

export default function Contas() {
  const [contas, setContas] = useState<Conta[]>([]);
  const [criando, setCriando] = useState(false);
  const [nova, setNova] = useState({ nome: "", tipo: "CHECKING", posse: "MIXED" });
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(() => {
    api<Conta[]>("contas").then(setContas).catch((e: Error) => setErro(e.message));
  }, []);

  useEffect(carregar, [carregar]);

  async function criar(e: React.FormEvent) {
    e.preventDefault();
    try {
      await api("contas", {
        method: "POST",
        body: JSON.stringify({ ...nova, saldoInicialCents: 0 }),
      });
      setNova({ nome: "", tipo: "CHECKING", posse: "MIXED" });
      setCriando(false);
      carregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  async function arquivar(id: string) {
    await api(`contas/${id}`, { method: "DELETE" });
    carregar();
  }

  return (
    <main
      style={{
        maxWidth: 480,
        margin: "0 auto",
        padding: "var(--space-5) var(--space-4) var(--space-12)",
        display: "grid",
        gap: "var(--space-6)",
      }}
    >
      <header style={{ display: "flex", alignItems: "center", gap: "var(--space-3)" }}>
        <Link
          href="/"
          style={{
            color: "var(--ink-muted)",
            textDecoration: "none",
            fontSize: "var(--text-body-sm-size)",
            minHeight: "var(--touch-min)",
            display: "flex",
            alignItems: "center",
          }}
        >
          ← Voltar
        </Link>
        <h1
          style={{
            fontSize: "var(--text-title-sm-size)",
            fontWeight: 500,
            letterSpacing: "-0.01em",
            margin: 0,
          }}
        >
          Contas
        </h1>
      </header>

      <p style={{ color: "var(--ink-muted)", margin: 0, fontSize: "var(--text-body-sm-size)" }}>
        Dinheiro na mão é uma conta como qualquer outra. Se você usa a mesma conta
        pra tudo, deixe em “as duas coisas” — você separa lançamento por lançamento.
      </p>

      <section style={{ display: "grid" }}>
        {contas.map((c) => (
          <div
            key={c.id}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "var(--space-4)",
              minHeight: "var(--row-min)",
              borderBottom: "1px solid var(--border-hairline)",
            }}
          >
            <span>
              <span
                style={{
                  display: "block",
                  fontSize: "var(--text-body-strong-size)",
                  fontWeight: 500,
                }}
              >
                {c.nome}
              </span>
              <span
                style={{
                  display: "block",
                  fontSize: "var(--text-body-sm-size)",
                  color: "var(--ink-muted)",
                  marginTop: 2,
                }}
              >
                {TIPOS.find((t) => t.valor === c.tipo)?.rotulo ?? c.tipo} ·{" "}
                {POSSES.find((p) => p.valor === c.posse)?.rotulo}
              </span>
            </span>
            <button
              type="button"
              onClick={() => arquivar(c.id)}
              style={{
                minHeight: "var(--touch-min)",
                padding: "0 var(--space-3)",
                background: "none",
                border: "none",
                color: "var(--ink-muted)",
                fontFamily: "inherit",
                fontSize: "var(--text-body-sm-size)",
                cursor: "pointer",
              }}
            >
              Arquivar
            </button>
          </div>
        ))}
      </section>

      {erro && <p style={{ color: "var(--danger)", margin: 0 }}>{erro}</p>}

      {criando ? (
        <form onSubmit={criar} style={{ display: "grid", gap: "var(--space-4)" }}>
          <Campo
            rotulo="Nome da conta"
            required
            value={nova.nome}
            onChange={(e) => setNova({ ...nova, nome: e.target.value })}
          />
          <Seletor
            rotulo="Tipo"
            opcoes={TIPOS}
            valor={nova.tipo}
            aoMudar={(v) => setNova({ ...nova, tipo: v })}
          />
          <Seletor
            rotulo="É do negócio ou seu?"
            opcoes={POSSES}
            valor={nova.posse}
            aoMudar={(v) => setNova({ ...nova, posse: v })}
          />
          <Botao type="submit">Criar conta</Botao>
          <Botao variante="fantasma" tamanho="md" type="button" onClick={() => setCriando(false)}>
            Cancelar
          </Botao>
        </form>
      ) : (
        <Botao variante="secundaria" onClick={() => setCriando(true)}>
          Nova conta
        </Botao>
      )}
    </main>
  );
}

function Seletor({
  rotulo,
  opcoes,
  valor,
  aoMudar,
}: {
  rotulo: string;
  opcoes: { valor: string; rotulo: string }[];
  valor: string;
  aoMudar: (v: string) => void;
}) {
  return (
    <div style={{ display: "grid", gap: "var(--space-2)" }}>
      <label style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}>
        {rotulo}
      </label>
      <select
        value={valor}
        onChange={(e) => aoMudar(e.target.value)}
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
        {opcoes.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.rotulo}
          </option>
        ))}
      </select>
    </div>
  );
}
