# Financeiro — controle financeiro para MEI

Responde três perguntas que o MEI hoje não consegue responder: **quanto sobrou pra mim**,
**o que é meu e o que é do negócio**, e **estou perto de estourar o limite anual**.

O que ele não é: emissor de nota fiscal, calculadora de imposto, substituto de contador.
Isso é limite de escopo, não omissão.

## Rodar

Precisa de Docker e Docker Compose. Nada mais.

```bash
cp .env.example .env
docker compose up -d --build
```

Abra <http://localhost>. A tela da semana 0 mostra a hora vinda do Postgres e quantas
migrações foram aplicadas — é a prova de que navegador → Next → Spring → Postgres está
ligado ponta a ponta.

### Rodar para desenvolver

Com recarga automática nos dois lados, só o banco no Docker:

```bash
docker compose up -d db                   # banco na porta 5433 do seu computador
cd backend && ./mvnw spring-boot:run     # porta 8080
cd frontend && npm install && npm run dev # porta 3000
```

### Testar

```bash
cd backend && ./mvnw verify   # sobe um Postgres de verdade com Testcontainers
cd frontend && npm run build  # o build roda o TypeScript junto
```

## Estrutura

```
backend/     Java 21 + Spring Boot, monolito modular (pacote por funcionalidade)
frontend/    Next.js App Router, PWA instalável
infra/       Caddyfile — HTTPS automático e proxy reverso
docker-compose.yml
```

O backend é organizado por assunto, não por camada. Hoje existe só `platform/`; os módulos
`identity/`, `accounts/`, `ledger/`, `catalog/`, `rules/`, `imports/`, `mei/`, `reporting/`,
`notifications/` e `billing/` nascem na semana em que são construídos — pasta vazia não
ajuda ninguém.

## Duas regras que não se negociam

**Dinheiro é `bigint` em centavos.** Nunca `float`, nunca `double`, nunca `BigDecimal`
espalhado. R$ 1.234,56 é `123456`. Ponto flutuante não representa R$ 0,10 exatamente, e
somar mil deles dá diferença de centavos — o cliente perde a confiança na primeira
conferência contra o extrato.

**Nenhum número de lei mora no código.** Limite anual, valor do DAS, dia de vencimento e
limiares de alerta vivem na tabela `legal_parameter`, com `valid_from`/`valid_to`. Ela
responde "quanto era o DAS em março de 2025?", não só "quanto é o DAS?" — relatório de mês
passado não pode ser reescrito quando a lei muda. Mudar um valor em produção é um `UPDATE`,
nunca um deploy.

> **Conferir antes do primeiro cliente pagante:** os valores do DAS em
> `V2__parametros_legais_2026.sql` vieram de fontes que divergem no centavo. Confirme no
> Portal do Simples Nacional. Falta de propósito o DAS de transporte (MEI caminhoneiro):
> preferimos que a geração de obrigação falhe alto a semear um número inventado.

## Design

O sistema visual é o artifact **Sistema visual MEI**, e ele é a fonte da verdade.
`frontend/design/tokens.json` é uma cópia dele; `frontend/app/tokens.css` é **gerado** a
partir dessa cópia:

```bash
cd frontend && npm run tokens
```

Ninguém edita `app/tokens.css` à mão — a CI compara o arquivo gerado com o versionado e
quebra o build se alguém tentar. Mudou o design system? Troque `design/tokens.json`, rode
`npm run tokens`, comite os dois.

Tema **claro é o padrão**, apesar de as referências visuais serem escuras: o cliente usa o
app em pé, no balcão, sob sol. Escuro entra automaticamente pela preferência do sistema.

## Decisões de versão que divergem do plano original

| Plano | Realidade | Por quê |
|---|---|---|
| Spring Boot 3 | **Spring Boot 4.1.2** | O Spring Initializr não serve mais 3.x, e a linha 3.5 saiu do suporte gratuito. Começar um produto financeiro num ramo sem correção de segurança é pior que a inconveniência de tutorial. |
| PostgreSQL 16 | **PostgreSQL 18** | `uuidv7()` é nativo no 18. O plano já pedia UUID v7 (aleatório mas ordenado por tempo); no 16 seria código nosso. |

Se preferirem voltar para Boot 3.5, agora é a hora: é uma linha do `pom.xml` e meia dúzia
de importes. Depois da semana 3, não é mais.
