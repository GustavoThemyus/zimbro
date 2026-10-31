"use client";

import type { ButtonHTMLAttributes } from "react";

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: "primaria" | "secundaria" | "fantasma";
  tamanho?: "md" | "lg" | "xl";
  largura?: "cheia" | "auto";
};

/**
 * Botao neutro de proposito: a acao mais importante e a mais contrastada, nao a
 * mais colorida. Cor de marca nunca preenche botao — ela vive na categoria.
 *
 * Altura minima 52px e piso de toque 44px: o produto e operado com o polegar,
 * em pe, no balcao. Botao apertado nao e elegante, e inacessivel.
 */
const ALTURAS = {
  md: "var(--control-md)",
  lg: "var(--control-lg)",
  xl: "var(--control-xl)",
} as const;

export function Botao({
  variante = "primaria",
  tamanho = "lg",
  largura = "cheia",
  style,
  ...resto
}: Props) {
  const cores = {
    primaria: { background: "var(--action-solid)", color: "var(--ink-inverse)", border: "none" },
    secundaria: {
      background: "var(--action-quiet)",
      color: "var(--ink)",
      border: "1px solid var(--border-strong)",
    },
    fantasma: { background: "transparent", color: "var(--ink)", border: "1px solid var(--border-strong)" },
  }[variante];

  return (
    <button
      {...resto}
      style={{
        ...cores,
        height: ALTURAS[tamanho],
        width: largura === "cheia" ? "100%" : undefined,
        padding: "0 var(--space-6)",
        borderRadius: "var(--radius-md)",
        fontFamily: "inherit",
        fontSize: "var(--text-body-strong-size)",
        fontWeight: 500,
        cursor: resto.disabled ? "not-allowed" : "pointer",
        opacity: resto.disabled ? 0.5 : 1,
        transition: "background 120ms ease",
        ...style,
      }}
    />
  );
}
