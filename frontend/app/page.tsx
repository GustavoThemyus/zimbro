"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import { EditarLancamento } from "@/components/EditarLancamento";
import { LinhaLancamento } from "@/components/LinhaLancamento";
import { Valor } from "@/components/Valor";
import { api } from "@/lib/api";
import type { Categoria, Lancamento, Mes } from "@/lib/tipos";

const NOMES_DE_MES = [
  "janeiro", "fevereiro", "março", "abril", "maio", "junho",
  "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
];

type Periodo = { ano: number; mes: number };

export default function Inicio() {
  const router = useRouter();
  // O mês corrente só é lido depois que a página monta. Ler o relógio durante a
  // renderização congelaria "hoje" no momento do build — a página seria gerada
  // uma vez e mostraria para sempre o mês em que foi publicada.
  const [periodo, setPeriodo] = useState<Periodo | null>(null);
  const [filtro, setFiltro] = useState<"" | "BUSINESS" | "PERSONAL">("");
  const [mes, setMes] = useState<Mes | null>(null);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [editando, setEditando] = useState<Lancamento | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const d = new Date();
    setPeriodo({ ano: d.getFullYear(), mes: d.getMonth() + 1 });
  }, []);

  const carregar = useCallback(() => {
    if (!periodo) return;

    const busca = new URLSearchParams({ ano: String(periodo.ano), mes: String(periodo.mes) });
    if (filtro) busca.set("escopo", filtro);

    api<Mes>(`mes?${busca}`)
      .then(setMes)
      .catch((e: Error) => setErro(e.message));
  }, [periodo, filtro]);

  useEffect(carregar, [carregar]);

  useEffect(() => {
    api<Categoria[]>("categorias").then(setCategorias).catch(() => undefined);
  }, []);

  function andarMes(passo: number) {
    setPeriodo((p) => {
      if (!p) return p;
      const d = new Date(p.ano, p.mes - 1 + passo, 1);
      return { ano: d.getFullYear(), mes: d.getMonth() + 1 };
    });
  }

  async function sair() {
    await fetch("/api/sessao", { method: "DELETE" });
    router.replace("/entrar");
    router.refresh();
  }

  const resumo = mes?.resumo;
  const nomeDoMes = periodo ? NOMES_DE_MES[periodo.mes - 1] : "";

  return (
    <main
      style={{
        maxWidth: 480,
        margin: "0 auto",
        padding: "var(--space-5) var(--space-4) 120px",
        display: "grid",
        gap: "var(--space-6)",
      }}
    >
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "var(--space-2)" }}>
          <BotaoDeMes rotulo="‹" aoTocar={() => andarMes(-1)} />
          <span style={{ fontSize: "var(--text-body-strong-size)", fontWeight: 500 }}>
            {nomeDoMes}
          </span>
          <BotaoDeMes rotulo="›" aoTocar={() => andarMes(1)} />
        </div>
        <div style={{ display: "flex", gap: "var(--space-2)" }}>
          <Link
            href="/contas"
            style={{
              color: "var(--ink-muted)",
              textDecoration: "none",
              fontSize: "var(--text-body-sm-size)",
              minHeight: "var(--touch-min)",
              display: "flex",
              alignItems: "center",
            }}
          >
            Contas
          </Link>
          <button
            type="button"
            onClick={sair}
            style={{
              background: "none",
              border: "none",
              color: "var(--ink-muted)",
              fontFamily: "inherit",
              fontSize: "var(--text-body-sm-size)",
              cursor: "pointer",
            }}
          >
            Sair
          </button>
        </div>
      </header>

      <section
        style={{
          background: "var(--surface-card)",
          border: "1px solid var(--border-hairline)",
          borderRadius: "var(--radius-lg)",
          padding: "var(--space-5)",
        }}
      >
        <p
          style={{
            fontFamily: "var(--font-mono)",
            fontSize: "var(--text-label-micro-size)",
            fontWeight: 500,
            letterSpacing: "0.06em",
            textTransform: "uppercase",
            color: "var(--ink-muted)",
            margin: 0,
          }}
        >
          Sobrou em {nomeDoMes}
        </p>
        <p style={{ margin: "var(--space-2) 0 var(--space-5)" }}>
          <Valor
            centavos={resumo?.sobrouCents ?? 0}
            tamanho="heroi"
            cor={(resumo?.sobrouCents ?? 0) < 0 ? "problema" : "neutro"}
          />
        </p>

        <dl style={{ display: "grid", gap: "var(--space-3)", margin: 0 }}>
          <LinhaResumo rotulo="Entrou" centavos={resumo?.entrouCents ?? 0} cor="entrada" />
          <LinhaResumo rotulo="Saiu" centavos={resumo?.saiuCents ?? 0} />
          <LinhaResumo rotulo="Você tirou pra você" centavos={resumo?.retiradaCents ?? 0} />
        </dl>
      </section>

      <div style={{ display: "flex", gap: "var(--space-2)" }}>
        {([
          { v: "", r: "Tudo" },
          { v: "BUSINESS", r: "Negócio" },
          { v: "PERSONAL", r: "Pessoal" },
        ] as const).map((o) => (
          <button
            key={o.v}
            type="button"
            aria-pressed={filtro === o.v}
            onClick={() => setFiltro(o.v)}
            style={{
              height: "var(--control-sm)",
              padding: "0 var(--space-4)",
              borderRadius: "var(--radius-pill)",
              border: `1px solid ${filtro === o.v ? "var(--ink)" : "var(--border-hairline)"}`,
              background: filtro === o.v ? "var(--action-quiet)" : "var(--surface-card)",
              color: "var(--ink)",
              fontFamily: "inherit",
              fontSize: "var(--text-body-sm-size)",
              fontWeight: 500,
              cursor: "pointer",
            }}
          >
            {o.r}
          </button>
        ))}
      </div>

      <section>
        {erro && <p style={{ color: "var(--danger)" }}>{erro}</p>}

        {mes && mes.lancamentos.length === 0 && (
          <p
            style={{
              color: "var(--ink-muted)",
              fontSize: "var(--text-body-lg-size)",
              lineHeight: 1.5,
              margin: 0,
            }}
          >
            Nada lançado em {nomeDoMes} ainda. Toque no botão de baixo e
            registre a primeira venda do dia.
          </p>
        )}

        {mes?.lancamentos.map((l) => (
          <LinhaLancamento key={l.id} lancamento={l} aoTocar={() => setEditando(l)} />
        ))}
      </section>

      <Link
        href="/lancar"
        aria-label="Novo lançamento"
        style={{
          position: "fixed",
          right: "var(--space-5)",
          bottom: "var(--space-6)",
          width: "var(--control-xl)",
          height: "var(--control-xl)",
          borderRadius: "var(--radius-pill)",
          background: "var(--action-solid)",
          color: "var(--ink-inverse)",
          boxShadow: "var(--shadow-lift)",
          display: "grid",
          placeItems: "center",
          fontSize: 30,
          textDecoration: "none",
        }}
      >
        +
      </Link>

      {editando && (
        <EditarLancamento
          lancamento={editando}
          categorias={categorias}
          aoFechar={() => setEditando(null)}
          aoMudar={carregar}
        />
      )}
    </main>
  );
}

function LinhaResumo({
  rotulo,
  centavos,
  cor = "neutro",
}: {
  rotulo: string;
  centavos: number;
  cor?: "neutro" | "entrada";
}) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
      <dt style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}>{rotulo}</dt>
      <dd style={{ margin: 0 }}>
        <Valor centavos={centavos} cor={cor} />
      </dd>
    </div>
  );
}

function BotaoDeMes({ rotulo, aoTocar }: { rotulo: string; aoTocar: () => void }) {
  return (
    <button
      type="button"
      onClick={aoTocar}
      aria-label={rotulo === "‹" ? "Mês anterior" : "Próximo mês"}
      style={{
        width: "var(--touch-min)",
        height: "var(--touch-min)",
        borderRadius: "var(--radius-pill)",
        border: "none",
        background: "transparent",
        color: "var(--ink-muted)",
        fontSize: 22,
        cursor: "pointer",
      }}
    >
      {rotulo}
    </button>
  );
}
