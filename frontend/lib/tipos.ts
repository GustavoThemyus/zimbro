export type Conta = {
  id: string;
  nome: string;
  tipo: string;
  posse: string;
  saldoInicialCents: number;
};

export type Categoria = {
  id: string;
  nome: string;
  slug: string;
  tipo: "INCOME" | "EXPENSE" | "TRANSFER" | "OWNER_DRAW" | "TAX";
  escopoPadrao: "BUSINESS" | "PERSONAL" | "MIXED";
  cor: string;
};

export type Lancamento = {
  id: string;
  quando: string;
  valorCents: number;
  direcao: "INFLOW" | "OUTFLOW";
  descricao: string | null;
  escopo: "BUSINESS" | "PERSONAL";
  categoriaId: string | null;
  categoria: string | null;
  cor: string | null;
  tipoCategoria: string | null;
  contaId: string;
  conta: string;
};

export type Mes = {
  ano: number;
  mes: number;
  resumo: {
    entrouCents: number;
    saiuCents: number;
    retiradaCents: number;
    pessoalCents: number;
    sobrouCents: number;
  };
  lancamentos: Lancamento[];
};
