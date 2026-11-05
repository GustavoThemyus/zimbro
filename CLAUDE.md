# Plano técnico — SaaS de controle financeiro para MEI

## Contexto

Vocês vão construir um SaaS de controle financeiro para MEI, vendido presencialmente em
João Pessoa a R$ 49/mês, com meta de 93 assinantes em 12 meses. O produto responde três
perguntas que o MEI hoje não consegue responder: *quanto sobrou pra mim*, *o que é meu e o
que é do negócio*, e *estou perto de estourar o limite anual*.

O diferencial não é integração bancária — é um **motor de categorização que nunca erra duas
vezes a mesma coisa**. Toda a arquitetura abaixo é organizada em volta disso.

Restrições que moldam o plano: 2 devs iniciantes, 30h/semana **somadas** (≈150 pessoa-horas
no total do MVP), R$ 150/mês de infra, 5 semanas sem extensão de prazo. Essas 150 horas são
o número mais importante deste documento — ele é que define o que entra e o que sai.

Stack já decidida: Java 21 + Spring Boot 4 + PostgreSQL 18 no backend, Next.js (App Router)
como PWA instalável, entrada manual + importação de OFX/CSV, sem Open Finance, sem
documento fiscal.

> **Nota de 05/11/2026.** Este plano foi escrito pedindo Spring Boot 3 e PostgreSQL 16; o
> código usa **Boot 4.1.2** e **PostgreSQL 18**, e o texto abaixo já está corrigido. Boot 4
> porque o Initializr não serve mais a linha 3 e a 3.5 saiu do suporte grátis — a
> consequência prática é Jackson 3 (`tools.jackson.databind`). PG 18 porque traz `uuidv7()`
> nativo, que é exatamente o que a seção 1 pede. As mesmas divergências estão numa tabela no
> `README.md`.

---

## 0. Decisões de infraestrutura (já resolvidas por mim)

### Onde hospedar

**Decisão: uma única VPS em São Paulo (2–4 GB RAM) rodando Docker Compose.**

Os dois lados:

| Opção | A favor | Contra |
|---|---|---|
| PaaS gerenciado (Railway, Render, Fly) | zero ops, deploy por git push, backup automático | US$ 12–25/mês já no começo, banco grátis expira, cold start, você não aprende nada que ajuda depois |
| VPS única com Docker Compose | R$ 30–90/mês total, tudo num lugar, latência de ~40ms em João Pessoa, controle total | vocês são responsáveis por backup, HTTPS, atualização de SO; se a máquina morre, o produto morre junto |

Recomendo a VPS porque o gargalo de vocês é **dinheiro e previsibilidade de custo**, não
tempo de ops — e com Docker Compose o ops real são ~4 horas de setup na semana 0 e depois
quase nada. A latência pesa: um servidor na Europa (Hetzner, mais barato) fica a ~200ms de
João Pessoa, contra ~40ms em São Paulo. Num PWA que o barbeiro usa entre dois cortes, 200ms
em cada toque é percebido como "travando". São Paulo.

Composição da máquina: **Caddy** (servidor web que resolve HTTPS automático, sem você
configurar certificado) + container do Spring Boot + container do Next.js + **PostgreSQL 18
em container** com volume no disco. Custo estimado: VPS R$ 50–90 + domínio ~R$ 40/ano +
backup R$ 0 (Cloudflare R2 ou Backblaze B2 têm 10 GB grátis) = **dentro do orçamento com
folga de mais da metade**.

Backup: `pg_dump` comprimido todas as noites via cron, enviado pro R2, com retenção de 30
dias. **Backup que nunca foi restaurado não é backup** — na semana 5 há um item obrigatório
de restaurar o dump numa máquina limpa.

Alternativa de R$ 0 que vale testar na semana 0: **Oracle Cloud Always Free** tem uma
instância ARM generosa (4 vCPU / 24 GB) na região de São Paulo, grátis para sempre. O
problema é indisponibilidade crônica de capacidade na criação e risco de a conta ser
reciclada. Vale 1 hora de tentativa; se não criar na primeira tarde, paga a VPS e segue.

### Armadilha de custo que quase pega todo mundo

**Não hospedem o Next.js no plano Hobby da Vercel.** O Hobby é explicitamente proibido para
uso comercial — e "comercial" na política deles é amplo o bastante para pegar um SaaS
pago. O plano Pro custa US$ 20/mês por assento (~R$ 110), o que come o orçamento inteiro de
infra. Como a VPS já vai existir, o Next.js roda nela, no mesmo Compose. Se um dia quiserem
CDN na frente, Cloudflare na frente da VPS resolve de graça.

### Pipeline de deploy

GitHub Actions: a cada push na `main`, roda testes → constrói as duas imagens Docker →
publica no GHCR (registro de imagens do GitHub, grátis) → conecta por SSH na VPS e roda
`docker compose pull && docker compose up -d`. São ~40 linhas de YAML e elimina deploy
manual, que é fonte número um de "funcionava na minha máquina".

### Observabilidade mínima (não é luxo, é o que evita vocês descobrirem bug pelo cliente)

- **Sentry** (plano grátis) para erro de exceção no backend e no frontend.
- **UptimeRobot** (grátis) pingando `/actuator/health` a cada 5 min, alerta no Telegram.
- Logs em JSON estruturado no stdout, lidos com `docker compose logs`. Sem ELK, sem Grafana.

---

## 1. Modelagem de dados

### Princípios que valem para o schema inteiro

**Multi-tenant por workspace.** *Multi-tenant* = vários clientes dividindo o mesmo banco de
dados. Toda tabela de dados de cliente carrega uma coluna `workspace_id` obrigatória — é o
crachá de quem é o dono daquela linha. Nenhuma consulta roda sem filtrar por ela.

Por que *workspace* e não direto *usuário*: o MEI frequentemente tem a esposa mexendo
também, e mais pra frente um contador querendo olhar. Se o dono dos dados é o usuário,
compartilhar depois obriga a migrar tudo. O workspace custa uma tabela a mais hoje e
resolve esse problema para sempre. E atenção: a separação pessoal/negócio **não** é feita
com dois workspaces — ela vive dentro de um workspace só, porque os relatórios precisam
somar e comparar os dois lados.

**Dinheiro em centavos inteiros (`bigint`), nunca `float`/`double`.** Número decimal de
ponto flutuante não representa R$ 0,10 exatamente; somar mil deles dá diferença de centavos
e o cliente perde a confiança no produto na primeira conferência. R$ 1.234,56 é guardado
como `123456`.

**Datas: `date` para o que é fato do negócio, `timestamptz` para o que é relógio do
sistema.** A data em que a venda aconteceu não tem hora nem fuso — se virar timestamp,
alguém vai ver uma venda do dia 31 cair no dia 1º do mês seguinte e o fechamento mensal
mente. Fuso da aplicação: `America/Sao_Paulo`.

**IDs: UUID v7.** *UUID v7* é um identificador aleatório mas ordenado por tempo — tem a
vantagem do UUID (não dá pra adivinhar o ID do vizinho, e o celular pode gerar o ID
offline antes de sincronizar) sem o problema de performance do UUID aleatório em índice.

**Enums como texto com restrição (`varchar(32)` + `CHECK`), não como tipo ENUM do
Postgres.** O ENUM nativo é chato de alterar em migração; texto com check é trivial de
evoluir e vocês vão evoluir muito nas primeiras semanas.

**Append-only onde a confiança importa.** *Append-only* = só insere, nunca altera nem
apaga. As decisões do motor e as correções do usuário são append-only: um relatório de
março precisa continuar se explicando com a regra de março, não com a regra de hoje.

### 1.1 Identidade e tenancy

**`workspace`** — o cliente.
`id`, `name`, `cnpj` (nulo; o MEI às vezes nem lembra), `mei_opened_on` (data de abertura),
`activity_type` (COMERCIO | SERVICOS | COMERCIO_SERVICOS | TRANSPORTE), `timezone`,
`paid_until` (data; cobrança manual no MVP), `created_at`.

`activity_type` existe porque o valor do DAS muda conforme a atividade (comércio paga ICMS,
serviço paga ISS). `mei_opened_on` existe porque no ano de abertura o limite anual é
proporcional aos meses — sem esse campo o alerta de limite mente para todo cliente novo.

**`app_user`** — `id`, `email` (único), `password_hash`, `name`, `phone`, `created_at`,
`last_login_at`.

**`workspace_member`** — `workspace_id`, `user_id`, `role` (OWNER | MEMBER | VIEWER),
`created_at`. Chave primária composta. No MVP só existe OWNER, mas a tabela já nasce porque
adicioná-la depois exige migrar dados; criá-la agora custa zero.

### 1.2 Contas

**`account`** — onde o dinheiro está.
`id`, `workspace_id`, `name`, `type` (CHECKING | SAVINGS | CASH | CREDIT_CARD | WALLET),
`ownership` (BUSINESS | PERSONAL | MIXED), `institution_code`, `opening_balance_cents`,
`currency` (fixo 'BRL'), `archived_at`.

`type = CASH` não é detalhe: barbearia e confeitaria recebem muito em dinheiro vivo, e se o
produto não trata dinheiro na mão como conta de primeira classe ele não serve ao público
alvo. `ownership = MIXED` é a situação real da maioria — uma conta pessoal usada para tudo.

**Saldo não é coluna.** Saldo é `opening_balance` + soma dos lançamentos, calculado na hora.
Os dois lados: saldo guardado em coluna é rápido mas desincroniza com qualquer bug de
concorrência e aí o cliente vê número errado; saldo calculado é sempre correto mas custa uma
soma. Com o volume real (um MEI faz 50–300 lançamentos/mês, ou seja ~3.600/ano) a soma com
índice é instantânea. Recomendo calculado. Se um dia ficar lento, a solução é uma tabela de
*snapshot* mensal, não uma coluna mutável.

### 1.3 O lançamento — o evento central

**`transaction`**

| Campo | Por que existe |
|---|---|
| `id`, `workspace_id`, `account_id` | identidade e dono |
| `occurred_on` (date) | data do fato — é por ela que o fechamento mensal soma |
| `posted_at` (timestamptz, nulo) | quando caiu no extrato; pode diferir da data do fato |
| `amount_cents` (bigint, positivo) | valor absoluto |
| `direction` (INFLOW \| OUTFLOW) | entrou ou saiu |
| `signed_amount_cents` (coluna gerada) | `amount * (direction=INFLOW ? 1 : -1)`, para somar sem `CASE` em toda query |
| `description_raw` | exatamente o que veio do banco, nunca alterado |
| `description` | versão limpa/editada pelo usuário |
| `merchant_id` (nulo) | quem é o estabelecimento, já normalizado |
| `category_id` (nulo) | a categoria |
| `scope` (BUSINESS \| PERSONAL \| SPLIT) | a separação central do produto |
| `counts_as_revenue` (bool) | se conta pro limite anual do MEI |
| `status` (CONFIRMED \| PENDING_REVIEW) | fila de revisão |
| `review_due_on` (date, nulo) | prazo dos 7 dias |
| `transfer_group_id` (nulo) | emparelha transferência entre contas próprias |
| `source` (MANUAL \| OFX \| CSV \| RECURRENCE) | de onde veio |
| `import_batch_id` (nulo), `external_id` (nulo), `fingerprint` | deduplicação de importação |
| `categorization_source`, `categorization_confidence`, `decided_by`, `decided_at` | a decisão atual, desnormalizada para leitura rápida |
| `created_at`, `updated_at`, `deleted_at` | auditoria e desfazer |

Três campos merecem explicação:

**`direction` + valor positivo, em vez de valor com sinal.** Valor com sinal deixa a soma
mais curta, mas gera bug de sinal invertido — o erro mais comum de dev iniciante em
finanças, e o mais difícil de perceber porque o número *parece* plausível. Com
`direction` a intenção é explícita no banco, e a coluna gerada `signed_amount_cents` dá a
soma curta de graça. Recomendo essa combinação.

**`counts_as_revenue` separado de "é entrada".** Nem toda entrada é faturamento: transferência
da conta pessoal pra conta do negócio, estorno de compra, empréstimo e devolução entram
dinheiro e **não** contam pro limite dos R$ 81 mil. Se o produto confundir isso, ele dá
alarme falso de estouro de limite — e alarme falso em algo com consequência legal destrói a
confiança de uma vez. Esse é um campo de valor alto.

**`fingerprint`.** *Fingerprint* = uma impressão digital calculada do lançamento (hash de
conta + data + valor + descrição normalizada). Serve pra detectar reimportação do mesmo
extrato. Importante: fingerprint **não** é índice único, porque duplicata legítima existe
(dois cafés de R$ 5 no mesmo dia). O `external_id` do OFX, quando o banco manda, esse sim
é único por conta. Então: índice único parcial em `(workspace_id, account_id, external_id)`
quando `external_id` não é nulo, e índice comum em `fingerprint` que alimenta uma tela de
"possível duplicata, confirma?".

**`transaction_split`** — `id`, `transaction_id`, `workspace_id`, `amount_cents`, `scope`,
`category_id`, `note`.

Resolve "paguei R$ 300 de luz, R$ 200 é da loja e R$ 100 é de casa". Linhas de split só
existem quando `scope = SPLIT`; o caso comum não paga esse custo.

Os dois lados: *sempre* criar linhas de split (mesmo quando é uma só) deixa todo relatório
com uma forma única, ao preço de dobrar o número de linhas e complicar a entrada rápida;
criar split só quando precisa é mais simples de escrever mas obriga todo relatório a tratar
dois casos. Recomendo o segundo **com uma saída elegante**: uma *view* SQL chamada
`transaction_line` que une os lançamentos sem split e as linhas de split. Todo relatório lê
só a view e vê uma forma única. Melhor dos dois lados por ~15 linhas de SQL.

Regra de arredondamento: ao dividir por porcentagem, usar *maior resto* — distribui os
centavos que sobram para as maiores parcelas, garantindo que a soma dos splits seja
**exatamente** o valor original. E guardar centavos absolutos nos splits, não porcentagem
(a porcentagem é recalculada para exibir). Porcentagem guardada acumula erro.

### 1.4 Categorias

**`category`** — `id`, `workspace_id` (**nulo = categoria de fábrica, global**), `parent_id`,
`name`, `slug`, `kind`, `default_scope`, `derived_from_id`, `is_system`, `sort_order`,
`archived_at`.

`kind` é o campo que faz os relatórios serem corretos: INCOME | EXPENSE | TRANSFER |
OWNER_DRAW | TAX. Transferência não é despesa. E **retirada do dono (`OWNER_DRAW`) não é
despesa do negócio** — é o pró-labore informal. Sem esse `kind`, "quanto sobrou" mistura o
que o dono tirou com o que o negócio gastou, e a pergunta número um do produto fica
respondida errado.

Hierarquia de no máximo 2 níveis (pai e filho). Árvore de profundidade livre exigiria
consultas recursivas e uma UI de árvore; 2 níveis cobrem tudo que um MEI precisa. Limite
deliberado.

Quando o usuário "edita" uma categoria de fábrica, o sistema **copia** para o workspace dele
(`derived_from_id` aponta pra original) em vez de alterar a linha global. Usuário nunca
escreve em linha compartilhada.

### 1.5 Estabelecimentos (merchants)

**`merchant`** — `id`, `workspace_id` (nulo = catálogo global), `canonical_name`,
`display_name`, `default_category_id`, `default_scope`.

**`merchant_pattern`** — `id`, `merchant_id`, `workspace_id` (nulo = global), `pattern_type`
(CONTAINS | PREFIX | REGEX), `pattern`, `weight`, `enabled`.

Por que duas tabelas: um mesmo estabelecimento aparece no extrato de dez formas
("IFD\*IFOOD", "IFOOD CLUB", "IFOOD.COM AGENCIA"). O merchant é a identidade; os patterns são
as máscaras que apontam pra ela. Como são dados e não código, vocês adicionam padrão novo em
produção sem deploy — e isso vai acontecer toda semana nos primeiros meses.

O catálogo global nasce com ~80 entradas que importam no Brasil: iFood, Uber, 99, Shopee,
Mercado Pago, Mercado Livre, PagSeguro/PagBank, Stone, InfinitePay, SumUp, Cielo, Rede,
Nubank, PicPay, Netflix, Spotify, Google, Apple, Correios, Shell/Ipiranga/Petrobras,
Assaí/Atacadão, Farmácias, concessionárias de energia e água do Nordeste, Sicoob, Caixa.
Esse catálogo é ativo do produto: ele melhora para todos os clientes de uma vez.

### 1.6 Regras e aprendizado

**`rule`** — `id`, `workspace_id`, `rule_group_id`, `version`, `name`, `priority`,
`match_mode` (ALL | ANY), `enabled`, `result_category_id`, `result_scope`,
`result_counts_as_revenue`, `created_from` (USER_MANUAL | CORRECTION | SYSTEM_SEED),
`source_transaction_id`, `superseded_by_id`, `archived_at`, `applied_count`,
`overridden_count`, `last_applied_at`, `created_at`, `created_by`.

**`rule_condition`** — `id`, `rule_id`, `field`, `operator`, `value_text`, `value_cents_min`,
`value_cents_max`, `value_int_min`, `value_int_max`, `negate`.

**`rule_split_template`** — `id`, `rule_id`, `percent_bp` (base 10.000), `scope`,
`category_id`. Para "sempre 60% loja / 40% casa".

**`categorization_decision`** (append-only) — `id`, `workspace_id`, `transaction_id`,
`decided_at`, `strategy`, `rule_id`, `merchant_id`, `learned_pattern_id`, `category_id`,
`scope`, `counts_as_revenue`, `confidence`, `engine_version`, `explanation` (jsonb),
`superseded_at`.

**`learned_pattern`** — `id`, `workspace_id`, `kind`, `signature` (jsonb), `category_id`,
`scope`, `support_count`, `purity_bp`, `confidence`, `first_seen_on`, `last_seen_on`,
`muted_at`, `promoted_rule_id`.

**`correction_event`** — `id`, `workspace_id`, `transaction_id`, `decision_id`,
`from_category_id`, `to_category_id`, `from_scope`, `to_scope`, `user_id`, `created_at`,
`suggested_rule_shape` (jsonb), `suggestion_outcome` (ACCEPTED | DECLINED | IGNORED).

**`declined_suggestion`** — `workspace_id`, `shape_signature`, `declined_at`. Para o sistema
não insistir numa sugestão que o usuário já recusou. Parte literal de "nunca errar duas
vezes".

A justificativa detalhada dessas tabelas está na seção 3, que é onde elas ganham sentido.

### 1.7 Importação

**`import_batch`** — `id`, `workspace_id`, `account_id`, `file_name`, `file_hash`, `format`,
`status`, `row_count`, `imported_count`, `duplicate_count`, `error_count`, `period_start`,
`period_end`, `created_by`, `created_at`, `reverted_at`.

`import_batch_id` em cada lançamento + `reverted_at` aqui = **desfazer importação inteira
com um clique**. Isso é barato de construir e é o que permite o usuário experimentar sem
medo. Para um público que tem medo de computador, "dá pra desfazer" vale mais que qualquer
funcionalidade bonita.

**`csv_mapping_profile`** — `id`, `workspace_id`, `name`, `bank_hint`, `column_map` (jsonb),
`date_format`, `decimal_separator`, `encoding`. Cada banco exporta CSV diferente; salvar o
mapeamento evita ensinar de novo todo mês.

### 1.8 Parâmetros legais — a parte que o contexto regulatório exige

**`legal_parameter`** — `id`, `key`, `valid_from` (date), `valid_to` (date, nulo), `value`
(jsonb), `source_note`, `created_at`.

Chaves iniciais: `mei.annual_revenue_limit`, `mei.tolerance_pct`, `mei.das.comercio`,
`mei.das.servicos`, `mei.das.comercio_servicos`, `mei.das.due_day`, `mei.dasn.due_date`,
`mei.proportional_first_year`, `mei.alert_thresholds`.

A decisão de design aqui é **`valid_from`/`valid_to`, não uma tabela de configuração
simples**. Uma tabela chave-valor comum responde "quanto é o DAS?". Essa tabela responde
"quanto era o DAS em março de 2025?" — e essa é a pergunta que o produto realmente faz,
porque relatório de mês passado não pode ser reescrito quando a lei muda. Os valores de 2025
e de 2026 coexistem; cada cálculo pede o parâmetro vigente *na data do fato*.

Valores de partida (confirmem no Portal do Simples Nacional antes de semear; as fontes de
2026 divergem entre si no centavo):

- limite anual: R$ 81.000 (inalterado desde 2018)
- tolerância: 20% → até R$ 97.200 o MEI segue no regime pagando DAS complementar
- DAS 2026: ~R$ 82,05 comércio/indústria · ~R$ 86,05 serviços · ~R$ 87,05 ambos
- vencimento do DAS: dia 20 do mês seguinte
- DASN-SIMEI: 31 de maio
- primeiro ano: limite proporcional = 81.000 ÷ 12 × meses de atividade

Carregamento: um arquivo YAML versionado no repositório, aplicado por migração Flyway. Assim
o número está no git (auditável, revisável em PR) **e** no banco (alterável em produção sem
deploy, via uma tela de admin simples na semana 5 ou via SQL manual antes disso). Os dois
requisitos do enunciado atendidos sem conflito.

Reforço de escopo: o produto **lê** o valor do DAS de uma tabela e lembra do vencimento. Ele
não calcula imposto, não gera guia, não transmite nada. Essa é a fronteira e ela é visível
no modelo de dados — não existe nenhuma tabela aqui capaz de calcular tributo.

**`workspace_setting`** — `workspace_id`, `key`, `value` (jsonb). Para o que é preferência do
cliente, não lei: dias até auto-aplicar pendência (padrão 7), limiar de confiança (padrão
70), canais de notificação.

### 1.9 MEI: limite, obrigações e fechamento

**`revenue_year_snapshot`** — `workspace_id`, `year`, `revenue_cents`, `computed_at`,
`last_alert_level`. Faturamento é derivável dos lançamentos, mas o painel consulta isso a
cada carga e o alerta precisa saber qual nível já foi disparado para não repetir. Recalculado
por job noturno e a cada alteração relevante. É cache, não verdade — reconstruível do zero.

**`obligation`** — `id`, `workspace_id`, `kind` (DAS | DASN), `period`, `due_date`,
`expected_amount_cents`, `status` (OPEN | PAID | SKIPPED), `paid_transaction_id`,
`created_at`. Gerado a partir de `legal_parameter` + `activity_type`. É lista de tarefas,
não apuração fiscal.

**`monthly_closing`** — `workspace_id`, `year_month`, `inflow_cents`, `outflow_cents`,
`business_net_cents`, `owner_draw_cents`, `closed_at`, `closed_by`, `reopened_at`, `notes`.
Fechar o mês congela os números. Se um lançamento atrasado chegar depois, o mês é marcado
como reaberto em vez de mudar silenciosamente. Isso é o que permite o cliente confiar no
número que ele viu mês passado.

**`notification`** — `id`, `workspace_id`, `user_id`, `kind`, `payload` (jsonb),
`scheduled_for`, `sent_at`, `read_at`, `channel`. Fila de saída para digesto de pendências,
lembrete de DAS e alerta de limite.

### 1.10 Índices que precisam existir desde o início

*Índice* = sumário do livro: sem ele o banco lê todas as páginas para achar uma linha.

- `transaction (workspace_id, occurred_on DESC)` — a tela principal
- `transaction (workspace_id, status)` parcial onde `status = 'PENDING_REVIEW'` — a fila
- `transaction (workspace_id, account_id, occurred_on)` — extrato por conta
- `transaction (workspace_id, category_id, occurred_on)` — relatórios
- único parcial `transaction (workspace_id, account_id, external_id)` onde não nulo — dedupe
- `transaction (workspace_id, fingerprint)` — possível duplicata
- `categorization_decision (transaction_id, decided_at DESC)` — painel "por quê"
- `rule (workspace_id, enabled, priority)` — carga do contexto do motor

---

## 2. Arquitetura do backend

**Monolito modular.** Um único processo, um único banco, um único deploy — organizado em
módulos com fronteiras explícitas. Sem CQRS, sem event sourcing, sem microsserviço, sem
Kafka, sem Redis. Para 93 clientes e ~350 mil lançamentos/ano no total, um monolito numa VPS
de 4 GB fica entediado.

### Módulos (pacote por funcionalidade, não por camada)

```
br.com.zimbro
├── platform/       config, segurança, contexto de tenant, Money, Clock, erros, migrações
├── identity/       usuários, workspaces, membros, autenticação
├── accounts/       contas e saldos
├── ledger/         lançamentos, splits, transferências, dedupe — a API de escrita
├── catalog/        categorias, merchants, normalização de descrição
├── rules/          ◀ O MOTOR: regras, pipeline, padrões aprendidos, log de decisão
├── imports/        parser OFX, parser CSV, ciclo de vida do lote, desfazer
├── mei/            faturamento vs limite, obrigações/DAS, pró-labore, fechamento
├── reporting/      leituras: fechamento do mês, painel, "quanto sobrou"
├── notifications/  fila de saída, jobs agendados, e-mail
└── billing/        estado da assinatura (manual no MVP)
```

Por que pacote por funcionalidade: a organização clássica (`controllers/`, `services/`,
`repositories/`) faz com que qualquer mudança real toque três pastas distantes, e para dev
iniciante isso significa perder o fio. Agrupado por assunto, mexer em importação é mexer numa
pasta.

### Camadas dentro de cada módulo

`web` (controller + DTOs de entrada/saída) → `app` (serviço de aplicação = um caso de uso,
e é aqui que a transação de banco começa e termina) → `domain` (entidades e lógica) →
`infra` (repositórios, adaptadores).

Sobre pureza de domínio, os dois lados: domínio puro (sem anotação de framework) é
perfeitamente testável mas obriga a escrever e manter código de conversão para as entidades
JPA — fácil 30% mais código. Entidade JPA servindo de domínio é pragmático e rápido, mas
acopla regra de negócio ao banco.

Recomendo **misto, com a linha no lugar certo**: entidades JPA fazem papel de domínio em
todos os módulos, **exceto no `rules`**, onde o motor é escrito como código puro, sem Spring
e sem JPA. O motor precisa de centenas de testes rodando em milissegundos e precisa ser
reexecutável em lote sem tocar o banco — ali a pureza se paga. No resto, não se paga em 5
semanas.

### Fronteiras entre módulos (a regra que impede o monolito de apodrecer)

- Cada módulo expõe um pacote `api` com interfaces e DTOs. Todo o resto é interno.
- Módulo **nunca** importa entidade JPA de outro módulo. Referência cruzada é por ID.
- Chamada entre módulos só através do pacote `api` do outro.
- Fiscalização automática com **ArchUnit** — uma biblioteca de teste que verifica regras de
  arquitetura e quebra o build quando alguém cruza fronteira. Custa ~50 linhas e para dois
  iniciantes vale mais que qualquer revisão de código, porque corrige na hora e sem
  constrangimento.
- Uma única pasta de migrações Flyway, um único schema. Separar schema por módulo é
  complexidade sem retorno aqui.

### Onde vive o motor de regras e como ele é chamado

O motor mora em `rules` e é consumido via uma interface declarada lá (`CategorizationPort`).
Dois caminhos de chamada:

1. **Lançamento manual** → chamada síncrona, dentro da mesma transação. O usuário digita o
   valor e vê a categoria sugerida na hora. Latência percebida é a funcionalidade.
2. **Importação em lote** → o `imports` carrega o contexto do workspace **uma vez** (todas as
   regras, patterns de merchant, padrões aprendidos) e chama o motor N vezes com esse
   contexto em memória. 500 lançamentos = 1 consulta de contexto, não 500.

A forma do motor é uma função pura:

```
decide(TransactionFacts, RuleContext) -> Decision
```

Nenhum acesso a banco dentro dela. Isso é o que torna possível testar com arquivo de casos,
reexecutar sobre o histórico e depurar sem subir nada.

### Transversais

**Isolamento de tenant.** Os dois lados: filtro automático do Hibernate (`@TenantId`) nunca
esquece, mas é mágica invisível — quando falha, o iniciante não tem onde olhar; `workspaceId`
como primeiro parâmetro explícito de todo método de repositório é visível e óbvio, mas pode
ser esquecido.

Recomendo **explícito + rede de proteção**: `workspaceId` explícito em todo método, mais (a)
um teste ArchUnit que falha se um método de repositório de entidade com tenant não receber
`workspaceId`, e (b) um teste de integração que cria dois workspaces com dados e afirma que
**todo** endpoint de listagem retorna zero linhas do vizinho. Esse teste é o mais importante
do projeto inteiro. Endurecimento futuro (não no MVP): *Row Level Security* do Postgres —
o banco em si recusando linha de outro tenant. Vale fazer quando houver clientes pagantes.

**Dinheiro:** um value object `Money` embrulhando `long` centavos, com soma/subtração/rateio.
Proibido `BigDecimal` espalhado e proibido `double` em qualquer lugar.

**Erros:** `problem+json` (RFC 7807) com um único `@ControllerAdvice`. Mensagem em português,
código estável para o frontend.

**Jobs agendados:** `@Scheduled` do Spring. Uma instância só, então sem necessidade de lock
distribuído. A proteção de verdade é **todo job ser idempotente** — rodar duas vezes produz o
mesmo resultado que rodar uma. Jobs: minerador de padrões (noturno), auto-aplicação de
pendências vencidas (diário), geração de obrigações (mensal), snapshot de faturamento
(noturno), digesto de notificação (diário).

**Testes, com o rigor distribuído de propósito:**
- `rules`: cobertura quase total, testes unitários puros + suíte de casos em arquivo.
- repositórios e importação: Testcontainers (Postgres de verdade num container durante o
  teste). Banco em memória mente sobre SQL.
- controllers: MockMvc, só caminho feliz e um de erro.
- resto: fumaça.

Com 150 horas, cobrir tudo igualmente é impossível. A escolha é cobrir fortemente o que é o
produto e levemente o que é encanamento.

### Frontend (o suficiente para o plano fechar)

Next.js App Router como PWA instalável. Autenticação por **cookie httpOnly**, com os
*route handlers* do Next funcionando como proxy fino para o Spring — assim o token nunca
fica em `localStorage`, onde qualquer XSS o rouba. Service worker cacheia o shell do app para
abrir offline; **escrita offline completa fica fora do MVP** (fila em IndexedDB com
reconciliação é uma semana inteira sozinha) — no MVP, só reenvio automático do formulário de
lançamento rápido quando a conexão volta.

Telas do MVP, em ordem de importância: lançamento rápido (uma tela, 3 toques), lista do mês,
fechamento do mês, fila de revisão, regras, contas, importação, onboarding.

---

## 3. Motor de regras — desenho detalhado

Esta é a parte que define o produto. Se tudo mais for medíocre e esta parte for excelente, o
produto vende.

### 3.1 Como a regra é representada

Uma regra é **dados, não código e não texto**:

```
regra := { prioridade, modo (TODAS|QUALQUER), condições[], resultado }
condição := { campo, operador, valor(es), negado? }
resultado := { categoria, escopo, conta_como_faturamento, split[]? }
```

Por que condições como linhas em tabela, e não uma expressão em texto ("desc contains X and
amount > 50"):

| Abordagem | A favor | Contra |
|---|---|---|
| DSL em texto | compacta, expressiva, fácil de guardar | precisa escrever parser; erro de sintaxe do usuário; avaliar texto de usuário é risco de segurança; impossível de construir UI decente em cima |
| Condições em linhas | UI monta-se sozinha a partir do schema; consultável em SQL ("quais regras usam esta categoria?"); sem parser; sem avaliação de texto arbitrário | mais linhas no banco; expressividade limitada |

Recomendo linhas em tabela. A expressividade limitada é uma *vantagem* aqui: um MEI não vai
escrever expressão booleana, e cada ponto de expressividade extra é uma forma nova de o
usuário criar uma regra que ele não entende.

**Campos e operadores disponíveis (matriz pequena de propósito):**

| Campo | Operadores |
|---|---|
| DESCRIPTION | contém, não contém, começa com, igual a |
| MERCHANT | é um de |
| AMOUNT | igual, igual ±tolerância%, entre, ≥, ≤ |
| DIRECTION | é |
| ACCOUNT | é um de |
| ACCOUNT_OWNERSHIP | é |
| DAY_OF_MONTH | entre |
| WEEKDAY | é um de |

**Regex não é exposto ao usuário no MVP.** Dois motivos: ninguém do público alvo escreve
regex, e regex de usuário abre porta para expressão que travа o servidor (*ReDoS* — uma
expressão que leva tempo exponencial para avaliar). Regex existe apenas nos
`merchant_pattern` internos e curados, compilado com limite de tamanho e timeout.

**Aninhamento não existe.** `match_mode` aplica TODAS ou QUALQUER no mesmo nível, sem
parênteses. Quem precisa de "(A ou B) e C" cria duas regras. Limite deliberado: aninhamento
exige UI de árvore, que é caro de construir e difícil de entender.

### 3.2 Normalização — o passo zero

Antes de qualquer resolução, a descrição crua passa por um normalizador determinístico:

1. maiúsculas, acentos removidos, pontuação colapsada
2. remoção de prefixos de adquirente e meio de pagamento: `IFD*`, `PAG*`, `MP*`, `PICPAY*`,
   `C/C`, `DEB VISA ELECTRON`, `COMPRA CARTAO`, `PIX ENVIADO`, `TRANSF`
3. remoção de datas embutidas, números de loja, fragmentos de CNPJ/CPF, sufixos de parcela
   ("PARC 03/12" → guardado como fato separado, não jogado fora)
4. colapso de espaços
5. saída: `normalizedDescription` + lista de `tokens` + eventuais fatos extraídos (parcela)

A descrição crua **nunca** é destruída. Ela é a prova — e quando o motor erra, é o que vocês
vão olhar.

Os `TransactionFacts` que chegam ao motor: descrição crua, descrição normalizada, tokens,
valor em centavos, direção, conta, ownership da conta, data, dia do mês, dia da semana,
merchant resolvido (se houver), origem da importação.

### 3.3 O pipeline de resolução

Cinco estágios em cadeia, **ordenados por confiabilidade da fonte, não por acurácia
estatística**. Cada estágio devolve uma decisão com confiança, ou nada. O primeiro que
devolve acima do próprio limiar ganha.

**Estágio 1 — Regra explícita do usuário. Confiança 100.**
Avalia todas as regras habilitadas do workspace. Ordena por `priority` crescente, empate
desfeito por `created_at` mais recente (regra nova reflete intenção nova). Se mais de uma
bate, a vencedora é aplicada e as perdedoras ficam registradas em
`explanation.alsoMatched` — a tela "por quê" então mostra o conflito e oferece reordenar.
Detectar conflito e expor é mais útil que tentar resolvê-lo com esperteza.

**Estágio 2 — Merchant identificado. Confiança 75–95.**
Resolve o estabelecimento pelos `merchant_pattern` (padrão mais específico ganha — maior
comprimento do padrão, `weight` desempata), e usa a categoria padrão do merchant.
Confiança: 95 se o próprio usuário já confirmou esse merchant→categoria 2+ vezes, 90 se é
casamento exato do catálogo global, 75 se é casamento parcial.
Detalhe importante: o `merchant_id` é gravado no lançamento **independentemente de qual
estágio ganha a categoria**. Saber que é iFood tem valor próprio (relatórios, regras
futuras), mesmo quando a categoria veio de uma regra do usuário.

**Estágio 3 — Padrão aprendido. Confiança 60–85 (teto rígido em 85).**
Consulta `learned_pattern`. Detalhado em 3.5. O teto de 85 é estrutural: **um padrão
aprendido jamais pode ganhar de uma regra do usuário**, nem por acidente de pontuação.

**Estágio 4 — Fallback genérico por tipo de operação. Confiança 30–50.**
Usa só sinais fortes e grosseiros:
- transferência detectada entre duas contas do próprio workspace (mesmo valor, direções
  opostas, até 3 dias de diferença) → categoria TRANSFER, `counts_as_revenue = false`,
  confiança 85 (exceção justificada: a evidência aqui é aritmética, não heurística, e
  classificar transferência como receita é o erro mais caro do produto)
- entrada via Pix em conta de negócio → "Receita de vendas", escopo BUSINESS,
  `counts_as_revenue = true`, confiança 45
- saída em cartão de crédito sem outro sinal → "Despesa a identificar", confiança 30
- saída em conta pessoal sem outro sinal → escopo PERSONAL, confiança 35

**Estágio 5 — Não categorizado. Confiança 0.**
Sempre devolve algo: categoria "A classificar", `status = PENDING_REVIEW`, e **até 3
sugestões** — os melhores candidatos dos estágios 2 a 4 mesmo abaixo do limiar. Assim a tela
de revisão é um toque, não um formulário. Fila de revisão com formulário em branco é fila
que o usuário abandona.

### 3.4 O que toda decisão carrega — a garantia anti-caixa-preta

Cada passagem pelo motor grava uma linha em `categorization_decision` com: estratégia, o ID
exato da regra/merchant/padrão que decidiu, categoria, escopo, confiança, `engine_version`, e
um `explanation` em JSON contendo **a frase em português** que explica a decisão e quais
condições bateram.

Exemplos do que a tela mostra, literalmente:
- "Sua regra **'Posto → Combustível'** foi aplicada porque a descrição contém *POSTO
  IPIRANGA*."
- "Reconheci **iFood** no texto *IFD\*IFOOD CLUB* e iFood costuma ser **Alimentação**."
- "Todo mês, perto do dia 5, sai **R$ 1.200,00** da mesma conta. Isso parece **Aluguel** —
  vi esse padrão **4 vezes**."
- "Não reconheci. Pelo tipo de operação chutei **Despesa a identificar**."

Três propriedades que essa estrutura garante:

**Determinismo.** Mesmos fatos + mesmo contexto + mesma `engine_version` = mesma decisão.
O motor não lê relógio (o `Clock` é injetado), não sorteia nada, não consulta serviço externo.
Bug reproduzível é bug corrigível.

**Rastreabilidade permanente.** Como regra é versionada e decisão aponta para a *versão*
exata, um lançamento de março continua explicando-se com a regra de março mesmo depois que o
usuário editou a regra em agosto. Os dois lados: versionar gera mais linhas e um fluxo de
edição um pouco mais complicado; `UPDATE` simples é fácil mas reescreve a história em
silêncio. Num produto cujo argumento de venda é confiança, recomendo versionar. Custo real:
uma coluna `rule_group_id` e um `superseded_by_id`. No MVP não há UI de diff, só uma lista
"histórico desta regra".

**Soberania do usuário.** Lançamento com `decided_by = USER` fica **congelado** contra
redecisão automática. Reprocessamento em massa nunca passa por cima de escolha humana. A
única forma de mudar é o usuário aceitar explicitamente um "aplicar esta regra a 12
lançamentos anteriores".

### 3.5 Aprendizado por correção — o laço central

É aqui que mora a promessa "nunca erra duas vezes a mesma coisa". O laço tem seis partes.

**(1) A correção é registrada.** Usuário troca categoria ou escopo → grava
`correction_event`, grava nova `categorization_decision` com `strategy = USER_MANUAL` e
confiança 100, marca a decisão anterior como superseded. Nada é apagado.

**(2) O sistema propõe a regra na hora — com a menor generalização que resolve.**
Esse é o ponto mais delicado do design. Proposta genérica demais estraga outras
categorizações; específica demais não serve na próxima vez. O algoritmo de generalização:

- Se o merchant foi resolvido → propõe `merchant = X → categoria`. É a forma mais limpa e a
  mais robusta a variação de texto.
- Senão, escolhe o **token ou bigrama mais distintivo** da descrição normalizada.
  "Distintivo" = frequente nos lançamentos *daquela* categoria no workspace e raro nas
  outras. É um escore tipo TF-IDF (*term frequency–inverse document frequency*: mede se uma
  palavra é característica de um grupo ou é só comum em tudo), calculado com **uma consulta
  SQL de agregação** sobre os lançamentos do próprio workspace. Sem biblioteca de machine
  learning, sem modelo treinado, sem serviço externo. Isso mantém o custo em zero e o
  comportamento explicável.
- Só adiciona condição de valor ou de dia **se a evidência histórica exigir** — isto é, se o
  mesmo descritor já apareceu com duas categorias diferentes distinguidas por valor ou por
  data. Condição desnecessária é o que faz a regra parar de funcionar no mês seguinte.

**(3) A proposta é mostrada como frase, com prévia do impacto.**

> Criar regra: descrição contém **"POSTO IPIRANGA"** → **Combustível (Negócio)**
> Isso também reclassificaria **7 lançamentos anteriores**. [Ver quais]
> `[Criar regra]` `[Criar e aplicar ao passado]` `[Só este lançamento]`

A prévia de impacto é a funcionalidade de maior retorno sobre esforço de todo o motor. Ela
transforma "o sistema mexeu nas minhas coisas" em "eu autorizei isso". Mesma prévia aparece
quando o usuário cria regra manualmente: ao salvar, a regra é avaliada contra os últimos N
lançamentos e o sistema mostra quantos mudariam e se há conflito com regra existente.

**(4) Recusa também é aprendizado.** `suggestion_outcome = DECLINED` + a assinatura da forma
recusada em `declined_suggestion`. O sistema não volta a propor aquela forma. Insistir numa
sugestão recusada é exatamente "errar duas vezes a mesma coisa", só na camada da UX.

**(5) Padrões aprendidos são minerados fora do caminho crítico.** Um job noturno (e sob
demanda, após X correções) roda três mineradores sobre o histórico:

- `MERCHANT_AMOUNT`: mesmo merchant + valor dentro de ±5%, visto ≥3 vezes com a mesma
  categoria confirmada pelo usuário.
- `RECURRING_AMOUNT_DAY`: mesmo valor ±2% + mesmo dia do mês ±3 dias, em ≥3 meses
  consecutivos → recorrência. Guarda a periodicidade detectada. É isso que pega aluguel,
  assinatura e mensalidade — exatamente os exemplos do enunciado.
- `DESCRIPTION_TOKEN`: token ou bigrama → categoria, com ≥3 ocorrências e **pureza ≥80%**
  (pureza = fração das ocorrências que concordam na categoria).

Cada padrão guarda `support_count`, `purity_bp` e `last_seen_on`. Confiança = função
crescente de suporte e pureza, **limitada a 85**. Padrão cuja pureza cai abaixo do limiar é
silenciado automaticamente.

Por que minerar offline em vez de aprender a cada lançamento, os dois lados: aprendizado
online reage na hora, mas torna o caminho crítico não determinístico e muito difícil de
depurar — e para dois devs iniciantes isso é uma fonte inesgotável de bug que não reproduz.
Mineração offline mantém o caminho crítico puro e os padrões reconstruíveis do zero (são
cache, não verdade). O preço — uma correção de hoje só vira padrão aprendido amanhã — é
irrelevante, porque a correção já teve efeito **imediato** pelo mecanismo mais forte: a regra
proposta no passo 2. Recomendo offline.

**(6) Promoção: o conhecimento implícito migra para regra visível.** Quando um padrão
aprendido atinge suporte ≥5 e pureza 100%, a UI oferece transformá-lo em regra explícita:

> Notei que todo dia 5 sai **R$ 1.200,00** da conta Itaú. É **Aluguel**?
> `[Sim, criar regra]` `[Não é]` `[Ignorar]`

Essa é a resposta estrutural para "sem virar caixa-preta": o sistema é projetado para
**esvaziar sua própria caixa preta ao longo do tempo**. O que ele descobre sozinho vira, com
um toque do usuário, uma regra que o usuário vê, entende, edita e desliga. Quanto mais o
cliente usa, maior a fração das decisões vinda do estágio 1 (100% explícito) e menor a
vinda do estágio 3. Essa proporção é, por si só, uma métrica de maturidade da conta.

### 3.6 A métrica que mede o motor

Duas consultas SQL numa página de admin interna, construídas na semana 3:

1. **Taxa de correção** = correções ÷ lançamentos categorizados automaticamente, por semana.
2. **Taxa de correção repetida** = fração das correções cujo descritor normalizado **já havia
   sido corrigido antes naquele workspace**.

A segunda é a estrela do norte. Ela é a medida direta da promessa do produto e **tem que
tender a zero**. Taxa de correção na faixa de 10–20% é saudável num produto desses; taxa de
correção *repetida* acima de 2% significa que o laço de aprendizado está furado, e é
prioridade sobre qualquer funcionalidade nova.

### 3.7 Pendências nunca acumulam — a regra dos 7 dias

Lançamento com confiança abaixo do limiar (padrão 70) entra como `PENDING_REVIEW` com
`review_due_on = data de entrada + 7 dias` (prazo configurável em `workspace_setting`).

Job diário, para cada pendência vencida:
1. aplica o melhor palpite (categoria/escopo da sugestão nº 1)
2. `status = CONFIRMED`, `decided_by = SYSTEM_AUTO`
3. **mantém a confiança real baixa** — o sistema não finge certeza que não tem
4. grava decisão com `strategy = AUTO_APPLIED_GUESS`
5. enfileira **um digesto por dia por workspace**, nunca uma notificação por lançamento

Reversibilidade, que o enunciado exige: o lançamento aparece na lista com selo
"**Aplicado automaticamente**" e um toque de "não era isso" que reabre. Existe uma tela
"revisar automáticos dos últimos 30 dias". Como decisão é append-only, reverter é só gravar
uma decisão nova — nenhum dado se perde e o histórico mostra o que o sistema chutou e o que o
humano corrigiu.

Duas salvaguardas que eu adiciono:

**Digesto, não notificação individual.** Fadiga de notificação mata retenção, e no público
alvo mata na primeira semana. Um e-mail/push por dia, no máximo: "classifiquei 6 lançamentos
pra você, confira".

**Decisão que afeta o limite anual exige humano.** A auto-aplicação **nunca** marca
`counts_as_revenue = true` por conta própria quando isso cruzaria um limiar de alerta de
faturamento. Esses casos permanecem pendentes e escalam o aviso em vez de resolver sozinhos.
Justificativa: um chute errado sobre categoria de despesa custa ao cliente um ajuste de 5
segundos; um chute errado sobre faturamento pode levá-lo a achar que estourou (ou que não
estourou) o limite legal. Riscos assimétricos pedem tratamentos assimétricos.

### 3.8 Reprocessamento

Um comando "reclassificar" pode rodar o motor sobre uma seleção (um mês, uma conta, tudo).
Regras: nunca sobrescreve `decided_by = USER`; sempre mostra prévia de quantos mudariam
antes de executar; roda em lote com contexto carregado uma vez; grava decisões novas (não
altera as antigas). Serve para três coisas: aplicar regra nova ao passado, recuperar de bug
do motor, e reprocessar após mineração de padrões.

---

## 4. Fatiamento em 5 semanas

### Aviso sobre capacidade, antes do plano

30h/semana somadas × 5 semanas = **150 pessoa-horas**. Dois devs iniciantes gastam parte
disso aprendendo a ferramenta, não produzindo. Em 150 horas não cabe backend + PWA +
importação OFX + motor de regras + venda, no padrão de qualidade que um produto pago exige.

Portanto: **a ordem de corte está decidida antecipadamente**, e é para ser obedecida sem
reunião quando a semana apertar:

1. primeiro corta **CSV** (fica para pós-MVP)
2. depois corta **importação OFX** inteira
3. depois corta **integração de pagamento** (já está cortada)
4. **nunca** corta o motor de regras nem o fechamento mensal — é o produto

Importação de CSV sai do MVP por decisão técnica: cada banco brasileiro exporta um CSV
diferente, com codificação, separador decimal e formato de data próprios. É um poço sem fundo
para iniciante. OFX é padrão (há biblioteca pronta em Java, `ofx4j`) e cobre os bancos que
importam. A estratégia de entrada de dados continua sendo "manual rápido + OFX/CSV"; o CSV
só chega na semana 7 ou 8, depois de vocês terem na mão os arquivos reais dos cinco primeiros
clientes.

### Semana 0 (os primeiros 2 dias, dentro da semana 1)

Repositório, `docker compose` local funcionando nas duas máquinas, Flyway com migração
inicial, GitHub Actions rodando build + teste, VPS provisionada com Caddy e HTTPS, deploy de
um "olá" ponta a ponta.

**Pronto quando:** existe uma URL pública em HTTPS onde uma página do Next mostra a hora
vinda do Postgres através do Spring; e `docker compose up` funciona do zero no notebook dos
dois.

### Semana 1 — Fundação, cadastro e lançamento manual

Autenticação (cadastro, login, logout, recuperação de senha por e-mail), criação do
workspace no cadastro, CRUD mínimo de contas (nome, tipo, ownership, saldo inicial),
**lançamento rápido** (valor, direção, data, descrição, conta, escopo pessoal/negócio,
seletor de categoria com catálogo semeado), lista do mês com filtro, editar e excluir,
PWA instalável com ícone.

**Pronto quando:** uma pessoa real, no celular dela, se cadastra sozinha, instala o app na
tela inicial e registra 10 lançamentos em menos de 3 minutos, vendo o total do mês. Medido
com cronômetro, não com opinião.

### Semana 2 — Fechamento mensal e separação pessoal/negócio

Tela do mês com "**entrou / saiu / sobrou / quanto é meu**", divisão de um lançamento entre
pessoal e negócio, pró-labore informal como `kind = OWNER_DRAW` com linha própria no
fechamento, faturamento acumulado contra o limite anual com alerta progressivo lendo
`legal_parameter`, cartão de lembrete do DAS a partir da mesma tabela, `monthly_closing`.
Relatórios por *view* SQL, não por montagem em Java.

**Pronto quando:** num workspace de teste com 60 lançamentos variados (incluindo
transferência, split e retirada do dono), os números da tela do mês batem com uma planilha
feita à mão; e trocando a linha de `legal_parameter` direto no banco, o limite e o nível de
alerta mudam na tela **sem redeploy**.

### Semana 3 — Motor de regras v1 (a semana mais importante)

CRUD de regras com condições de descrição/valor/direção/conta, prioridade padrão por
especificidade com reordenação manual, pipeline de 5 estágios, catálogo semeado de ~80
merchants com seus patterns, log de decisão, painel "**por que esta categoria?**", fluxo de
correção com proposta de regra, prévia de impacto no passado, aplicar ao passado, página
interna com as duas métricas de 3.6.

**Pronto quando:**
1. Existe uma suíte de ~120 descritores reais (colhidos de extratos de vocês e dos primeiros
   prospects) com categoria esperada, rodando como teste automatizado, e ela passa.
2. **Corrigir um descritor duas vezes é impossível**: num teste ponta a ponta, corrigir um
   lançamento, aceitar a regra proposta, importar/criar outro lançamento com o mesmo
   descritor, e ele já chega categorizado certo. Esse teste é a promessa do produto escrita
   em código.
3. Todo lançamento da tela mostra uma frase de explicação legível.

Comecem a colher descritores reais **na semana 1** — pedir o OFX ou um print do extrato aos
primeiros cinco prospects. Sem esse material, a semana 3 vira adivinhação.

### Semana 4 — Importação OFX e pendências

Upload de OFX, parsing com `ofx4j`, vínculo com conta, dedupe por `external_id` +
`fingerprint` com tela de possível duplicata, **prévia do lote antes de confirmar**, desfazer
lote, fila de revisão de baixa confiança, minerador noturno de padrões aprendidos, job dos 7
dias com digesto por e-mail, emparelhamento de transferência entre contas próprias.

**Pronto quando:** importar o mesmo arquivo OFX real duas vezes cria **zero** duplicatas;
um extrato de 200 lançamentos vai a zero pendências em menos de 10 minutos de trabalho
humano; e um teste que avança o relógio prova que o job dos 7 dias aplica o palpite, notifica
e que o "não era isso" reverte.

### Semana 5 — Vender

Onboarding de 3 telas (que negócio você tem → quais contas → primeiro lançamento), polimento
do PWA e prompt de instalação, **trava de assinatura manual** (`paid_until` no workspace,
que vocês atualizam à mão depois do Pix), Sentry e UptimeRobot ligados, **ensaio real de
restauração de backup**, básicos de LGPD (política de privacidade, exportar meus dados,
excluir minha conta), landing page de uma página, e um roteiro de demonstração de 10 minutos
para a venda presencial.

Cobrança manual via Pix em vez de integração: justificativa em 4.1 abaixo.

**Pronto quando:** vocês instalam o app no celular de **3 MEIs reais** em João Pessoa, cada
um completa o onboarding sozinho em menos de 5 minutos e registra um lançamento no dia
seguinte **sem ajuda** — e vocês conseguem restaurar o banco do backup numa máquina limpa.

**Reservem ~40% da semana 5 para estouro das semanas anteriores.** Não é pessimismo, é como
funciona.

### 4.1 Por que cobrança manual no MVP

Os dois lados: integração com Asaas ou Mercado Pago dá cobrança recorrente automática,
inadimplência tratada e zero trabalho por cliente — ao custo de cadastro, homologação,
webhook, tratamento de estado de assinatura e testes, facilmente 25–30 horas, ou seja 20% do
MVP inteiro. Pix manual + uma coluna `paid_until` custa 2 horas e 10 minutos de trabalho
humano por mês por cliente.

Com venda presencial e meta de 93 assinantes em 12 meses, vocês vão passar meses abaixo de 30
clientes. 30 clientes × 10 min = 5 horas/mês de trabalho manual — mais barato que as 30 horas
de desenvolvimento, e com a vantagem de cada cobrança ser um contato com o cliente, que é
exatamente o que vocês precisam nos primeiros meses. Recomendo manual, com a integração
entrando quando passar de ~40 assinantes.

---

## 5. O que fica de fora, e por quê

**Fiscal e contábil**
- Emissão de NF-e/NFS-e — fora de escopo declarado; exige certificado digital e integração
  municipal, meses de trabalho
- Leitura de NFC-e / QR code de nota — fora de escopo declarado
- Cálculo de imposto, geração de guia do DAS, transmissão de DASN — o produto **lembra** do
  DAS lendo um valor configurado; não apura nada
- Exportação para contador (SPED, balancete) — ninguém vai pedir antes do cliente nº 30

**Integrações**
- Open Finance / sincronia bancária — inviabilizada pelo custo, decisão tomada
- **Bot de WhatsApp** — este é o que mais vai tentar vocês, porque todo cliente vai pedir.
  A API oficial da Meta exige aprovação, tem custo por conversa e janela de sessão de 24h;
  gateways não oficiais são baníveis. São semanas de trabalho. O atalho do PWA na tela
  inicial do celular resolve 90% do caso de uso por 0% do custo. Fica para depois dos 93
  assinantes
- OCR de recibo / foto de nota — alto custo, alta taxa de erro, não ataca nenhuma das três
  dores
- App nativo em loja — decisão tomada, PWA

**Produto**
- Convite de usuário, perfil de contador, múltiplas empresas por conta — tabela já prevê,
  tela não entra
- Fatura e ciclo de fechamento de cartão de crédito — cartão é só mais uma conta no MVP.
  Ciclo de fatura (compra em maio que vence em junho, parcelamento) é um subsistema inteiro
- Contas a pagar/receber, parcelamentos futuros, projeção de fluxo de caixa — o MVP olha para
  o que já aconteceu
- Orçamento e meta por categoria — funcionalidade bonita que não responde nenhuma das três
  dores
- Relatórios customizáveis e gráficos elaborados — **um** gráfico de barras no fechamento, e
  acabou
- Conciliação bancária completa (bater extrato contra lançamentos linha a linha)

**Motor**
- Regex para o usuário, condições aninhadas, regras compartilhadas entre workspaces,
  sugestão por modelo de linguagem — todos adicionam poder e tiram previsibilidade

**Técnico**
- Microsserviços, Kafka, Redis, cache distribuído, Kubernetes — o monolito na VPS atende
  93 clientes sem suar
- Row Level Security — endurecimento valioso, mas depois dos primeiros pagantes
- Escrita offline completa com fila e reconciliação — uma semana inteira sozinha
- i18n, multi-moeda, tema escuro, acessibilidade além do básico
- Qualquer refatoração durante as 5 semanas

---

## 6. Riscos técnicos

### Risco 1 — 150 pessoa-horas não cabem no escopo (probabilidade: alta)

É o risco número um e não é técnico, é aritmético. Mitigação: a ordem de corte da seção 4 já
está decidida, para não precisar de reunião no momento de pressão; demo funcionando como
portão rígido no fim de cada semana (se a semana não entrega demo, a semana seguinte começa
cortando, não acumulando); zero refatoração durante o MVP; *pair programming* **só** na
semana 3 — o motor é a parte onde a cabeça dos dois junta vale mais que o paralelismo. Nas
outras semanas, trabalho dividido por módulo.

### Risco 2 — Categorização ruim no mundo real (probabilidade: alta, impacto: existencial)

Descritor de extrato brasileiro é lixo textual. O risco não é o motor ser imperfeito — é
vocês descobrirem o quão imperfeito só na frente do cliente. Mitigação: colher **300+
descritores reais antes da semana 3** (pedir OFX aos primeiros prospects já na semana 1, e
usar o extrato de vocês mesmos); suíte de casos em arquivo como teste automatizado; e, acima
de tudo, o laço de correção da seção 3.5, que faz a acurácia baixa **degradar com
elegância**: o cliente corrige uma vez, o sistema nunca erra de novo, e a experiência é de
produto que aprende em vez de produto quebrado. Medir a taxa de correção desde o primeiro
cliente.

### Risco 3 — Diversidade de arquivos dos bancos brasileiros (probabilidade: alta)

OFX 1.x é SGML, OFX 2.x é XML; Nubank, Sicoob, Caixa, Banco do Brasil, Mercado Pago, PicPay,
InfinitePay e Stone divergem em codificação (latin-1 vs UTF-8), em `FITID` (alguns repetem,
alguns omitem), em como representam Pix. Mitigação: `ofx4j` em vez de parser próprio; uma
pasta de *fixtures* com um arquivo real por instituição, cada um virando caso de teste;
suportar **somente** os bancos dos cinco primeiros clientes e exibir, para os demais, um
"ainda não suportamos este banco — nos manda o arquivo" que é ao mesmo tempo mensagem honesta
e canal de coleta.

### Risco 4 — Vazamento entre tenants (probabilidade: média, impacto: fim do produto)

Um cliente ver o extrato de outro acaba com um produto financeiro vendido boca a boca numa
cidade onde todos se conhecem. Mitigação em três camadas: `workspaceId` explícito em todo
método de repositório; teste ArchUnit que quebra o build se faltar; e o teste de integração
descrito na seção 2 — dois workspaces povoados, afirmação de que **todo** endpoint de
listagem retorna zero linhas do vizinho. Esse teste roda em cada push e é o mais valioso do
projeto. Camada futura: Row Level Security no Postgres.

### Risco 5 — Perda de dados, ou VPS única como ponto único de falha (probabilidade: média)

Banco em container numa máquina só, operado por dois iniciantes. Mitigação: `pg_dump` noturno
comprimido para armazenamento externo (R2/B2), retenção de 30 dias, **e um ensaio de
restauração executado na semana 5 e repetido a cada trimestre** — backup não testado não
conta. Volume do Postgres em disco separado do container. No nível do usuário, o desfazer de
lote de importação e as decisões append-only já tornam quase todo erro humano reversível sem
tocar em backup. Se a VPS cair, o plano é recriar em outra em ~1h a partir do Compose + dump:
escrevam esse procedimento em um arquivo `RUNBOOK.md` na semana 5, não na hora do incêndio.

---

## 7. Decisões que vocês precisam tomar

Tudo o mais neste plano já está decidido e justificado. Estas cinco dependem de informação
que só vocês têm.

**Situação em 05/11/2026:** 3, 4 e 5 estão resolvidas — o produto chama **Zimbro**, e só o
domínio continua em aberto dentro da decisão 4. 1 e 2 dependem de conversar com prospects: o
plano segue com o padrão seguro descrito em cada uma, e as respostas são necessárias **antes
da semana 2** (decisão 2) e **antes da semana 4** (decisão 1).

Duas pendências de infraestrutura que não são decisões de produto, mas bloqueiam cliente
real: **não há VPS provisionada** (o job de deploy do CI fica desligado esperando o segredo
`VPS_HOST`) e **o envio de e-mail não está ligado** — o adaptador SMTP existe e é escolhido
por `EMAIL_MODO=smtp`, mas falta credencial de provedor e, antes dela, o domínio: o remetente
precisa ser um endereço do próprio domínio com SPF/DKIM, senão é barrado. Enquanto isso o modo
é `log` e quem esquecer a senha fica trancado fora.

**1. Quais bancos e aplicativos os cinco primeiros prospects usam?**
Define quais *fixtures* de OFX construir na semana 4 e se o corte "OFX antes de CSV" está
certo. Se a maioria usar algo que só exporta CSV (alguns aplicativos de maquininha, por
exemplo), a ordem inverte. **Peçam um extrato exportado a cinco prospects nesta semana** —
isso desbloqueia a semana 3 e a semana 4 ao mesmo tempo.

> **ABERTA.** Padrão adotado até haver resposta: OFX primeiro, CSV fora do MVP, conforme a
> ordem de corte da seção 4. Essa escolha só precisa estar firme no **início da semana 4**,
> então há três semanas de prazo. Se até lá os extratos mostrarem que a maioria vem de
> maquininha (que normalmente só exporta CSV), a ordem inverte: CSV na semana 4 e OFX depois.
> Nada do que é construído nas semanas 1 a 3 muda por causa disso.

**2. Esses MEIs têm conta bancária separada para o negócio, ou misturam tudo numa conta
pessoal?**
É a pergunta que mais afeta o produto. Se a maioria tem conta separada, o `ownership` da
conta resolve quase toda a classificação e a separação pessoal/negócio é quase automática.
Se misturam tudo (minha aposta, e é a dor nº 2 do enunciado), então o escopo **por
lançamento** e o split são o fluxo central da interface, e precisam de três toques, não de
um menu escondido. Minha recomendação é construir assumindo mistura — mas confirmem com 5
conversas antes da semana 2, porque muda a tela principal.

> **ABERTA.** Padrão adotado: assumir que **misturam tudo**, que é o caso mais difícil. A
> consequência é que quem tiver conta separada ganha a facilidade de graça — o escopo vem
> pré-preenchido pelo `ownership` da conta — enquanto o caminho de quem mistura fica
> otimizado. O inverso não seria verdade: construir assumindo conta separada e descobrir na
> semana 4 que misturam obrigaria a refazer a tela principal. Prazo: **antes da semana 2**,
> que é quando essa tela é construída. Bastam 5 conversas.

**3. Confirmam cobrança manual via Pix no MVP, com ativação feita à mão?**
Recomendo sim (justificativa completa em 4.1): economiza ~30 das 150 horas e cria contato
mensal com o cliente. O custo é ~10 min/mês por assinante de trabalho manual e risco de
esquecer de desativar inadimplente.

> **DECIDIDA: Pix manual.** Implementação: coluna `paid_until` em `workspace`, atualizada à
> mão depois do Pix; a trava de assinatura checa a data no login. Mitigação do risco de
> esquecer de cortar inadimplente: um item no painel interno listando workspaces com
> `paid_until` vencido ou vencendo em 7 dias — 1 query, meia hora de trabalho na semana 5.
> Integração (Asaas ou Mercado Pago) entra quando passar de ~40 assinantes.

**4. Nome do produto e domínio.**
Bloqueia deploy, certificado HTTPS, remetente de e-mail, ícone do PWA e a landing page — ou
seja, bloqueia a semana 0. Decidam em 48h, mesmo que o nome não seja o definitivo: trocar
domínio depois custa uma tarde.

> **NOME DECIDIDO: Zimbro.** Já aplicado em todo lugar: pacote Java `br.com.zimbro`, banco
> `zimbro`, nome do projeto do Compose `zimbro`, prefixo do cookie de sessão e dos códigos de
> erro `problem+json` (`urn:zimbro:`). O repositório é
> `git@github.com:GustavoThemyus/zimbro.git`, licença MIT.
>
> **DOMÍNIO AINDA ABERTO — e é ele, não o nome, que bloqueia.** Sem domínio não há HTTPS
> válido (o Caddy não emite certificado para IP), não há remetente de e-mail e não há landing
> page; o deploy inicial fica no IP da VPS, o que basta para testar entre vocês mas não para
> mostrar a cliente. Renomear pacote foi tarefa de IDE de 20 minutos, como previsto; trocar
> domínio **depois de ter cliente cadastrado** é que dói, por causa de link de recuperação de
> senha e do PWA já instalado no celular de alguém. Decidam antes de pôr o primeiro MEI real.

**5. Como dividir o trabalho entre vocês dois?**
Minha recomendação: **Dev A** fica com backend, banco e motor de regras; **Dev B** fica com
Next.js, PWA e as telas; os dois fazem *pair programming* na semana 3 inteira (o motor) e na
semana 0 (infra, para os dois saberem fazer deploy). Com 150 horas, duas pessoas tropeçando
no mesmo arquivo é desperdício caro — mas o motor é a exceção que justifica o custo. Se um de
vocês já for claramente mais forte em um dos lados, digam que eu ajusto o fatiamento.

> **DECIDIDA: Dev A em backend/banco/motor, Dev B em Next.js/PWA/telas.** Pair programming
> apenas na semana 0 (infra — os dois precisam saber fazer deploy, senão o projeto para
> quando um adoece) e na semana 3 inteira (o motor). Consequência prática para o fatiamento:
> o **contrato da API tem que ser acordado antes de cada semana começar**, senão o Dev B fica
> bloqueado esperando endpoint. Combinem isso na segunda-feira de cada semana, em 30 minutos:
> quais endpoints, qual formato de entrada e saída. Enquanto o backend não existe, o Dev B
> trabalha contra respostas falsas (*mock*) escritas à mão nos route handlers do Next.

---

## Verificação ponta a ponta (como saber que cada semana está pronta)

Os critérios de "pronto" das seções 4 são os testes de aceitação. Além deles, a verificação
técnica contínua:

1. **Local:** `docker compose up` sobe Postgres + backend + frontend; Flyway aplica migrações
   num banco vazio sem erro.
2. **Testes automatizados no CI, a cada push:**
   - suíte de descritores do motor (arquivo de casos esperados) — semana 3 em diante
   - teste de isolamento entre dois tenants em todos os endpoints de listagem
   - teste de idempotência da importação (mesmo arquivo duas vezes → zero duplicatas)
   - teste de "não erra duas vezes" (correção → regra proposta → novo lançamento igual chega
     certo)
   - teste de parâmetro legal (mudar linha de `legal_parameter` muda o cálculo sem redeploy)
   - teste dos jobs com relógio injetado (7 dias, geração de obrigação, snapshot)
3. **Conferência manual contra planilha:** workspace de teste semeado com ~60 lançamentos
   cobrindo transferência, split, pró-labore e estorno; os números do fechamento conferidos à
   mão uma vez por semana. Se o fechamento mentir, nada mais importa.
4. **Em produção:** `/actuator/health` monitorado; Sentry sem erro novo após deploy; restauração
   de backup ensaiada na semana 5 e por trimestre.
5. **Com humanos:** 3 MEIs reais instalando e usando sozinhos na semana 5. É o único teste
   que diz se o produto vende.

---

## Fontes consultadas

- [Limite do MEI em 2026 e reajuste do DAS](https://www.mixvale.com.br/2026/10/26/mei-em-2026-mantem-limite-de-r-81-mil-reajusta-das-para-r-8105-e/)
- [Limite do MEI segue em R$ 81 mil e prazo da DASN-SIMEI](https://www.mixvale.com.br/2026/10/28/limite-do-mei-segue-em-r-81-mil-em-2026-e-dasn-simei-vence-em-31-de/)
- [MEI em 2026: o que muda no DAS e na nota fiscal](https://www.contabeis.com.br/noticias/74842/mei-em-2026-veja-o-que-muda-no-das-nota-fiscal-e-regras/)
- [Limite de faturamento MEI 2026](https://www.infinitepay.io/blog/limite-faturamento-mei-2026)
- [Vercel — fair use: Hobby restrito a uso não comercial](https://vercel.com/docs/limits/fair-use-guidelines)
- [Vercel — plano Hobby](https://vercel.com/docs/plans/hobby-plan)
- [Comparativo Cloudflare Pages × Vercel × Netlify (2026)](https://hostfleet.pages.dev/cf-pages-vs-vercel-vs-netlify-2026/)
- [Preços de servidores Hetzner Cloud (2026)](https://cloudpricecheck-com.pages.dev/hetzner/cloud-servers-pricing)

Os valores de DAS divergem entre as fontes no centavo; confirmem no Portal do Simples
Nacional antes de semear a tabela `legal_parameter`. Como esses números são configuráveis por
desenho, errar na semeadura custa um `UPDATE`, não um deploy.