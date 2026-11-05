# Zimbro — controle financeiro para MEI

Responde três perguntas que o MEI hoje não consegue responder: **quanto sobrou pra mim**,
**o que é meu e o que é do negócio**, e **estou perto de estourar o limite anual**.

O que ele não é: emissor de nota fiscal, calculadora de imposto, substituto de contador.
Isso é limite de escopo, não omissão.

## Rodar

> `npm run dev` **na raiz não funciona** — o `package.json` mora em `frontend/`.
> Use os atalhos do `make`, que rodam da raiz: `make ajuda` lista todos.

### Tudo no Docker, como em produção

Precisa de Docker e Docker Compose. Nada mais.

```bash
cp .env.example .env
make up
```

Abra <http://localhost>, crie uma conta e lance. O cadastro já cria duas contas
(“Conta do banco” e “Dinheiro”) para você poder registrar antes de configurar qualquer
coisa.

> **Neste modo a porta 3000 não responde.** Dentro do Compose o Next e o Spring só escutam na
> rede interna do Docker; quem atende o navegador é o Caddy, na porta 80. Isso é de propósito —
> é o que mantém o backend nunca exposto direto ao navegador. Se você abrir
> `localhost:3000` aqui, vai levar "conexão recusada", e está tudo certo: o endereço é
> <http://localhost>. A 3000 é do modo de desenvolvimento, abaixo.

### Telas

| Rota | O que é |
|---|---|
| `/cadastro`, `/entrar` | Criar conta e entrar. Cadastrar já entra. |
| `/` | O mês: quanto sobrou, entrou, saiu, quanto você tirou, e a lista. |
| `/lancar` | Lançamento rápido. Três toques: valor, categoria, salvar. |
| `/contas` | Contas do workspace. |

### Desenvolvendo, com recarga automática

Só o banco no Docker; backend e frontend rodam na sua máquina. São **três
terminais**:

```bash
make dev
```

Isso sobe o banco e te lembra dos outros dois:

```bash
cd backend && ./mvnw spring-boot:run      # porta 8080
```

```bash
cd frontend && npm install && npm run dev # porta 3000  <- é esta que você abre
```

Abra **<http://localhost:3000>**. O Next conversa com o Spring sozinho; você
nunca abre a porta 8080 no navegador.

### Testar

```bash
make teste
```

Os testes do backend sobem um Postgres de verdade com Testcontainers — banco em
memória mente sobre SQL, e aqui o SQL é o produto.

## Estrutura

```
backend/     Java 21 + Spring Boot, monolito modular (pacote por funcionalidade)
frontend/    Next.js App Router, PWA instalável
infra/       Caddyfile — HTTPS automático e proxy reverso
docker-compose.yml
```

O backend é organizado por assunto, não por camada. Existem `platform/`, `identity/`,
`accounts/`, `catalog/`, `ledger/`, `reporting/` e `notifications/`; `rules/`, `imports/`,
`mei/` e `billing/` nascem na semana em que são construídos — pasta vazia não ajuda
ninguém.

Conversa entre módulos passa por um pacote `api` (veja `accounts/api/ContasApi`): ninguém
importa a entidade nem o repositório de outro módulo, e referência cruzada é por id.

### Sessão

O token de sessão vive num cookie **httpOnly** escrito pelo Next. O JavaScript da página
nunca o vê, então um XSS não rouba sessão de ninguém. O navegador fala só com o Next, que
encaminha para o Spring em `app/api/b/[...rota]`. No banco fica apenas o SHA-256 do token.

### E-mail

A recuperação de senha é hoje a única saída de quem esquece a senha, então o envio tem dois
adaptadores atrás da mesma porta (`notifications/api/Avisos`), escolhidos por configuração:

| `EMAIL_MODO` | Adaptador | O que faz |
|---|---|---|
| `log` (padrão) | `AvisosPorLog` | escreve o link no log do servidor e **grita no startup** que nada é enviado |
| `smtp` | `AvisosPorEmail` | envia de verdade, em texto puro, por SMTP com STARTTLS |

Para ligar o envio real, preencha no `.env`: `EMAIL_MODO=smtp`, `EMAIL_REMETENTE`,
`SMTP_HOST`, `SMTP_PORT`, `SMTP_USUARIO`, `SMTP_SENHA`. Nenhum código muda.

Três decisões que valem explicar:

**`EMAIL_MODO=smtp` com `SMTP_HOST` vazio derruba o app no startup.** Não volta calado para
o modo de log. Erro de digitação em variável de ambiente precisa quebrar o deploy, não
aparecer semanas depois no primeiro cliente que esquece a senha.

**Falha de envio não estoura para quem pediu.** A chamada acontece depois de o token de
recuperação já estar gravado. Se o SMTP estiver fora do ar, o erro vai para o log e para o
Sentry, e o usuário pede outro link — em vez de levar 500 na tela e nenhum e-mail.

**`APP_ENDERECO` é obrigatório em produção.** É ele que monta o link do e-mail. O padrão é
`http://localhost:3000`, que serve para desenvolver e não serve para ninguém mais; no Compose
ele vem de `DOMINIO`.

> **Falta antes do primeiro cliente:** nenhuma credencial de SMTP está preenchida, e o
> remetente precisa ser um endereço do domínio do Zimbro com SPF/DKIM configurados —
> remetente de domínio alheio é barrado ou cai em spam. Ou seja: **isto só fecha depois de o
> domínio existir.** Até lá o modo é `log`, e nenhum MEI real pode se cadastrar.

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
