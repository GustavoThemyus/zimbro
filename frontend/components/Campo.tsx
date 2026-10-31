"use client";

import type { InputHTMLAttributes, ReactNode } from "react";
import { useId } from "react";

type Props = InputHTMLAttributes<HTMLInputElement> & {
  rotulo: string;
  ajuda?: ReactNode;
  erro?: string | null;
};

/**
 * Rotulo SEMPRE visivel. Placeholder como rotulo desaparece no momento em que a
 * pessoa comeca a digitar, que e exatamente quando ela precisa conferir o que o
 * campo pedia.
 *
 * Fonte de 16px nao e escolha estetica: abaixo disso o iOS da zoom automatico
 * ao focar o campo, e a tela inteira pula.
 */
export function Campo({ rotulo, ajuda, erro, style, ...resto }: Props) {
  const id = useId();

  return (
    <div style={{ display: "grid", gap: "var(--space-2)" }}>
      <label
        htmlFor={id}
        style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)" }}
      >
        {rotulo}
      </label>
      <input
        id={id}
        {...resto}
        style={{
          height: "var(--control-md)",
          padding: "0 var(--space-4)",
          borderRadius: "var(--radius-md)",
          border: `1px solid ${erro ? "var(--danger)" : "var(--border-strong)"}`,
          background: "var(--surface-sunken)",
          color: "var(--ink)",
          fontFamily: "inherit",
          fontSize: "16px",
          width: "100%",
          ...style,
        }}
      />
      {erro ? (
        <p style={{ fontSize: "var(--text-body-sm-size)", color: "var(--danger)", margin: 0 }}>
          {erro}
        </p>
      ) : ajuda ? (
        <p style={{ fontSize: "var(--text-body-sm-size)", color: "var(--ink-muted)", margin: 0 }}>
          {ajuda}
        </p>
      ) : null}
    </div>
  );
}
