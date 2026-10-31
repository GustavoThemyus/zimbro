// Gera app/tokens.css a partir de design/tokens.json.
//
// design/tokens.json e uma copia do arquivo do design system (artifact "Sistema
// visual MEI"). Mexeu no design system? Troque este arquivo e rode `npm run tokens`.
// Ninguem edita app/tokens.css a mao.

import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const raiz = join(dirname(fileURLToPath(import.meta.url)), "..");
const t = JSON.parse(readFileSync(join(raiz, "design/tokens.json"), "utf8"));

/** "{ink}" -> "var(--ink)"; qualquer outra coisa passa direto. */
const resolver = (v) => {
  const alias = /^\{(.+)\}$/.exec(String(v));
  return alias ? `var(--${alias[1]})` : v;
};

const linhas = (pares) => pares.map(([k, v]) => `  --${k}: ${resolver(v)};`).join("\n");

const corPorTema = (tema) =>
  t.color.tokens.map((tok) => [tok.name, tok.value[tema] ?? tok.value]);

const sombraPorTema = (tema) =>
  t.shadow.tokens.map((tok) => [tok.name, tok.value[tema] ?? tok.value]);

// Escalas sem tema: tipografia, controle, espaco, raio.
const invariantes = [
  ...Object.entries(t.type.families).map(([k, v]) => [`font-${k}`, v]),
  ...t.type.groups.flatMap((g) =>
    g.styles.flatMap((s) => [
      [`text-${s.name}-size`, s.fontSize],
      [`text-${s.name}-line`, String(s.lineHeight)],
      [`text-${s.name}-weight`, String(s.fontWeight)],
      ...(s.letterSpacing ? [[`text-${s.name}-tracking`, s.letterSpacing]] : []),
    ]),
  ),
  ...t.control.tokens.map((tok) => [tok.name, tok.value]),
  ...t.spacing.tokens.map((tok) => [tok.name, tok.value]),
  ...t.radius.tokens.map((tok) => [tok.name, tok.value]),
];

// Mapa para shadcn/ui e Origin UI, que esperam estes nomes.
const shadcn = [
  ["background", "var(--surface-canvas)"],
  ["foreground", "var(--ink)"],
  ["card", "var(--surface-card)"],
  ["card-foreground", "var(--ink)"],
  ["popover", "var(--surface-raised)"],
  ["popover-foreground", "var(--ink)"],
  ["primary", "var(--action-solid)"],
  ["primary-foreground", "var(--ink-inverse)"],
  ["secondary", "var(--action-quiet)"],
  ["secondary-foreground", "var(--ink)"],
  ["muted", "var(--surface-sunken)"],
  ["muted-foreground", "var(--ink-muted)"],
  ["accent", "var(--action-quiet)"],
  ["accent-foreground", "var(--ink)"],
  ["destructive", "var(--danger)"],
  ["destructive-foreground", "var(--ink-inverse)"],
  ["border", "var(--border-hairline)"],
  ["input", "var(--border-strong)"],
  ["ring", "var(--focus-ring)"],
  ["radius", "var(--radius-md)"],
];

// Mapa para utilitarias do Tailwind: --color-ink vira bg-ink, text-ink, border-ink...
const tailwind = t.color.tokens.map((tok) => [`color-${tok.name}`, `var(--${tok.name})`]);

const css = `/* GERADO por scripts/build-tokens.mjs a partir de design/tokens.json.
   Nao edite a mao: rode \`npm run tokens\`.
   Fonte: ${t.name}, versao ${t.version}. */

:root {
${linhas(corPorTema("light"))}
${linhas(sombraPorTema("light"))}

${linhas(invariantes)}

${linhas(shadcn)}
}

/* Escuro automatico pela preferencia do sistema, a nao ser que o usuario tenha
   escolhido claro. Claro e o padrao: o cliente usa o app em pe, no balcao, sob sol. */
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
${linhas(corPorTema("dark"))}
${linhas(sombraPorTema("dark"))}
  }
}

/* Escuro escolhido a mao. */
:root[data-theme="dark"] {
${linhas(corPorTema("dark"))}
${linhas(sombraPorTema("dark"))}
}

@theme inline {
${linhas(tailwind)}
}
`;

writeFileSync(join(raiz, "app/tokens.css"), css);
console.log(
  `app/tokens.css: ${t.color.tokens.length} cores x 2 temas, ${invariantes.length} escalas.`,
);
