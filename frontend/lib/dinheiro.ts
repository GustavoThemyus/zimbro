/**
 * Dinheiro e sempre inteiro de centavos, do banco ate a tela. Nenhuma conta
 * neste projeto passa por ponto flutuante: 0,1 + 0,2 em float nao da 0,3, e
 * somar mil lancamentos assim gera diferenca de centavos que o cliente encontra
 * na primeira conferencia contra o extrato.
 */

const BRL = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** 123456 -> "1.234,56" */
export function formatar(centavos: number): string {
  return BRL.format(Math.abs(centavos) / 100);
}

/** 123456 -> "R$ 1.234,56" */
export function formatarComMoeda(centavos: number): string {
  return `R$ ${formatar(centavos)}`;
}

/** "1234" digitado no teclado -> 1234 centavos (R$ 12,34). */
export function digitosParaCentavos(digitos: string): number {
  const limpo = digitos.replace(/\D/g, "").slice(0, 11);
  return limpo === "" ? 0 : Number.parseInt(limpo, 10);
}

export const hoje = (): string => new Date().toLocaleDateString("sv-SE");
