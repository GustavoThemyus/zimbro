import { formatar } from "@/lib/dinheiro";

type Props = {
  centavos: number;
  tamanho?: "linha" | "grande" | "heroi";
  cor?: "neutro" | "entrada" | "problema";
  sinal?: boolean;
};

const TAMANHOS = {
  linha: { fontSize: "var(--text-money-size)", letterSpacing: "0" },
  grande: { fontSize: "var(--text-money-lg-size)", letterSpacing: "-0.02em" },
  heroi: { fontSize: "var(--text-money-hero-size)", letterSpacing: "-0.03em" },
} as const;

const CORES = {
  neutro: "var(--ink)",
  entrada: "var(--money-in)",
  problema: "var(--danger)",
} as const;

/**
 * Entrada e verde. Saida e NEUTRA, nao vermelha. Vermelho so para problema de
 * verdade: limite estourado, saldo negativo, DAS vencido. Pintar toda saida de
 * vermelho faz um negocio saudavel parecer prejuizo e esvazia o vermelho.
 *
 * Mono com tabular-nums sempre: digito de largura variavel desalinha a coluna
 * de valores e torna a conferencia contra o extrato impossivel.
 */
export function Valor({ centavos, tamanho = "linha", cor = "neutro", sinal = false }: Props) {
  const prefixo = sinal ? (centavos < 0 ? "− " : "+ ") : "";

  return (
    <span
      style={{
        fontFamily: "var(--font-mono)",
        fontVariantNumeric: "tabular-nums",
        fontWeight: 500,
        lineHeight: 1.05,
        color: CORES[cor],
        whiteSpace: "nowrap",
        ...TAMANHOS[tamanho],
      }}
    >
      {prefixo}
      <span style={{ fontSize: "0.62em", color: "var(--ink-muted)", marginRight: 2 }}>R$</span>
      {formatar(centavos)}
    </span>
  );
}
