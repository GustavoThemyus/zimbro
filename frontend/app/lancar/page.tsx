"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Botao } from "@/components/Botao";
import { api } from "@/lib/api";
import { digitosParaCentavos, formatar, hoje } from "@/lib/dinheiro";
import type { Categoria, Conta } from "@/lib/tipos";

const TECLAS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "apagar", "0", "00"];

/**
 * A tela mais importante do produto. Tres toques: valor, categoria, salvar.
 *
 * Nada de navegacao, nada de rolagem, nada de campo opcional na frente. Quem
 * usa isto esta em pe, com um cliente esperando — se demorar, ele nao lanca, e
 * se ele nao lanca o fechamento do mes nao vale nada.
 */
export default function Lancar() {
  const [digitos, setDigitos] = useState("");
  const [direcao, setDirecao] = useState<"INFLOW" | "OUTFLOW">("INFLOW");
  const [escopo, setEscopo] = useState<"BUSINESS" | "PERSONAL">("BUSINESS");
  const [categoriaId, setCategoriaId] = useState<string | null>(null);
  const [contaId, setContaId] = useState<string>("");
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [contas, setContas] = useState<Conta[]>([]);
  const [salvando, setSalvando] = useState(false);
  const [salvo, setSalvo] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const centavos = digitosParaCentavos(digitos);

  useEffect(() => {
    Promise.all([api<Categoria[]>("categorias"), api<Conta[]>("contas")])
      .then(([cats, cs]) => {
        setCategorias(cats);
        setContas(cs);
        setContaId(cs[0]?.id ?? "");
      })
      .catch((e: Error) => setErro(e.message));
  }, []);

  // Poucas opcoes a vista. Lista comprida faz a pessoa parar para pensar, e
  // parar para pensar e o que mata os tres toques.
  const sugeridas = useMemo(() => {
    const querido = direcao === "INFLOW" ? "INCOME" : "EXPENSE";
    return categorias
      .filter((c) => c.tipo === querido || (direcao === "OUTFLOW" && c.tipo === "OWNER_DRAW"))
      .filter((c) => c.escopoPadrao === escopo || c.escopoPadrao === "MIXED")
      .slice(0, 6);
  }, [categorias, direcao, escopo]);

  function teclar(tecla: string) {
    setSalvo(false);
    if (tecla === "apagar") {
      setDigitos((d) => d.slice(0, -1));
    } else {
      setDigitos((d) => (d === "" && tecla === "00" ? "" : d + tecla));
    }
  }

  async function salvar() {
    if (centavos <= 0 || !contaId) return;
    setSalvando(true);
    setErro(null);

    try {
      await api("lancamentos", {
        method: "POST",
        body: JSON.stringify({
          contaId,
          quando: hoje(),
          valorCents: centavos,
          direcao,
          descricao: categorias.find((c) => c.id === categoriaId)?.nome ?? null,
          categoriaId,
          escopo,
        }),
      });
      // Zera e fica pronto para o proximo: quem lanca uma venda costuma lancar
      // a de seguida no mesmo minuto.
      setDigitos("");
      setCategoriaId(null);
      setSalvo(true);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <main
      style={{
        maxWidth: 420,
        margin: "0 auto",
        minHeight: "100dvh",
        padding: "var(--space-4)",
        display: "grid",
        gridTemplateRows: "auto auto auto 1fr auto",
        gap: "var(--space-4)",
      }}
    >
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
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
        <Segmentado
          opcoes={[
            { valor: "BUSINESS", rotulo: "Negócio" },
            { valor: "PERSONAL", rotulo: "Pessoal" },
          ]}
          valor={escopo}
          aoMudar={(v) => {
            setEscopo(v as "BUSINESS" | "PERSONAL");
            setCategoriaId(null);
          }}
        />
      </header>

      <Segmentado
        opcoes={[
          { valor: "INFLOW", rotulo: "Entrou" },
          { valor: "OUTFLOW", rotulo: "Saiu" },
        ]}
        valor={direcao}
        aoMudar={(v) => {
          setDirecao(v as "INFLOW" | "OUTFLOW");
          setCategoriaId(null);
        }}
        largura="cheia"
      />

      <div style={{ textAlign: "center", padding: "var(--space-4) 0" }}>
        <span
          style={{
            fontFamily: "var(--font-mono)",
            fontVariantNumeric: "tabular-nums",
            fontWeight: 500,
            fontSize: "var(--text-money-hero-size)",
            lineHeight: 1,
            letterSpacing: "-0.03em",
            color: centavos === 0 ? "var(--ink-faint)" : "var(--ink)",
          }}
        >
          <span style={{ fontSize: "0.62em", color: "var(--ink-muted)", marginRight: 4 }}>R$</span>
          {formatar(centavos)}
        </span>
        <p
          style={{
            margin: "var(--space-2) 0 0",
            fontSize: "var(--text-body-sm-size)",
            color: salvo ? "var(--money-in)" : "var(--ink-muted)",
            minHeight: 20,
          }}
        >
          {erro ?? (salvo ? "Lançado. Pode lançar outro." : "Digite o valor")}
        </p>
      </div>

      <div style={{ display: "grid", gap: "var(--space-4)", alignContent: "start" }}>
        <div style={{ display: "flex", gap: "var(--space-2)", flexWrap: "wrap" }}>
          {sugeridas.map((c) => {
            const ativa = c.id === categoriaId;
            return (
              <button
                key={c.id}
                type="button"
                onClick={() => setCategoriaId(ativa ? null : c.id)}
                style={{
                  height: "var(--control-sm)",
                  padding: "0 var(--space-4)",
                  borderRadius: "var(--radius-pill)",
                  // A cor da categoria vive aqui e no poço de ícone da lista —
                  // nunca no preenchimento de um botão de ação.
                  border: `1.5px solid ${ativa ? `var(--${c.cor})` : "var(--border-hairline)"}`,
                  background: ativa ? `var(--${c.cor}-quiet)` : "var(--surface-card)",
                  color: ativa ? `var(--${c.cor})` : "var(--ink)",
                  fontFamily: "inherit",
                  fontSize: "var(--text-body-sm-size)",
                  fontWeight: 500,
                  cursor: "pointer",
                }}
              >
                {c.nome}
              </button>
            );
          })}
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(3, 1fr)",
            gap: "var(--space-3)",
          }}
        >
          {TECLAS.map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => teclar(t)}
              aria-label={t === "apagar" ? "Apagar" : t}
              style={{
                height: "var(--control-xl)",
                borderRadius: "var(--radius-sm)",
                border: "1px solid var(--border-hairline)",
                background: "var(--surface-sunken)",
                color: "var(--ink)",
                fontFamily: t === "apagar" ? "inherit" : "var(--font-mono)",
                fontSize: "var(--text-money-size)",
                fontWeight: 500,
                cursor: "pointer",
              }}
            >
              {t === "apagar" ? "←" : t}
            </button>
          ))}
        </div>
      </div>

      <div style={{ display: "grid", gap: "var(--space-3)" }}>
        {contas.length > 1 && (
          <select
            value={contaId}
            onChange={(e) => setContaId(e.target.value)}
            aria-label="Conta"
            style={{
              height: "var(--control-sm)",
              padding: "0 var(--space-3)",
              borderRadius: "var(--radius-md)",
              border: "1px solid var(--border-hairline)",
              background: "var(--surface-card)",
              color: "var(--ink-muted)",
              fontFamily: "inherit",
              fontSize: "16px",
            }}
          >
            {contas.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </select>
        )}
        <Botao tamanho="xl" onClick={salvar} disabled={salvando || centavos <= 0}>
          {salvando ? "Salvando…" : "Salvar"}
        </Botao>
      </div>
    </main>
  );
}

function Segmentado({
  opcoes,
  valor,
  aoMudar,
  largura = "auto",
}: {
  opcoes: { valor: string; rotulo: string }[];
  valor: string;
  aoMudar: (v: string) => void;
  largura?: "auto" | "cheia";
}) {
  return (
    <div
      role="group"
      style={{
        display: "grid",
        gridTemplateColumns: `repeat(${opcoes.length}, 1fr)`,
        gap: 4,
        padding: 4,
        borderRadius: "var(--radius-pill)",
        background: "var(--surface-sunken)",
        width: largura === "cheia" ? "100%" : undefined,
      }}
    >
      {opcoes.map((o) => {
        const ativo = o.valor === valor;
        return (
          <button
            key={o.valor}
            type="button"
            aria-pressed={ativo}
            onClick={() => aoMudar(o.valor)}
            style={{
              height: "var(--control-sm)",
              padding: "0 var(--space-4)",
              borderRadius: "var(--radius-pill)",
              border: "none",
              background: ativo ? "var(--surface-card)" : "transparent",
              color: ativo ? "var(--ink)" : "var(--ink-muted)",
              fontFamily: "inherit",
              fontSize: "var(--text-body-sm-size)",
              fontWeight: 500,
              cursor: "pointer",
            }}
          >
            {o.rotulo}
          </button>
        );
      })}
    </div>
  );
}
