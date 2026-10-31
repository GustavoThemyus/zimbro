-- V3 — O que a semana 1 precisa: categoria, lancamento e sessao de usuario.

-- ---------------------------------------------------------------- categorias

-- workspace_id nulo = categoria de fabrica, compartilhada por todos os clientes.
-- Quando o usuario "edita" uma de fabrica, o sistema COPIA para o workspace dele
-- (derived_from_id aponta para a original). Usuario nunca escreve em linha global.
create table category (
    id              uuid        primary key default uuidv7(),
    workspace_id    uuid        references workspace (id) on delete cascade,
    parent_id       uuid        references category (id),
    name            varchar(60) not null,
    slug            varchar(60) not null,
    -- kind e o campo que faz o relatorio ser correto: transferencia nao e despesa,
    -- e retirada do dono (OWNER_DRAW) nao e despesa do negocio — e pro-labore.
    kind            varchar(16) not null
        constraint category_kind_ck
        check (kind in ('INCOME', 'EXPENSE', 'TRANSFER', 'OWNER_DRAW', 'TAX')),
    default_scope   varchar(16) not null
        constraint category_default_scope_ck
        check (default_scope in ('BUSINESS', 'PERSONAL', 'MIXED')),
    -- Token de cor do design system (cat-teal, cat-lime, ...). A cor vive na
    -- categoria, nunca no botao: e ela que da ritmo visual a lista do mes.
    color_token     varchar(24) not null default 'cat-slate'
        constraint category_color_ck
        check (color_token in ('cat-teal', 'cat-blue', 'cat-violet', 'cat-rose', 'cat-lime', 'cat-slate')),
    derived_from_id uuid        references category (id),
    is_system       boolean     not null default false,
    sort_order      int         not null default 0,
    archived_at     timestamptz,
    created_at      timestamptz not null default now()
);

create unique index category_slug_global_uk on category (slug) where workspace_id is null;
create unique index category_slug_ws_uk on category (workspace_id, slug) where workspace_id is not null;
create index category_workspace_idx on category (workspace_id) where archived_at is null;

-- ---------------------------------------------------------------- lancamentos

create table transaction (
    id                  uuid        primary key default uuidv7(),
    workspace_id        uuid        not null references workspace (id) on delete cascade,
    account_id          uuid        not null references account (id),

    -- data do FATO. E por ela que o fechamento mensal soma. Sem hora e sem fuso:
    -- virando timestamp, uma venda do dia 31 cai no dia 1 e o fechamento mente.
    occurred_on         date        not null,
    posted_at           timestamptz,

    -- valor sempre positivo + direcao explicita. Valor com sinal deixa a soma mais
    -- curta e gera o bug mais comum de dev iniciante em financas: sinal invertido,
    -- que passa despercebido porque o numero PARECE plausivel.
    amount_cents        bigint      not null constraint transaction_amount_ck check (amount_cents > 0),
    direction           varchar(8)  not null
        constraint transaction_direction_ck check (direction in ('INFLOW', 'OUTFLOW')),
    signed_amount_cents bigint generated always as
        (case when direction = 'INFLOW' then amount_cents else -amount_cents end) stored,

    description_raw     text,
    description         varchar(140),
    category_id         uuid        references category (id),

    scope               varchar(16) not null
        constraint transaction_scope_ck check (scope in ('BUSINESS', 'PERSONAL', 'SPLIT')),

    -- Nem toda entrada e faturamento: transferencia da conta pessoal para a do
    -- negocio, estorno e emprestimo entram dinheiro e NAO contam para o teto.
    -- Confundir isso gera alarme falso de estouro de limite, que tem consequencia
    -- legal — e alarme falso ai destroi a confianca de uma vez.
    counts_as_revenue   boolean     not null default false,

    status              varchar(20) not null default 'CONFIRMED'
        constraint transaction_status_ck check (status in ('CONFIRMED', 'PENDING_REVIEW')),
    review_due_on       date,
    transfer_group_id   uuid,
    source              varchar(16) not null default 'MANUAL'
        constraint transaction_source_ck check (source in ('MANUAL', 'OFX', 'CSV', 'RECURRENCE')),

    -- A decisao atual do motor, desnormalizada para leitura rapida. O historico
    -- completo vive em categorization_decision, que nasce na semana 3.
    categorization_source       varchar(32),
    categorization_confidence   smallint
        constraint transaction_confidence_ck
        check (categorization_confidence between 0 and 100),
    decided_by          varchar(16)
        constraint transaction_decided_by_ck
        check (decided_by in ('USER', 'SYSTEM_RULE', 'SYSTEM_AUTO')),
    decided_at          timestamptz,

    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    deleted_at          timestamptz
);

create index transaction_mes_idx on transaction (workspace_id, occurred_on desc) where deleted_at is null;
create index transaction_pendentes_idx on transaction (workspace_id, review_due_on) where status = 'PENDING_REVIEW' and deleted_at is null;
create index transaction_conta_idx on transaction (workspace_id, account_id, occurred_on) where deleted_at is null;
create index transaction_categoria_idx on transaction (workspace_id, category_id, occurred_on) where deleted_at is null;

-- ---------------------------------------------------------------- sessoes

-- Guardamos o HASH do token, nunca o token. Se o banco vazar, as sessoes nao
-- viram chave de entrada — do jeito que ja fazemos com senha.
create table user_session (
    id           uuid        primary key default uuidv7(),
    user_id      uuid        not null references app_user (id) on delete cascade,
    token_hash   varchar(64) not null unique,
    created_at   timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    expires_at   timestamptz not null,
    revoked_at   timestamptz
);

create index user_session_user_idx on user_session (user_id);

create table password_reset_token (
    id         uuid        primary key default uuidv7(),
    user_id    uuid        not null references app_user (id) on delete cascade,
    token_hash varchar(64) not null unique,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    used_at    timestamptz
);
