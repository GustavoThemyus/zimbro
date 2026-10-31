"use client";

import { Valor } from "@/components/Valor";
import type { Lancamento } from "@/lib/tipos";

const INICIAIS: Record<string, string> = {
  INCOME: "↓",
  EXPENSE: "↑",
  OWNER_DRAW: "◆",
  TRANSFER: "⇄",
  TAX: "§",
};

/**
 * Linha de 72px com poco de icone de 48px. A cor da categoria vive AQUI — e o
 * que da ritmo visual a lista sem colorir um unico controle.
 */
export function LinhaLancamento({
  lancamento,
  aoTocar,
}: {
  lancamento: Lancamento;
  aoTocar: () => void;
}) {
  const cor = lancamento.cor ?? "cat-slate";
  const entrada = lancamento.direcao === "INFLOW";

  return (
    <button
      type="button"
      onClick={aoTocar}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "var(--space-4)",
        width: "100%",
        minHeight: "var(--row-min)",
        padding: "var(--space-3) 0",
        background: "transparent",
        border: "none",
        borderBottom: "1px solid var(--border-hairline)",
        textAlign: "left",
        cursor: "pointer",
        fontFamily: "inherit",
      }}
    >
      <span
        aria-hidden
        style={{
          width: "var(--icon-well)",
          height: "var(--icon-well)",
          flex: "none",
          display: "grid",
          placeItems: "center",
          borderRadius: "var(--radius-pill)",
          background: `var(--${cor}-quiet)`,
          color: `var(--${cor})`,
          fontSize: 20,
        }}
      >
        {INICIAIS[lancamento.tipoCategoria ?? ""] ?? "•"}
      </span>

      <span style={{ flex: 1, minWidth: 0 }}>
        <span
          style={{
            display: "block",
            fontSize: "var(--text-body-strong-size)",
            fontWeight: 500,
            color: "var(--ink)",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {lancamento.descricao ?? lancamento.categoria ?? "Sem descrição"}
        </span>
        <span
          style={{
            display: "block",
            fontSize: "var(--text-body-sm-size)",
            color: "var(--ink-muted)",
            marginTop: 2,
          }}
        >
          {new Date(`${lancamento.quando}T12:00:00`).toLocaleDateString("pt-BR", {
            day: "2-digit",
            month: "short",
          })}
          {" · "}
          {lancamento.conta}
          {lancamento.escopo === "PERSONAL" ? " · pessoal" : ""}
        </span>
      </span>

      <Valor centavos={lancamento.valorCents} cor={entrada ? "entrada" : "neutro"} />
    </button>
  );
}
