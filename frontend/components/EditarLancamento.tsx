"use client";

import { useState } from "react";

import { Botao } from "@/components/Botao";
import { Campo } from "@/components/Campo";
import { api } from "@/lib/api";
import { digitosParaCentavos, formatar } from "@/lib/dinheiro";
import type { Categoria, Lancamento } from "@/lib/tipos";

/** Folha de baixo para cima. Editar e apagar moram aqui, longe do toque sem querer. */
export function EditarLancamento({
  lancamento,
  categorias,
  aoFechar,
  aoMudar,
}: {
  lancamento: Lancamento;
  categorias: Categoria[];
  aoFechar: () => void;
  aoMudar: () => void;
}) {
  const [digitos, setDigitos] = useState(String(lancamento.valorCents));
  const [descricao, setDescricao] = useState(lancamento.descricao ?? "");
  const [categoriaId, setCategoriaId] = useState(lancamento.categoriaId ?? "");
  const [escopo, setEscopo] = useState(lancamento.escopo);
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [confirmandoApagar, setConfirmandoApagar] = useState(false);

  async function salvar() {
    setOcupado(true);
    setErro(null);
    try {
      await api(`lancamentos/${lancamento.id}`, {
        method: "PUT",
        body: JSON.stringify({
          contaId: lancamento.contaId,
          quando: lancamento.quando,
          valorCents: digitosParaCentavos(digitos),
          direcao: lancamento.direcao,
          descricao: descricao || null,
          categoriaId: categoriaId || null,
          escopo,
        }),
      });
      aoMudar();
      aoFechar();
    } catch (e) {
      setErro((e as Error).message);
      setOcupado(false);
    }
  }

  async function apagar() {
    setOcupado(true);
    try {
      await api(`lancamentos/${lancamento.id}`, { method: "DELETE" });
      aoMudar();
      aoFechar();
    } catch (e) {
      setErro((e as Error).message);
      setOcupado(false);
    }
  }

  return (
    <div
      role="dialog"
      aria-label="Editar lançamento"
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.35)",
        display: "flex",
        alignItems: "flex-end",
        justifyContent: "center",
        zIndex: 50,
      }}
      onClick={(e) => e.target === e.currentTarget && aoFechar()}
    >
      <div
        style={{
          width: "100%",
          maxWidth: 420,
          background: "var(--surface-raised)",
          borderRadius: "var(--radius-xl) var(--radius-xl) 0 0",
          boxShadow: "var(--shadow-overlay)",
          padding: "var(--space-6) var(--space-4) var(--space-8)",
          display: "grid",
          gap: "var(--space-5)",
        }}
      >
        <Campo
          rotulo="Valor"
          inputMode="numeric"
          value={formatar(digitosParaCentavos(digitos))}
          onChange={(e) => setDigitos(e.target.value)}
          style={{ fontFamily: "var(--font-mono)", fontSize: "var(--text-money-size)" }}
        />
        <Campo
          rotulo="Descrição"
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          placeholder="Opcional"
        />

        <div style={{ display: "grid", gap: "var(--space-2)" }}>
          <label
            htmlFor="categoria"
            style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}
          >
            Categoria
          </label>
          <select
            id="categoria"
            value={categoriaId}
            onChange={(e) => setCategoriaId(e.target.value)}
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
            <option value="">Sem categoria</option>
            {categorias.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </select>
        </div>

        <div style={{ display: "grid", gap: "var(--space-2)" }}>
          <span style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}>
            É do negócio ou seu?
          </span>
          <div style={{ display: "flex", gap: "var(--space-2)" }}>
            {(["BUSINESS", "PERSONAL"] as const).map((v) => (
              <button
                key={v}
                type="button"
                aria-pressed={escopo === v}
                onClick={() => setEscopo(v)}
                style={{
                  flex: 1,
                  height: "var(--control-sm)",
                  borderRadius: "var(--radius-pill)",
                  border: `1px solid ${escopo === v ? "var(--ink)" : "var(--border-hairline)"}`,
                  background: escopo === v ? "var(--action-quiet)" : "transparent",
                  color: "var(--ink)",
                  fontFamily: "inherit",
                  fontSize: "var(--text-body-sm-size)",
                  fontWeight: 500,
                  cursor: "pointer",
                }}
              >
                {v === "BUSINESS" ? "Negócio" : "Pessoal"}
              </button>
            ))}
          </div>
        </div>

        {erro && (
          <p style={{ color: "var(--danger)", fontSize: "var(--text-body-sm-size)", margin: 0 }}>
            {erro}
          </p>
        )}

        <Botao onClick={salvar} disabled={ocupado}>
          Salvar
        </Botao>

        {confirmandoApagar ? (
          <div style={{ display: "grid", gap: "var(--space-2)" }}>
            <p style={{ margin: 0, fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}>
              Apagar este lançamento?
            </p>
            <div style={{ display: "flex", gap: "var(--space-2)" }}>
              <Botao
                variante="fantasma"
                tamanho="md"
                onClick={apagar}
                disabled={ocupado}
                style={{ color: "var(--danger)", borderColor: "var(--danger)" }}
              >
                Apagar
              </Botao>
              <Botao variante="fantasma" tamanho="md" onClick={() => setConfirmandoApagar(false)}>
                Cancelar
              </Botao>
            </div>
          </div>
        ) : (
          <Botao variante="fantasma" tamanho="md" onClick={() => setConfirmandoApagar(true)}>
            Apagar
          </Botao>
        )}
      </div>
    </div>
  );
}
