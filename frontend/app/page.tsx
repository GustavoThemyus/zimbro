"use client";

import { useEffect, useState } from "react";

type Ping = {
  app: string;
  horaDoBanco: string;
  migracoesAplicadas: number;
};

/**
 * Tela da semana 0. Existe para provar uma coisa so: navegador -> Next ->
 * Spring -> Postgres, com as migracoes aplicadas. Ela e apagada na semana 1,
 * quando a tela de lancamento rapido toma este lugar.
 */
export default function Page() {
  const [dados, setDados] = useState<Ping | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/ping")
      .then(async (r) => {
        const corpo = await r.json();
        if (!r.ok) throw new Error(corpo.erro ?? "Falhou.");
        return corpo as Ping;
      })
      .then(setDados)
      .catch((e: Error) => setErro(e.message));
  }, []);

  return (
    <main className="mx-auto flex max-w-[480px] flex-col gap-[var(--space-8)] px-[var(--space-4)] py-[var(--space-12)]">
      <header>
        <p
          className="font-mono uppercase text-[var(--text-label-micro-size)] tracking-[0.08em] text-ink-muted"
          style={{ fontWeight: 500 }}
        >
          Semana 0
        </p>
        <h1
          className="mt-[var(--space-2)] text-[var(--text-title-size)] tracking-[-0.02em] text-ink"
          style={{ fontWeight: 500 }}
        >
          Quanto entrou, quanto saiu, quanto sobrou.
        </h1>
      </header>

      <section
        className="rounded-[var(--radius-lg)] border border-border-hairline bg-surface-card p-[var(--space-5)]"
        aria-live="polite"
      >
        {erro === null && dados === null && (
          <p className="text-ink-muted">Falando com o banco…</p>
        )}

        {erro !== null && (
          <>
            <p
              className="text-[var(--text-body-strong-size)] text-danger"
              style={{ fontWeight: 500 }}
            >
              A corrente está quebrada.
            </p>
            <p className="mt-[var(--space-2)] text-ink-muted">{erro}</p>
          </>
        )}

        {dados !== null && (
          <>
            <p
              className="text-[var(--text-body-strong-size)] text-money-in"
              style={{ fontWeight: 500 }}
            >
              Next, Spring e Postgres estão conversando.
            </p>
            <dl className="mt-[var(--space-4)] grid gap-[var(--space-3)]">
              <div className="flex items-baseline justify-between gap-[var(--space-4)]">
                <dt className="text-[var(--text-body-sm-size)] text-ink-muted">
                  Hora do banco
                </dt>
                <dd className="tabular font-mono text-ink">
                  {new Date(dados.horaDoBanco).toLocaleString("pt-BR", {
                    timeZone: "America/Sao_Paulo",
                  })}
                </dd>
              </div>
              <div className="flex items-baseline justify-between gap-[var(--space-4)]">
                <dt className="text-[var(--text-body-sm-size)] text-ink-muted">
                  Migrações aplicadas
                </dt>
                <dd className="tabular font-mono text-ink">{dados.migracoesAplicadas}</dd>
              </div>
            </dl>
          </>
        )}
      </section>

      <p className="text-[var(--text-body-sm-size)] leading-[1.45] text-ink-muted">
        Esta tela é descartável. Na semana 1 ela dá lugar ao lançamento rápido, e
        este texto sai do repositório junto.
      </p>
    </main>
  );
}
