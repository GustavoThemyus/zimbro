-- V1 — Fundacao: quem e o cliente, quem sao os usuarios, onde o dinheiro esta,
-- e os parametros legais. Lancamento, categoria e regra entram nas semanas 1 a 3.
--
-- Convencoes que valem para o schema inteiro:
--   dinheiro  -> bigint em centavos, nunca float
--   fato      -> date (sem hora, sem fuso); relogio de sistema -> timestamptz
--   id        -> uuidv7() (aleatorio mas ordenado por tempo, nativo no Postgres 18)
--   enum      -> varchar + check, porque alterar tipo ENUM em migracao e sofrimento

create extension if not exists btree_gist;

-- ---------------------------------------------------------------- workspace

create table workspace (
    id              uuid        primary key default uuidv7(),
    name            varchar(120) not null,
    -- o MEI as vezes nao lembra o CNPJ no cadastro; nao bloqueia
    cnpj            varchar(14),
    -- no ano de abertura o limite anual e proporcional aos meses de atividade:
    -- sem esta data o alerta de teto mente para todo cliente novo
    mei_opened_on   date,
    -- o valor do DAS muda conforme a atividade (comercio paga ICMS, servico paga ISS)
    activity_type   varchar(32) not null
        constraint workspace_activity_type_ck
        check (activity_type in ('COMERCIO', 'SERVICOS', 'COMERCIO_SERVICOS', 'TRANSPORTE')),
    timezone        varchar(64) not null default 'America/Sao_Paulo',
    -- cobranca manual via Pix no MVP: esta data e atualizada a mao depois do pagamento
    paid_until      date,
    created_at      timestamptz not null default now()
);

comment on column workspace.paid_until is
    'Trava de assinatura. Atualizada a mao apos o Pix. Vencida = acesso bloqueado no login.';

-- ---------------------------------------------------------------- usuarios

create table app_user (
    id              uuid        primary key default uuidv7(),
    email           varchar(255) not null,
    password_hash   varchar(100) not null,
    name            varchar(120) not null,
    phone           varchar(20),
    created_at      timestamptz not null default now(),
    last_login_at   timestamptz
);

-- unico ignorando maiusculas: ninguem perde a conta por ter digitado Joao@ em vez de joao@
create unique index app_user_email_uk on app_user (lower(email));

-- O dono dos dados e o workspace, nao o usuario. Custa esta tabela hoje e evita
-- migrar tudo no dia em que a esposa ou o contador precisarem olhar.
create table workspace_member (
    workspace_id    uuid        not null references workspace (id) on delete cascade,
    user_id         uuid        not null references app_user (id) on delete cascade,
    role            varchar(16) not null
        constraint workspace_member_role_ck check (role in ('OWNER', 'MEMBER', 'VIEWER')),
    created_at      timestamptz not null default now(),
    primary key (workspace_id, user_id)
);

create index workspace_member_user_idx on workspace_member (user_id);

-- ---------------------------------------------------------------- contas

create table account (
    id                      uuid        primary key default uuidv7(),
    workspace_id            uuid        not null references workspace (id) on delete cascade,
    name                    varchar(80) not null,
    -- CASH nao e detalhe: barbearia e confeitaria recebem muito em dinheiro vivo
    type                    varchar(16) not null
        constraint account_type_ck
        check (type in ('CHECKING', 'SAVINGS', 'CASH', 'CREDIT_CARD', 'WALLET')),
    -- MIXED e a situacao real da maioria: uma conta pessoal usada para tudo
    ownership               varchar(16) not null
        constraint account_ownership_ck check (ownership in ('BUSINESS', 'PERSONAL', 'MIXED')),
    institution_code        varchar(20),
    opening_balance_cents   bigint      not null default 0,
    currency                char(3)     not null default 'BRL'
        constraint account_currency_ck check (currency = 'BRL'),
    archived_at             timestamptz,
    created_at              timestamptz not null default now()
);

-- Saldo nao e coluna: e saldo inicial + soma dos lancamentos, calculado na hora.
-- Coluna de saldo desincroniza com qualquer bug de concorrencia e o cliente ve numero errado.
create index account_workspace_idx on account (workspace_id) where archived_at is null;

-- ---------------------------------------------------------------- parametros legais

-- Nenhum numero de lei mora no codigo. Esta tabela responde "quanto era o DAS em
-- marco de 2025?", nao apenas "quanto e o DAS?" — relatorio de mes passado nao pode
-- ser reescrito quando a lei muda.
create table legal_parameter (
    id          uuid        primary key default uuidv7(),
    param_key   varchar(64) not null,
    valid_from  date        not null,
    valid_to    date,
    value       jsonb       not null,
    source_note text,
    created_at  timestamptz not null default now(),
    constraint legal_parameter_period_ck check (valid_to is null or valid_to >= valid_from)
);

-- Dois valores da mesma chave nunca podem valer no mesmo dia. O banco recusa,
-- em vez de o calculo escolher um dos dois em silencio.
alter table legal_parameter
    add constraint legal_parameter_no_overlap
    exclude using gist (param_key with =, daterange(valid_from, valid_to, '[]') with &&);
